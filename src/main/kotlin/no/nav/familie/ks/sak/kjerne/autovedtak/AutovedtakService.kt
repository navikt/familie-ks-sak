package no.nav.familie.ks.sak.kjerne.autovedtak

import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.OpprettBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.Beslutning
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.iverksettmotoppdrag.IverksettMotOppdragTask
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutovedtakService(
    private val stegService: StegService,
    private val behandlingService: BehandlingService,
    private val opprettBehandlingService: OpprettBehandlingService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val loggService: LoggService,
    private val vedtakService: VedtakService,
    private val fagsakService: FagsakService,
    private val behandlingRepository: BehandlingRepository,
    private val snikeIKøenService: SnikeIKøenService,
    private val taskService: TaskRepositoryWrapper,
) {
    fun opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
        aktør: Aktør,
        behandlingType: BehandlingType,
        behandlingÅrsak: BehandlingÅrsak,
    ): Behandling {
        val nyBehandling =
            opprettBehandlingService.opprettBehandling(
                OpprettBehandlingDto(
                    søkersIdent = aktør.aktivFødselsnummer(),
                    behandlingType = behandlingType,
                    behandlingÅrsak = behandlingÅrsak,
                ),
            )

        stegService.utførSteg(behandlingId = nyBehandling.id, behandlingSteg = BehandlingSteg.VILKÅRSVURDERING)
        stegService.utførSteg(behandlingId = nyBehandling.id, behandlingSteg = BehandlingSteg.BEHANDLINGSRESULTAT)

        return behandlingService.hentBehandling(behandlingId = nyBehandling.id)
    }

    fun opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling: Behandling): Vedtak {
        totrinnskontrollService.opprettAutomatiskTotrinnskontroll(behandling)

        loggService.opprettBeslutningOmVedtakLogg(
            behandling = behandling,
            beslutning = Beslutning.GODKJENT,
            begrunnelse = null,
        )

        return vedtakService.oppdaterVedtakMedDatoOgStønadsbrev(behandling)
    }

    @Transactional
    fun opprettAutovedtakBehandlingPåFagsak(
        fagsakId: Long,
        behandlingÅrsak: BehandlingÅrsak,
        behandlingType: BehandlingType,
    ): Behandling? {
        val fagsak = fagsakService.hentFagsak(fagsakId = fagsakId)

        val aktivOgÅpenBehandling = behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId = fagsakId)

        if (aktivOgÅpenBehandling != null) {
            if (snikeIKøenService.kanSnikeForbi(aktivOgÅpenBehandling)) {
                snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                    aktivOgÅpenBehandling.id,
                    SettPåMaskinellVentÅrsak.LOVENDRING,
                )
            } else {
                throw Feil("Kan ikke gjennomføre revurderingsbehandling for fagsak=$fagsakId fordi det er en åpen behandling vi ikke klarer å snike forbi")
            }
        }

        val søkerAktør = fagsak.aktør
        val behandlingEtterBehandlingsresultat = opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(aktør = søkerAktør, behandlingÅrsak = BehandlingÅrsak.LOVENDRING_2024, behandlingType = BehandlingType.REVURDERING)

        val vedtak = vedtakService.hentAktivVedtakForBehandling(behandlingId = behandlingEtterBehandlingsresultat.id)

        taskService.save(
            IverksettMotOppdragTask.opprettTask(
                behandling = behandlingEtterBehandlingsresultat,
                vedtakId = vedtak.id,
                saksbehandlerId = SikkerhetContext.SYSTEM_FORKORTELSE,
            ),
        )
        return behandlingEtterBehandlingsresultat
    }
}
