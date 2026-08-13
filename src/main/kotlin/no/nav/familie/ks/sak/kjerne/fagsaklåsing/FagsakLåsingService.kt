package no.nav.familie.ks.sak.kjerne.fagsaklåsing

import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.AvsluttSakRequest
import no.nav.familie.kontrakter.felles.dokarkiv.DokarkivBruker
import no.nav.familie.kontrakter.felles.dokarkiv.GjenåpneSakRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsstatus
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.integrasjon.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.PubliserSaksstatistikkTask
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.klage.KlagebehandlingHenter
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext.hentSaksbehandlerNavn
import no.nav.familie.ks.sak.task.GjenåpneFagsakIDokarkivTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus as KlageBehandlingStatus

@Service
class FagsakLåsingService(
    private val fagsakRepository: FagsakRepository,
    private val fagsakLåsingRepository: FagsakLåsingRepository,
    private val integrasjonKlient: IntegrasjonKlient,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val featureToggleService: FeatureToggleService,
    private val taskService: TaskRepositoryWrapper,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val vedtakRepository: VedtakRepository,
    private val klagebehandlingHenter: KlagebehandlingHenter,
    private val tilbakekrevingKlient: TilbakekrevingKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun låsFagsak(fagsakId: Long): FagsakLåsingResultat {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_LÅSE_FAGSAK)) {
            return ikkeLåst(fagsakId, "Toggle for låsing av fagsak er av")
        }

        val fagsak = fagsakRepository.finnFagsak(fagsakId) ?: throw Feil("Fant ikke fagsak $fagsakId")

        årsakTilAtFagsakIkkeSkalLåses(fagsak)?.let { return ikkeLåst(fagsakId, it) }

        val sisteVedtatteBehandling =
            behandlingService.hentSisteBehandlingSomErVedtatt(fagsakId)
                ?: throw Feil("Fant ingen vedtatt behandling på fagsak $fagsakId")

        val sisteStønadTom = tilkjentYtelseRepository.hentOptionalTilkjentYtelseForBehandling(sisteVedtatteBehandling.id)?.stønadTom
        val vedtaksdato = vedtakRepository.findByBehandlingAndAktivOptional(sisteVedtatteBehandling.id)?.vedtaksdato

        // stønadTom er en måned, og 1-årsfristen løper fra siste dag i måneden det ble utbetalt for
        val sisteUtbetalingEllerVedtaksdato =
            listOfNotNull(sisteStønadTom?.sisteDagIInneværendeMåned(), vedtaksdato?.toLocalDate()).maxOrNull()
                ?: throw Feil("Fant hverken stønad tom-dato eller vedtaksdato på fagsak $fagsakId")

        val låsedato = sisteUtbetalingEllerVedtaksdato.plusYears(1).atStartOfDay()

        if (LocalDateTime.now() < låsedato) {
            return ikkeLåst(fagsakId, "Fagsaken skal ikke låses før $låsedato")
        }

        val barnPåFagsak =
            personopplysningGrunnlagService
                .hentSøkerOgBarnPåFagsak(fagsakId)
                .orEmpty()
                .filter { it.type == PersonType.BARN }
                .ifEmpty { throw Feil("Fant ingen barn på fagsak $fagsakId") }

        lagreOgDeaktiverGammel(
            FagsakLåsing(
                fagsak = fagsak,
                tidspunkt = låsedato,
                hendelse = FagsakLåsHendelse.LÅST,
                begrunnelse = "Automatisk låst iht. arkivloven fordi siste utbetaling eller vedtak på fagsaken var $sisteUtbetalingEllerVedtaksdato",
                aktiv = true,
            ),
        )

        oppdaterStatus(fagsak, FagsakStatus.LÅST)

        val arbeidsfordeling = arbeidsfordelingService.hentArbeidsfordelingsenhetPåIdenter(fagsak.aktør.aktivFødselsnummer(), barnPåFagsak.map { it.aktør.aktivFødselsnummer() }, null)

        integrasjonKlient.avsluttSak(
            AvsluttSakRequest(
                tema = Tema.KON,
                fagsakId = fagsakId.toString(),
                fagsaksystem = Fagsystem.KONT,
                bruker = DokarkivBruker(BrukerIdType.FNR, fagsak.aktør.aktivFødselsnummer()),
                opprettetDato = fagsak.opprettetTidspunkt,
                avsluttetDato = låsedato,
                administrativEnhet = arbeidsfordeling.enhetId,
            ),
        )
        logger.info("Fagsak $fagsakId er låst og meldt til Joark")
        return FagsakLåsingResultat.Låst
    }

    private fun ikkeLåst(
        fagsakId: Long,
        årsak: String,
    ): FagsakLåsingResultat.IkkeLåst {
        logger.info("Fagsak $fagsakId ble ikke låst: $årsak")
        return FagsakLåsingResultat.IkkeLåst(årsak)
    }

    private fun årsakTilAtFagsakIkkeSkalLåses(fagsak: Fagsak): String? {
        if (fagsak.status != FagsakStatus.AVSLUTTET) {
            return "Status for fagsaken er ${fagsak.status}"
        }

        val aktivLåsForFagsak = finnAktivLåsForFagsak(fagsak.id)

        if (aktivLåsForFagsak?.hendelse == FagsakLåsHendelse.LÅST) {
            throw Feil("Fagsak ${fagsak.id} med status ${fagsak.status} har allerede en aktiv låsing.")
        }

        if (aktivLåsForFagsak?.opprettetTidspunkt?.isAfter(LocalDateTime.now().minusDays(30)) == true) {
            return "Fagsaken ble låst opp for under 30 dager siden"
        }

        if (erÅpenBehandlingPåFagsak(fagsak.id)) {
            return "Fagsaken har åpen behandling"
        }

        val klagebehandlinger = klagebehandlingHenter.hentKlagebehandlingerPåFagsak(fagsak.id)
        if (klagebehandlinger.any { it.status != KlageBehandlingStatus.FERDIGSTILT }) {
            return "Fagsaken har åpen klagebehandling"
        }

        val tilbakekrevingsbehandlinger = tilbakekrevingKlient.hentTilbakekrevingsbehandlinger(fagsak.id)
        if (tilbakekrevingsbehandlinger.any { it.status != Behandlingsstatus.AVSLUTTET }) {
            return "Fagsaken har åpen tilbakekrevingsbehandling"
        }

        val sisteAvsluttetTidspunkt =
            listOfNotNull(
                sisteAvsluttedeBehandlingTidspunkt(fagsak.id),
                klagebehandlinger.mapNotNull { it.vedtaksdato }.maxOrNull(),
                tilbakekrevingsbehandlinger.mapNotNull { it.vedtaksdato }.maxOrNull(),
            ).maxOrNull()

        if (sisteAvsluttetTidspunkt != null && sisteAvsluttetTidspunkt.isAfter(LocalDateTime.now().minusYears(1))) {
            return "Fagsaken hadde siste avsluttede behandling $sisteAvsluttetTidspunkt, som er for under 1 år siden"
        }

        return null
    }

    @Transactional
    fun låsOppFagsak(
        fagsakId: Long,
        begrunnelseForÅLåseOppFagsak: String,
    ): Fagsak {
        val fagsak = fagsakRepository.finnFagsak(fagsakId) ?: throw Feil("Finner ikke fagsak med id $fagsakId")

        if (fagsak.status != FagsakStatus.LÅST) {
            throw FunksjonellFeil("Fagsaken må ha status LÅST for å kunne låses opp. Nåværende status: ${fagsak.status}")
        }

        if (begrunnelseForÅLåseOppFagsak.isBlank()) {
            throw FunksjonellFeil("Begrunnelse kan ikke være tom")
        }

        lagreOgDeaktiverGammel(
            FagsakLåsing(
                fagsak = fagsak,
                tidspunkt = LocalDateTime.now(),
                hendelse = FagsakLåsHendelse.LÅST_OPP,
                begrunnelse = begrunnelseForÅLåseOppFagsak,
                aktiv = true,
            ),
        )

        oppdaterStatus(fagsak, FagsakStatus.AVSLUTTET)

        // Gjenåpningen i Joark kan ikke rulles tilbake, så den legges på task og kjører først når
        // opplåsingen faktisk er commitet.
        taskService.save(GjenåpneFagsakIDokarkivTask.opprettTask(fagsakId))

        return fagsak
    }

    private fun finnAktivLåsForFagsak(fagsakId: Long) = fagsakLåsingRepository.finnAktivLåsForFagsak(fagsakId = fagsakId)

    private fun erÅpenBehandlingPåFagsak(fagsakId: Long): Boolean = behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId) != null

    private fun sisteAvsluttedeBehandlingTidspunkt(fagsakId: Long): LocalDateTime? =
        behandlingRepository
            .finnBehandlinger(fagsakId)
            .filter { it.status == BehandlingStatus.AVSLUTTET }
            .maxOfOrNull { it.endretTidspunkt }

    private fun lagreOgDeaktiverGammel(fagsakLåsing: FagsakLåsing): FagsakLåsing {
        val aktivFagsakLåsing = fagsakLåsingRepository.finnAktivLåsForFagsak(fagsakLåsing.fagsak.id)

        if (aktivFagsakLåsing != null && aktivFagsakLåsing.id != fagsakLåsing.id) {
            fagsakLåsingRepository.saveAndFlush(aktivFagsakLåsing.also { it.aktiv = false })
        }

        return fagsakLåsingRepository.save(fagsakLåsing)
    }

    private fun oppdaterStatus(
        fagsak: Fagsak,
        nyStatus: FagsakStatus,
    ): Fagsak {
        logger.info("${hentSaksbehandlerNavn()} endrer status på fagsak ${fagsak.id} fra ${fagsak.status} til $nyStatus")
        fagsak.status = nyStatus

        return lagre(fagsak)
    }

    private fun lagre(fagsak: Fagsak): Fagsak {
        logger.info("${hentSaksbehandlerNavn()} oppdaterer fagsak $fagsak")
        return fagsakRepository.save(fagsak).also { taskService.save(PubliserSaksstatistikkTask.lagTask(it.id)) }
    }
}

sealed interface FagsakLåsingResultat {
    data object Låst : FagsakLåsingResultat

    data class IkkeLåst(
        val årsak: String,
    ) : FagsakLåsingResultat
}
