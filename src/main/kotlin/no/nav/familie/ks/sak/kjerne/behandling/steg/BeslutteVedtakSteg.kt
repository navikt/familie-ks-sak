package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.ks.sak.api.dto.BehandlingStegDto
import no.nav.familie.ks.sak.api.dto.BesluttVedtakDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.FeatureToggleConfig
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.integrasjon.oppgave.FerdigstillOppgaverTask
import no.nav.familie.ks.sak.integrasjon.oppgave.OpprettOppgaveTask
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ks.sak.kjerne.brev.GenererBrevService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class BeslutteVedtakSteg(
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
    private val taskService: TaskService,
    private val loggService: LoggService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val unleashService: UnleashNextMedContextService,
    private val genererBrevService: GenererBrevService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.BESLUTTE_VEDTAK

    @Transactional
    override fun utførSteg(
        behandlingId: Long,
        behandlingStegDto: BehandlingStegDto,
    ) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
        val behandling = behandlingService.hentBehandling(behandlingId)

        validerAtBehandlingKanBesluttes(behandling)

        val besluttVedtakDto = behandlingStegDto as BesluttVedtakDto

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

            // Oppdater vedtaksbrev med beslutter
            val vedtak = vedtakService.hentAktivVedtakForBehandling(behandlingId)

            vedtak.vedtaksdato = LocalDateTime.now()
            if (behandling.skalSendeVedtaksbrev()) {
                val brev = genererBrevService.genererBrevForBehandling(behandling.id)
                vedtak.stønadBrevPdf = brev
            }

            vedtakService.oppdaterVedtak(vedtak)
        } else {
            val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId)
            // Her oppdaterer vi endretAv til beslutter saksbehandler og endretTid til næværende tidspunkt
            vilkårsvurderingService.oppdater(vilkårsvurdering)

            vedtakService.opprettOgInitierNyttVedtakForBehandling(
                behandling = behandling,
                kopierVedtakBegrunnelser = true,
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
                !unleashService.isEnabled(FeatureToggleConfig.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV) ->
                throw FunksjonellFeil(
                    melding =
                        "Årsak ${BehandlingÅrsak.KORREKSJON_VEDTAKSBREV.visningsnavn} og " +
                            "toggle ${FeatureToggleConfig.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV} false",
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

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BeslutteVedtakSteg::class.java)
    }
}
