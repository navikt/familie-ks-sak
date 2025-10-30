package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.ks.sak.api.dto.BehandlingStegDto
import no.nav.familie.ks.sak.api.dto.BesluttVedtakDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.Beslutning
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.AVSLUTT_BEHANDLING
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.BEHANDLINGSRESULTAT
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.BESLUTTE_VEDTAK
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.IVERKSETT_MOT_OPPDRAG
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.JOURNALFØR_VEDTAKSBREV
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.VEDTAK
import no.nav.familie.ks.sak.kjerne.behandling.steg.iverksettmotoppdrag.IverksettMotOppdragTask
import no.nav.familie.ks.sak.kjerne.behandling.steg.journalførvedtaksbrev.JournalførVedtaksbrevTask
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.overgangsordningAndelerPerAktør
import no.nav.familie.ks.sak.kjerne.beregning.domene.totalKalkulertUtbetalingsbeløpForPeriode
import no.nav.familie.ks.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil.lagEndringIUtbetalingTidslinje
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.SendOpprettTilbakekrevingsbehandlingRequestTask
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class StegService(
    private val steg: List<IBehandlingSteg>,
    private val behandlingRepository: BehandlingRepository,
    private val vedtakRepository: VedtakRepository,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val sakStatistikkService: SakStatistikkService,
    private val taskService: TaskRepositoryWrapper,
    private val loggService: LoggService,
    private val behandlingService: BehandlingService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {
    @Transactional
    fun utførSteg(
        behandlingId: Long,
        behandlingSteg: BehandlingSteg,
        behandlingStegDto: BehandlingStegDto? = null,
    ) {
        val behandling = behandlingRepository.hentAktivBehandling(behandlingId)
        val behandlingStegTilstand = hentStegTilstandForBehandlingSteg(behandling, behandlingSteg)

        valider(behandling, behandlingSteg)
        when (behandlingStegTilstand.behandlingStegStatus) {
            BehandlingStegStatus.KLAR -> {
                // utfør steg, kaller utfør metode i tilsvarende steg klasser
                behandlingStegDto?.let { hentStegInstans(behandlingSteg).utførSteg(behandlingId, it) }
                    ?: hentStegInstans(behandlingSteg).utførSteg(behandlingId)
                // utleder nåværendeSteg status, den blir TILBAKEFØRT når beslutter underkjenner vedtaket ellers UTFØRT
                behandlingStegTilstand.behandlingStegStatus =
                    utledNåværendeBehandlingStegStatus(
                        behandlingSteg,
                        behandlingStegDto,
                    )
                // AVSLUTT_BEHANDLING er siste steg, der slipper man å hente neste steg
                if (behandlingSteg != AVSLUTT_BEHANDLING) {
                    // Henter neste steg basert på sekvens og årsak
                    val nesteSteg = hentNesteSteg(behandling, behandlingSteg, behandlingStegDto)
                    // legger til neste steg hvis steget er ny, eller oppdaterer eksisterende steg status til KLAR
                    behandling.behandlingStegTilstand
                        .singleOrNull { it.behandlingSteg == nesteSteg }
                        ?.let { it.behandlingStegStatus = BehandlingStegStatus.KLAR }
                        ?: behandling.leggTilNesteSteg(nesteSteg)
                }

                // oppdaterer behandling med behandlingstegtilstand og behandling status
                behandlingRepository.saveAndFlush(oppdaterBehandlingStatus(behandling))

                // forsøker om neste steg kan utføres automatisk
                utførStegAutomatisk(behandling)
            }

            BehandlingStegStatus.UTFØRT -> {
                // tilbakefører alle stegene som er etter behandlede steg
                behandling.behandlingStegTilstand
                    .filter { it.behandlingSteg.sekvens > behandlingSteg.sekvens }
                    .forEach { it.behandlingStegStatus = BehandlingStegStatus.TILBAKEFØRT }

                // oppdaterte behandling med behandlede steg som KLAR slik at det kan behandles
                hentStegTilstandForBehandlingSteg(behandling, behandlingSteg).behandlingStegStatus =
                    BehandlingStegStatus.KLAR
                behandlingRepository.saveAndFlush(oppdaterBehandlingStatus(behandling))

                utførSteg(behandlingId, behandlingSteg, behandlingStegDto)
            }

            BehandlingStegStatus.VENTER -> {
                // oppdaterte behandling med behandlede steg som KLAR slik at det kan behandles
                logger.info("Gjenopptar behandling ${behandling.id}")

                behandlingStegTilstand.behandlingStegStatus = BehandlingStegStatus.KLAR
                behandlingStegTilstand.frist = null
                behandlingStegTilstand.årsak = null
                behandlingRepository.saveAndFlush(oppdaterBehandlingStatus(behandling))
            }
            // AVBRUTT kan brukes kun for henleggelse
            // TILBAKEFØRT steg blir oppdatert til KLAR når det forrige steget er behandlet
            BehandlingStegStatus.AVBRUTT, BehandlingStegStatus.TILBAKEFØRT ->
                throw Feil(
                    "Kan ikke behandle behandling $behandlingId " +
                        "med steg $behandlingSteg med status ${behandlingStegTilstand.behandlingStegStatus}",
                )
        }
        // statistikk til datavarehus
        sakStatistikkService.opprettSendingAvBehandlingensTilstand(behandlingId, behandlingSteg)
    }

    private fun valider(
        behandling: Behandling,
        behandlingSteg: BehandlingSteg,
    ) {
        // valider om steget kan behandles av saksbehandler eller beslutter
        if (!behandlingSteg.kanStegBehandles() && !SikkerhetContext.erSystemKontekst()) {
            throw Feil("Steget ${behandlingSteg.name} kan ikke behandles for behandling ${behandling.id}")
        }

        // valider at behandlingen ikke er avsluttet
        if (behandling.erAvsluttet()) {
            throw Feil(
                message = "Forsøker å starte steget ${behandlingSteg.name} i en avsluttet behandling. Fagsak ${behandling.fagsak.id}, behandling ${behandling.id}.",
                frontendFeilmelding = "Kan ikke utføre steget ${behandlingSteg.name}. Behandlingen er avsluttet. Oppdater siden eller gå til saksoversikt.",
            )
        }

        // valider om steget samsvarer med opprettet årsak til behandling
        behandlingSteg.gyldigForÅrsaker.singleOrNull { it == behandling.opprettetÅrsak }
            ?: throw Feil(
                "Steget ${behandlingSteg.name} er ikke gyldig for behandling ${behandling.id} " +
                    "med opprettetÅrsak ${behandling.opprettetÅrsak}",
            )

        // valider om et tidligere steg i behandlingen har stegstatus KLAR
        val stegKlarForBehandling =
            behandling.behandlingStegTilstand.singleOrNull {
                it.behandlingSteg.sekvens < behandlingSteg.sekvens &&
                    it.behandlingStegStatus == BehandlingStegStatus.KLAR
            }
        if (stegKlarForBehandling != null) {
            throw Feil(
                "Behandling ${behandling.id} har allerede et steg " +
                    "${stegKlarForBehandling.behandlingSteg.name}} som er klar for behandling. " +
                    "Kan ikke behandle ${behandlingSteg.name}",
            )
        }

        // dersom behandlingsteg er BESLUTTE_VEDTAK eller senere valider at steget ikke allerede finnes i behandlingStegTilstand med status utført.
        if (behandlingSteg.sekvens >= BESLUTTE_VEDTAK.sekvens) {
            val alleredeUtførtSteg = behandling.behandlingStegTilstand.singleOrNull { it.behandlingSteg == behandlingSteg && it.behandlingStegStatus == BehandlingStegStatus.UTFØRT }
            if (alleredeUtførtSteg != null) {
                val feilmelding = "${behandlingSteg.name} er allerede utført for behandlingen. Oppdater siden eller gå til saksoversikt."
                throw Feil(
                    message = feilmelding + "Fagsak: ${behandling.fagsak.id}, behandling ${behandling.id}.",
                    frontendFeilmelding = feilmelding,
                )
            }
        }
    }

    fun hentNesteSteg(
        behandling: Behandling,
        behandletSteg: BehandlingSteg,
        behandlingStegDto: BehandlingStegDto?,
    ): BehandlingSteg {
        val nesteGyldigeSteg = behandletSteg.nesteGyldigeSteg(behandling)
        return when (behandletSteg) {
            AVSLUTT_BEHANDLING -> throw Feil("Behandling ${behandling.id} er allerede avsluttet")

            BESLUTTE_VEDTAK -> {
                val beslutteVedtakDto = behandlingStegDto as BesluttVedtakDto
                when (beslutteVedtakDto.beslutning) {
                    Beslutning.GODKJENT -> hentNesteStegOgOpprettTaskEtterBeslutteVedtak(behandling)
                    Beslutning.UNDERKJENT -> VEDTAK
                }
            }

            BEHANDLINGSRESULTAT ->
                if (behandling.skalBehandlesAutomatisk() && !behandlingService.erLovendringOgFremtidigOpphørOgHarFlereAndeler(behandling)) {
                    IVERKSETT_MOT_OPPDRAG
                } else {
                    nesteGyldigeSteg
                }

            IVERKSETT_MOT_OPPDRAG ->
                if (behandling.skalBehandlesAutomatisk() && !behandlingService.erLovendringOgFremtidigOpphørOgHarFlereAndeler(behandling)) {
                    AVSLUTT_BEHANDLING
                } else {
                    nesteGyldigeSteg
                }

            else -> nesteGyldigeSteg
        }
    }

    private fun hentNesteStegOgOpprettTaskEtterBeslutteVedtak(behandling: Behandling): BehandlingSteg {
        val nesteSteg =
            if (erEndringIUtbetaling(behandling)) {
                IVERKSETT_MOT_OPPDRAG
            } else {
                IVERKSETT_MOT_OPPDRAG.nesteGyldigeSteg(behandling)
            }
        if (nesteSteg == JOURNALFØR_VEDTAKSBREV) {
            opprettJournalførVedtaksbrevTaskPåBehandling(behandling)
        }
        return nesteSteg
    }

    fun erEndringIUtbetaling(behandling: Behandling): Boolean {
        val forrigeBehandling = behandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id)
        val (ordinæreAndelerForrigeBehandling, overgangsordningAndelerForrigeBehandling) =
            forrigeBehandling?.let {
                andelTilkjentYtelseRepository
                    .finnAndelerTilkjentYtelseForBehandling(it.id)
                    .partition { it.type == YtelseType.ORDINÆR_KONTANTSTØTTE }
            } ?: Pair(emptyList(), emptyList())

        val (ordinæreAndelerNåværendeBehandling, overgangsordningAndelerNåværendeBehandling) =
            behandling.let {
                andelTilkjentYtelseRepository
                    .finnAndelerTilkjentYtelseForBehandling(it.id)
                    .partition { it.type == YtelseType.ORDINÆR_KONTANTSTØTTE }
            }

        return lagEndringIUtbetalingTidslinje(ordinæreAndelerNåværendeBehandling, ordinæreAndelerForrigeBehandling)
            .tilPerioder()
            .any { it.verdi == true } ||
            erEndringIOvergangsordningAndeler(overgangsordningAndelerNåværendeBehandling, overgangsordningAndelerForrigeBehandling)
    }

    private fun erEndringIOvergangsordningAndeler(
        andelerNåværendeBehandling: List<AndelTilkjentYtelse>,
        andelerForrigeBehandling: List<AndelTilkjentYtelse>,
    ): Boolean {
        val sumOvergangsordningAndelerPerAktørNåværendeBehandling =
            andelerNåværendeBehandling
                .overgangsordningAndelerPerAktør()
                .mapValues { (_, andeler) ->
                    andeler.sumOf { it.totalKalkulertUtbetalingsbeløpForPeriode() }
                }

        val sumOvergangsordningAndelerPerAktørForrigeBehandling =
            andelerForrigeBehandling
                .overgangsordningAndelerPerAktør()
                .mapValues { (_, andeler) ->
                    andeler.sumOf { it.totalKalkulertUtbetalingsbeløpForPeriode() }
                }

        return sumOvergangsordningAndelerPerAktørNåværendeBehandling != sumOvergangsordningAndelerPerAktørForrigeBehandling
    }

    private fun utførStegAutomatisk(behandling: Behandling) {
        // Dersom behandling er lovendring må vedtak kontrolleres først
        if (behandling.steg == IVERKSETT_MOT_OPPDRAG && !behandling.erLovendring()) {
            val vedtakId = vedtakRepository.findByBehandlingAndAktiv(behandling.id).id
            val saksbehandlerId = SikkerhetContext.hentSaksbehandler()
            taskService.save(IverksettMotOppdragTask.opprettTask(behandling, vedtakId, saksbehandlerId))
        }
    }

    // Denne metoden kalles av HentStatusFraOppdragTask for å sende behandling videre etter OK status fra oppdrag er mottatt
    fun utførStegEtterIverksettelseAutomatisk(behandlingId: Long) {
        val behandling = behandlingRepository.hentAktivBehandling(behandlingId)

        // opprett tilbakekreving task om det finnes en tilbakekrevingsvalg
        tilbakekrevingRepository.findByBehandlingId(behandlingId)?.let {
            when (it.valg) {
                Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING -> {
                    logger.info(
                        """Tilbakekrevingsvalg er ${it.valg.name} for behandling $behandlingId.
                            Oppretter ikke tilbakekrevingsbehandling""",
                    )
                }

                else -> taskService.save(SendOpprettTilbakekrevingsbehandlingRequestTask.opprettTask(behandlingId))
            }
        }
        when (behandling.steg) {
            JOURNALFØR_VEDTAKSBREV -> {
                // JournalførVedtaksbrevTask -> DistribuerBrevTask -> AvsluttBehandlingTask for å avslutte behandling automatisk
                opprettJournalførVedtaksbrevTaskPåBehandling(behandling)
            }
            // Behandling med årsak SATSENDRING eller TEKNISK_ENDRING sender ikke vedtaksbrev. Da avslutter behandling her
            AVSLUTT_BEHANDLING -> utførSteg(behandlingId = behandling.id, AVSLUTT_BEHANDLING)

            else -> {} // Gjør ingenting
        }
    }

    private fun opprettJournalførVedtaksbrevTaskPåBehandling(behandling: Behandling) {
        val vedtakId = vedtakRepository.findByBehandlingAndAktiv(behandling.id).id
        taskService.save(JournalførVedtaksbrevTask.opprettTask(behandling, vedtakId))
    }

    fun settBehandlingstegPåVent(
        behandling: Behandling,
        frist: LocalDate,
        årsak: VenteÅrsak,
    ) {
        val behandlingStegTilstand = hentStegTilstandForBehandlingSteg(behandling, behandling.steg)

        loggService.opprettSettPåVentLogg(
            behandling = behandling,
            årsak = årsak.visningsnavn,
        )

        logger.info("Setter behandling ${behandling.id} på vent med frist $frist og årsak $årsak")

        behandlingStegTilstand.frist = frist
        behandlingStegTilstand.årsak = årsak
        behandlingStegTilstand.behandlingStegStatus = BehandlingStegStatus.VENTER
        behandlingRepository.saveAndFlush(behandling)
        // Send data til Dvh
        sakStatistikkService.opprettSendingAvBehandlingensTilstand(behandling.id, behandling.steg)
    }

    fun oppdaterBehandlingstegFristOgÅrsak(
        behandling: Behandling,
        frist: LocalDate,
        årsak: VenteÅrsak,
    ): LocalDate? {
        val behandlingStegTilstand = hentStegTilstandForBehandlingSteg(behandling, behandling.steg)

        if (frist == behandlingStegTilstand.frist && årsak == behandlingStegTilstand.årsak) {
            throw FunksjonellFeil("Behandlingen er allerede satt på vent med frist $frist og årsak $årsak.")
        }

        loggService.opprettOppdaterVentingLogg(
            behandling = behandling,
            endretÅrsak = if (årsak != behandlingStegTilstand.årsak) årsak.visningsnavn else null,
            endretFrist = if (frist != behandlingStegTilstand.frist) frist else null,
        )

        logger.info("Oppdater ventende behandling ${behandling.id} med frist $frist og årsak $årsak")

        val gammelFrist = behandlingStegTilstand.frist

        behandlingStegTilstand.frist = frist
        behandlingStegTilstand.årsak = årsak
        behandlingRepository.saveAndFlush(behandling)

        // Send data til Dvh
        sakStatistikkService.opprettSendingAvBehandlingensTilstand(behandling.id, behandling.steg)

        return gammelFrist
    }

    fun settAlleStegTilAvbrutt(behandling: Behandling) {
        behandling.behandlingStegTilstand.forEach { it.behandlingStegStatus = BehandlingStegStatus.AVBRUTT }
    }

    private fun hentStegTilstandForBehandlingSteg(
        behandling: Behandling,
        behandlingSteg: BehandlingSteg,
    ): BehandlingStegTilstand =
        behandling.behandlingStegTilstand.singleOrNull { it.behandlingSteg == behandlingSteg }
            ?: throw Feil("$behandlingSteg finnes ikke i Behandling ${behandling.id}")

    private fun hentStegInstans(behandlingssteg: BehandlingSteg): IBehandlingSteg =
        steg.singleOrNull { it.getBehandlingssteg() == behandlingssteg }
            ?: throw Feil("Finner ikke behandlingssteg $behandlingssteg")

    private fun utledNåværendeBehandlingStegStatus(
        behandlingSteg: BehandlingSteg,
        behandlingStegDto: BehandlingStegDto? = null,
    ) = when (behandlingSteg) {
        BESLUTTE_VEDTAK -> {
            val beslutteVedtakDto = behandlingStegDto as BesluttVedtakDto
            when (beslutteVedtakDto.beslutning) {
                Beslutning.GODKJENT -> BehandlingStegStatus.UTFØRT
                Beslutning.UNDERKJENT -> BehandlingStegStatus.TILBAKEFØRT
            }
        }

        else -> BehandlingStegStatus.UTFØRT
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StegService::class.java)

        fun oppdaterBehandlingStatus(behandling: Behandling): Behandling {
            // oppdaterer ikke behandling status for siste steg AVSLUTT_BEHANDLING. Det skjer direkte i steget
            if (behandling.steg == AVSLUTT_BEHANDLING) {
                return behandling
            }
            val nyBehandlingStatus = behandling.steg.tilknyttetBehandlingStatus
            logger.info(
                "${SikkerhetContext.hentSaksbehandlerNavn()} endrer status på behandling ${behandling.id} " +
                    "fra ${behandling.status} til $nyBehandlingStatus",
            )
            behandling.status = nyBehandlingStatus
            return behandling
        }
    }
}
