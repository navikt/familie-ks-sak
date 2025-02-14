package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ks.sak.kjerne.eøs.EøsSkjemaerForNyBehandlingService
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegistrerPersonGrunnlagSteg(
    private val behandlingRepository: BehandlingRepository,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val overgangsordningAndelService: OvergangsordningAndelService,
    private val eøsSkjemaerForNyBehandlingService: EøsSkjemaerForNyBehandlingService,
    private val adopsjonService: AdopsjonService,
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.REGISTRERE_PERSONGRUNNLAG

    @Transactional
    override fun utførSteg(behandlingId: Long) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
        val behandling = behandlingRepository.hentBehandling(behandlingId)
        val sisteVedtattBehandling = hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)

        personopplysningGrunnlagService.opprettPersonopplysningGrunnlag(behandling, sisteVedtattBehandling)

        // Oppretter vilkårsvurdering her for behandlinger(revurdering,teknisk endring) som ikke har opprettet årsak søknad
        if (!behandling.erSøknad()) {
            vilkårsvurderingService.opprettVilkårsvurdering(behandling, sisteVedtattBehandling)
        }

        if (sisteVedtattBehandling != null) {
            endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
                behandling = behandling,
                forrigeBehandling = sisteVedtattBehandling,
            )

            overgangsordningAndelService.kopierOvergangsordningAndelFraForrigeBehandling(
                behandling = behandling,
                forrigeBehandling = sisteVedtattBehandling,
            )

            eøsSkjemaerForNyBehandlingService.kopierEøsSkjemaer(
                forrigeBehandlingSomErVedtattId = sisteVedtattBehandling.behandlingId,
                behandlingId = behandling.behandlingId,
            )

            adopsjonService.kopierAdopsjonerFraForrigeBehandling(
                behandlingId = behandling.behandlingId,
                forrigeBehandlingId = sisteVedtattBehandling.behandlingId,
            )
        }
    }

    private fun hentSisteBehandlingSomErVedtatt(fagsakId: Long): Behandling? =
        behandlingRepository
            .finnBehandlinger(fagsakId)
            .filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.aktivertTidspunkt }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RegistrerPersonGrunnlagSteg::class.java)
    }
}
