package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.kontrakter.felles.klage.IkkeOpprettet
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettetÅrsak
import no.nav.familie.kontrakter.felles.klage.KanIkkeOppretteRevurderingÅrsak
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.Opprettet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.integrasjon.oppgave.OpprettOppgaveTask
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingMetrikker
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import no.nav.familie.ks.sak.kjerne.behandling.domene.NyEksternBehandlingRelasjon
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class OpprettBehandlingService(
    private val personidentService: PersonidentService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val vedtakService: VedtakService,
    private val loggService: LoggService,
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val taskService: TaskService,
    private val stegService: StegService,
    private val behandlingMetrikker: BehandlingMetrikker,
    private val unleashService: UnleashNextMedContextService,
    private val eksternBehandlingRelasjonService: EksternBehandlingRelasjonService,
) {
    @Transactional
    fun opprettBehandling(opprettBehandlingRequest: OpprettBehandlingDto): Behandling {
        if (opprettBehandlingRequest.behandlingÅrsak == BehandlingÅrsak.IVERKSETTE_KA_VEDTAK &&
            !unleashService.isEnabled(FeatureToggle.KAN_OPPRETTE_REVURDERING_MED_ÅRSAK_IVERKSETTE_KA_VEDTAK)
        ) {
            throw FunksjonellFeil(
                melding = "Kan ikke opprette behandling med årsak Iverksette KA-vedtak.",
            )
        }

        val aktør = personidentService.hentAktør(opprettBehandlingRequest.søkersIdent)
        val fagsak =
            fagsakRepository.finnFagsakForAktør(aktør)
                ?: throw FunksjonellFeil(
                    melding = "Kan ikke lage behandling på person uten tilknyttet fagsak.",
                )

        val aktivBehandling = behandlingRepository.findByFagsakAndAktiv(fagsak.id)
        val sisteVedtattBehandling = hentSisteBehandlingSomErVedtatt(fagsak.id)

        // Kan ikke opprette en behandling når det allerede finnes en behandling som ikke er avsluttet
        if (aktivBehandling != null && aktivBehandling.status != BehandlingStatus.AVSLUTTET) {
            throw FunksjonellFeil(
                melding = "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.",
            )
        }

        val behandlingKategori =
            bestemKategoriVedOpprettelse(
                overstyrtKategori = opprettBehandlingRequest.kategori,
                behandlingType = opprettBehandlingRequest.behandlingType,
                behandlingÅrsak = opprettBehandlingRequest.behandlingÅrsak,
                kategoriFraLøpendeBehandling = BehandlingKategori.NASJONAL,
            )
        val behandling =
            Behandling(
                fagsak = fagsak,
                type = opprettBehandlingRequest.behandlingType,
                opprettetÅrsak = opprettBehandlingRequest.behandlingÅrsak,
                kategori = behandlingKategori,
                søknadMottattDato = opprettBehandlingRequest.søknadMottattDato?.atStartOfDay(),
            ).initBehandlingStegTilstand() // oppretter behandling med initielt steg Registrer Persongrunnlag

        behandling.validerBehandlingstype(sisteVedtattBehandling)
        val lagretBehandling =
            lagreNyOgDeaktiverGammelBehandling(
                nyBehandling = behandling,
                aktivBehandling = aktivBehandling,
                sisteVedtattBehandling = sisteVedtattBehandling,
            )

        if (opprettBehandlingRequest.nyEksternBehandlingRelasjon != null) {
            eksternBehandlingRelasjonService.lagreEksternBehandlingRelasjon(
                EksternBehandlingRelasjon.opprettFraNyEksternBehandlingRelasjon(
                    internBehandlingId = lagretBehandling.id,
                    nyEksternBehandlingRelasjon = opprettBehandlingRequest.nyEksternBehandlingRelasjon,
                ),
            )
        }

        vedtakService.opprettOgInitierNyttVedtakForBehandling(lagretBehandling) // initierer vedtak
        loggService.opprettBehandlingLogg(lagretBehandling) // lag historikkinnslag
        // Oppretter BehandleSak oppgave via task. Ruller tasken tilbake, hvis behandling opprettelse feiler
        if (lagretBehandling.skalOppretteBehandleSakOppgave()) {
            taskService.save(
                OpprettOppgaveTask.opprettTask(
                    behandlingId = lagretBehandling.id,
                    oppgavetype = Oppgavetype.BehandleSak,
                    fristForFerdigstillelse = LocalDate.now(),
                    tilordnetRessurs = opprettBehandlingRequest.saksbehandlerIdent,
                ),
            )
        }
        // Utfør Registrer Persongrunnlag steg
        stegService.utførSteg(lagretBehandling.id, BehandlingSteg.REGISTRERE_PERSONGRUNNLAG)

        // opprett task for å sende start behandling hendelse til infotrygd for førstegangsbehandling
        if (lagretBehandling.type == BehandlingType.FØRSTEGANGSBEHANDLING) {
            taskService.save(SendStartBehandlingHendelseTilInfotrygdTask.opprettTask(aktør))
        }
        return lagretBehandling
    }

    private fun lagreNyOgDeaktiverGammelBehandling(
        nyBehandling: Behandling,
        aktivBehandling: Behandling?,
        sisteVedtattBehandling: Behandling?,
    ): Behandling {
        aktivBehandling?.let { behandlingRepository.saveAndFlush(aktivBehandling.also { it.aktiv = false }) }
        return lagreEllerOppdater(nyBehandling).also {
            arbeidsfordelingService.fastsettBehandlendeEnhet(
                it,
                sisteVedtattBehandling,
            )

            if (it.versjon == 0L) {
                behandlingMetrikker.tellNøkkelTallVedOpprettelseAvBehandling(it)
            }
        }
    }

    @Transactional(readOnly = true)
    fun kanOppretteRevurdering(fagsakId: Long): KanOppretteRevurderingResponse {
        val fagsak = hentFagsak(fagsakId)
        val resultat = utledKanOppretteRevurdering(fagsak)
        return when (resultat) {
            is KanOppretteRevurdering -> KanOppretteRevurderingResponse(true, null)
            is KanIkkeOppretteRevurdering -> KanOppretteRevurderingResponse(false, resultat.årsak.kanIkkeOppretteRevurderingÅrsak)
        }
    }

    @Transactional
    fun validerOgOpprettRevurderingKlage(
        fagsakId: Long,
        klagebehandlingId: UUID,
    ): OpprettRevurderingResponse {
        val fagsak = hentFagsak(fagsakId)

        val resultat = utledKanOppretteRevurdering(fagsak)
        return when (resultat) {
            is KanOppretteRevurdering -> opprettRevurderingKlage(fagsak, klagebehandlingId)
            is KanIkkeOppretteRevurdering -> OpprettRevurderingResponse(IkkeOpprettet(resultat.årsak.ikkeOpprettetÅrsak))
        }
    }

    private fun opprettRevurderingKlage(
        fagsak: Fagsak,
        klagebehandlingId: UUID,
    ): OpprettRevurderingResponse =
        try {
            val forrigeBehandling = hentSisteBehandlingSomErVedtatt(fagsakId = fagsak.id)

            val behandlingDto =
                OpprettBehandlingDto(
                    kategori = forrigeBehandling?.kategori ?: BehandlingKategori.NASJONAL,
                    søkersIdent = fagsak.aktør.aktivFødselsnummer(),
                    behandlingType = BehandlingType.REVURDERING,
                    behandlingÅrsak = BehandlingÅrsak.KLAGE,
                    nyEksternBehandlingRelasjon = NyEksternBehandlingRelasjon.opprettForKlagebehandling(klagebehandlingId),
                )

            val revurdering = opprettBehandling(behandlingDto)
            OpprettRevurderingResponse(Opprettet(revurdering.id.toString()))
        } catch (e: Exception) {
            logger.error("Feilet opprettelse av revurdering for fagsak=${fagsak.id}, se secure logg for detaljer")
            secureLogger.error("Feilet opprettelse av revurdering for fagsak=$fagsak", e)
            OpprettRevurderingResponse(IkkeOpprettet(IkkeOpprettetÅrsak.FEIL, e.message))
        }

    private fun utledKanOppretteRevurdering(fagsak: Fagsak): KanOppretteRevurderingResultat {
        val finnesÅpenBehandlingPåFagsak = erÅpenBehandlingPåFagsak(fagsak.id)
        if (finnesÅpenBehandlingPåFagsak) {
            return KanIkkeOppretteRevurdering(Årsak.ÅPEN_BEHANDLING)
        }
        if (!erAktivBehandlingPåFagsak(fagsak.id)) {
            return KanIkkeOppretteRevurdering(Årsak.INGEN_BEHANDLING)
        }
        return KanOppretteRevurdering
    }

    private fun lagreEllerOppdater(behandling: Behandling): Behandling {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter behandling $behandling")
        return behandlingRepository.save(behandling)
    }

    // kan kalles fra BehandlingController eller OpprettBehandlingServiceTest metoder,
    // andre tjenester bruker eventuelt BehandlingService istedet
    fun hentSisteBehandlingSomErVedtatt(fagsakId: Long): Behandling? =
        behandlingRepository
            .finnBehandlinger(fagsakId)
            .filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.aktivertTidspunkt }

    // kan kalles fra BehandlingController eller OpprettBehandlingServiceTest metoder,
    // andre tjenester bruker eventuelt BehandlingService istedet
    fun hentBehandling(behandlingId: Long): Behandling = behandlingRepository.hentBehandling(behandlingId)

    private fun hentFagsak(fagsakId: Long) =
        fagsakRepository.finnFagsak(fagsakId)
            ?: throw FunksjonellFeil("Fant ikke fagsak med ID=$fagsakId.")

    fun finnAktivBehandlingPåFagsak(fagsakId: Long): Behandling? = behandlingRepository.findByFagsakAndAktiv(fagsakId)

    fun erAktivBehandlingPåFagsak(fagsakId: Long): Boolean = finnAktivBehandlingPåFagsak(fagsakId) != null

    fun finnÅpenBehandlingPåFagsak(fagsakId: Long): Behandling? = behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId)

    fun erÅpenBehandlingPåFagsak(fagsakId: Long): Boolean = finnÅpenBehandlingPåFagsak(fagsakId) != null

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(OpprettBehandlingService::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

private sealed interface KanOppretteRevurderingResultat

private object KanOppretteRevurdering : KanOppretteRevurderingResultat

private data class KanIkkeOppretteRevurdering(
    val årsak: Årsak,
) : KanOppretteRevurderingResultat

private enum class Årsak(
    val ikkeOpprettetÅrsak: IkkeOpprettetÅrsak,
    val kanIkkeOppretteRevurderingÅrsak: KanIkkeOppretteRevurderingÅrsak,
) {
    ÅPEN_BEHANDLING(IkkeOpprettetÅrsak.ÅPEN_BEHANDLING, KanIkkeOppretteRevurderingÅrsak.ÅPEN_BEHANDLING),
    INGEN_BEHANDLING(IkkeOpprettetÅrsak.INGEN_BEHANDLING, KanIkkeOppretteRevurderingÅrsak.INGEN_BEHANDLING),
}
