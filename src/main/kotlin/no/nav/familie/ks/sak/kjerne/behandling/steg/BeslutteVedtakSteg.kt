package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.ks.sak.api.dto.BehandlingStegDto
import no.nav.familie.ks.sak.api.dto.BesluttVedtakDto
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.integrasjon.oppgave.FerdigstillOppgaverTask
import no.nav.familie.ks.sak.integrasjon.oppgave.OpprettOppgaveTask
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerAdresseValidering
import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class BeslutteVedtakSteg(
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
    private val taskService: TaskRepositoryWrapper,
    private val loggService: LoggService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val featureToggleService: FeatureToggleService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val brevmottakerService: BrevmottakerService,
    private val tilbakekrevingService: TilbakekrevingService,
    private val simuleringService: SimuleringService,
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.BESLUTTE_VEDTAK

    @Transactional
    override fun utførSteg(
        behandlingId: Long,
        behandlingStegDto: BehandlingStegDto,
    ) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
        val behandling = behandlingService.hentBehandling(behandlingId)

        if (behandling.erTekniskEndring() &&
            !featureToggleService.isEnabled(
                FeatureToggle.TEKNISK_ENDRING,
                behandling.id,
            )
        ) {
            throw FunksjonellFeil(
                "Du har ikke tilgang til å beslutte en behandling med årsak=${behandling.opprettetÅrsak.visningsnavn}. Ta kontakt med teamet dersom dette ikke stemmer.",
            )
        }

        val besluttVedtakDto = behandlingStegDto as BesluttVedtakDto

        validerAtBehandlingKanBesluttes(behandling)

        validerBrevmottakere(BehandlingId(behandling.id), besluttVedtakDto.beslutning.erGodkjent())

        val totrinnskontroll =
            totrinnskontrollService.besluttTotrinnskontroll(
                behandlingId = behandling.id,
                beslutter = SikkerhetContext.hentSaksbehandlerNavn(),
                beslutterId = SikkerhetContext.hentSaksbehandler(),
                beslutning = besluttVedtakDto.beslutning,
                kontrollerteSider = besluttVedtakDto.kontrollerteSider,
            )

        // opprett historikkinnslag
        loggService.opprettBeslutningOmVedtakLogg(
            behandling,
            besluttVedtakDto.beslutning,
            behandlingStegDto.begrunnelse,
        )

        // ferdigstill GodkjenneVedtak oppgave
        opprettTaskFerdigstillGodkjenneVedtak(behandling = behandling)

        if (besluttVedtakDto.beslutning.erGodkjent()) {
            tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(behandling)

            val erÅpenTilbakekrevingPåFagsak = tilbakekrevingService.harÅpenTilbakekrevingsbehandling(behandling.fagsak.id)

            if (!erÅpenTilbakekrevingPåFagsak) {
                val feilutbetaling = simuleringService.hentFeilutbetaling(behandlingId = behandling.id)
                val tilbakekrevingsvalg = tilbakekrevingService.finnTilbakekrevingsbehandling(behandling.id)?.valg

                validerErTilbakekrevingHvisFeilutbetaling(feilutbetaling, tilbakekrevingsvalg)
            }

            // Oppdater vedtaksbrev med beslutter
            vedtakService.oppdaterVedtakMedDatoOgStønadsbrev(behandling)
        } else {
            val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId)
            // Her oppdaterer vi endretAv til beslutter saksbehandler og endretTid til næværende tidspunkt
            vilkårsvurderingService.oppdater(vilkårsvurdering)

            vedtakService.opprettOgInitierNyttVedtakForBehandling(
                behandling = behandling,
                kopiervedtaksbegrunnelser = true,
            )

            val behandleUnderkjentVedtakTask =
                OpprettOppgaveTask.opprettTask(
                    behandlingId = behandling.id,
                    oppgavetype = Oppgavetype.BehandleUnderkjentVedtak,
                    tilordnetRessurs = totrinnskontroll.saksbehandlerId,
                    fristForFerdigstillelse = LocalDate.now(),
                )
            taskService.save(behandleUnderkjentVedtakTask)
        }
    }

    private fun validerAtBehandlingKanBesluttes(behandling: Behandling) {
        when {
            behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK ->
                throw FunksjonellFeil("Behandlingen er allerede sendt til oppdrag og venter på kvittering")

            behandling.status == BehandlingStatus.AVSLUTTET ->
                throw FunksjonellFeil("Behandlingen er allerede avsluttet")

            behandling.opprettetÅrsak == BehandlingÅrsak.KORREKSJON_VEDTAKSBREV &&
                !featureToggleService.isEnabled(FeatureToggle.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV) ->
                throw FunksjonellFeil(
                    melding =
                        "Årsak ${BehandlingÅrsak.KORREKSJON_VEDTAKSBREV.visningsnavn} og " +
                            "toggle ${FeatureToggle.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV.navn} false",
                    frontendFeilmelding =
                        "Du har ikke tilgang til å beslutte for denne behandlingen. " +
                            "Ta kontakt med teamet dersom dette ikke stemmer.",
                )
        }
    }

    private fun opprettTaskFerdigstillGodkjenneVedtak(behandling: Behandling) {
        val ferdigstillGodkjenneVedtakTask =
            FerdigstillOppgaverTask.opprettTask(behandling.id, Oppgavetype.GodkjenneVedtak)

        taskService.save(ferdigstillGodkjenneVedtakTask)
    }

    private fun validerBrevmottakere(
        behandlingId: BehandlingId,
        totrinnskontrollErGodkjent: Boolean,
    ) {
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId.id)
        if (totrinnskontrollErGodkjent && !BrevmottakerAdresseValidering.harBrevmottakereGyldigAddresse(brevmottakere)) {
            throw FunksjonellFeil(
                melding = "Det finnes ugyldige brevmottakere, vi kan ikke beslutte vedtaket",
                frontendFeilmelding = "Adressen som er lagt til manuelt har ugyldig format, og vedtaksbrevet kan ikke sendes. Behandlingen må underkjennes, og saksbehandler må legge til manuell adresse på nytt.",
            )
        }
    }

    private fun validerErTilbakekrevingHvisFeilutbetaling(
        feilutbetaling: BigDecimal,
        tilbakekrevingsvalg: Tilbakekrevingsvalg?,
    ) {
        if (feilutbetaling != BigDecimal.ZERO && tilbakekrevingsvalg == null) {
            throw FunksjonellFeil("Det er en feilutbetaling som saksbehandler ikke har tatt stilling til. Saken må underkjennes og sendes tilbake til saksbehandler for ny vurdering.")
        }

        if (feilutbetaling == BigDecimal.ZERO && tilbakekrevingsvalg in listOf(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL, Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_AUTOMATISK, Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)) {
            throw FunksjonellFeil("Det er valgt å opprette tilbakekrevingssak men det er ikke lenger feilutbetalt beløp. Behandlingen må underkjennes, og saksbehandler må gå tilbake til behandlingsresultatet og trykke neste og fullføre behandlingen på nytt.")
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BeslutteVedtakSteg::class.java)
    }
}
