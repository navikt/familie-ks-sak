package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.oppgave.FerdigstillOppgaverTask
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.integrasjon.oppgave.OpprettOppgaveTask
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.IBehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.validerPerioderInneholderBegrunnelser
import no.nav.familie.ks.sak.kjerne.brev.GenererBrevService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class VedtakSteg(
    private val behandlingService: BehandlingService,
    private val taskService: TaskService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val loggService: LoggService,
    private val oppgaveService: OppgaveService,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val genererBrevService: GenererBrevService
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.VEDTAK

    @Transactional
    override fun utførSteg(behandlingId: Long) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")

        val behandling = behandlingService.hentBehandling(behandlingId)
        validerAtBehandlingErGyldigForVedtak(behandling)

        loggService.opprettSendTilBeslutterLogg(behandling.id)
        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling)

        val godkjenneVedtakTask = OpprettOppgaveTask.opprettTask(
            behandlingId = behandling.id,
            oppgavetype = Oppgavetype.GodkjenneVedtak,
            fristForFerdigstillelse = LocalDate.now()
        )

        taskService.save(godkjenneVedtakTask)

        opprettFerdigstillOppgaveTasker(behandling)

        val vedtak = vedtakService.hentAktivVedtakForBehandling(behandlingId)

        vedtak.vedtaksdato = LocalDateTime.now()
        if (behandling.skalSendeVedtaksbrev()) {
            val brev = genererBrevService.genererBrevForBehandling(behandling.id)
            vedtak.stønadBrevPdf = brev
        }

        vedtakService.oppdaterVedtak(vedtak)

        behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.FATTER_VEDTAK)
    }

    private fun opprettFerdigstillOppgaveTasker(behandling: Behandling) {
        val oppgaver = oppgaveService.hentOppgaverSomIkkeErFerdigstilt(behandling)
        val relevanteOppgave = oppgaver.filter {
            it.type in listOf(
                Oppgavetype.BehandleSak,
                Oppgavetype.BehandleUnderkjentVedtak,
                Oppgavetype.VurderLivshendelse
            )
        }

        relevanteOppgave.forEach {
            val ferdigstillOppgaverTask = FerdigstillOppgaverTask.opprettTask(behandling.id, it.type)
            taskService.save(ferdigstillOppgaverTask)
        }
    }

    private fun validerAtBehandlingErGyldigForVedtak(behandling: Behandling) {
        if (behandling.erHenlagt()) {
            throw Feil("Behandlingen er henlagt og dermed så kan ikke vedtak foreslås.")
        }

        if (behandling.behandlingStegTilstand.count { it.behandlingStegStatus == BehandlingStegStatus.VENTER || it.behandlingStegStatus == BehandlingStegStatus.KLAR } > 1) {
            throw Feil("Behandlingen har mer enn ett ikke fullført steg.")
        }

        if (behandling.resultat != Behandlingsresultat.FORTSATT_INNVILGET) {
            val vedtak = vedtakService.hentAktivVedtakForBehandling(behandlingId = behandling.id)
            val utvidetVedtaksperioder = vedtaksperiodeService.hentUtvidetVedtaksperioderMedBegrunnelser(vedtak)
            utvidetVedtaksperioder.validerPerioderInneholderBegrunnelser(
                behandlingId = behandling.id,
                fagsakId = behandling.fagsak.id
            )
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(VedtakSteg::class.java)
    }
}
