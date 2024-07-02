package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ks.sak.kjerne.eøs.EøsSkjemaerForNyBehandlingService
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
    private val eøsSkjemaerForNyBehandlingService: EøsSkjemaerForNyBehandlingService,
    private val persongrunnlagService: PersonopplysningGrunnlagService,
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
                behandling,
                sisteVedtattBehandling,
            )

            eøsSkjemaerForNyBehandlingService.kopierEøsSkjemaer(
                forrigeBehandlingSomErVedtattId = BehandlingId(sisteVedtattBehandling.id),
                behandlingId = BehandlingId(behandling.id),
            )

            if (behandling.opprettetÅrsak == BehandlingÅrsak.LOVENDRING) {
                genererVilkårsvurderingForSatsendring(sisteVedtattBehandling, behandling)
            }
        }
    }

    private fun genererVilkårsvurderingForSatsendring(
        forrigeBehandlingSomErVedtatt: Behandling,
        inneværendeBehandling: Behandling,
    ): Vilkårsvurdering {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivPersonopplysningGrunnlagThrows(inneværendeBehandling.id)

        val forrigeBehandlingVilkårsvurdering = hentVilkårsvurderingThrows(forrigeBehandlingSomErVedtatt.id)

        val nyVilkårsvurdering =
            forrigeBehandlingVilkårsvurdering.tilKopiForNyBehandling(
                nyBehandling = inneværendeBehandling,
                personopplysningGrunnlag,
            )

        endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
            behandling = inneværendeBehandling,
            forrigeBehandling = forrigeBehandlingSomErVedtatt,
        )

        return vilkårsvurderingService.lagreNyOgDeaktiverGammel(nyVilkårsvurdering)
    }

    private fun hentVilkårsvurdering(behandlingId: Long): Vilkårsvurdering? =
        vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
            behandlingId = behandlingId,
        )

    fun hentVilkårsvurderingThrows(
        behandlingId: Long,
        feilmelding: String? = null,
    ): Vilkårsvurdering =
        hentVilkårsvurdering(behandlingId) ?: throw Feil(
            message = feilmelding ?: "Fant ikke aktiv vilkårsvurdering for behandling $behandlingId",
            frontendFeilmelding = feilmelding ?: "Fant ikke aktiv vilkårsvurdering for behandling.",
        )

    private fun hentSisteBehandlingSomErVedtatt(fagsakId: Long): Behandling? =
        behandlingRepository
            .finnBehandlinger(fagsakId)
            .filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.aktivertTidspunkt }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RegistrerPersonGrunnlagSteg::class.java)
    }
}
