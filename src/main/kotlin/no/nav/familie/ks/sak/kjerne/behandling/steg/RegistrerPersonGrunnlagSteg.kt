package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RegistrerPersonGrunnlagSteg(
    private val behandlingRepository: BehandlingRepository,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.REGISTRERE_PERSONGRUNNLAG

    override fun utførSteg(behandlingId: Long) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
        val behandling = behandlingRepository.hentBehandling(behandlingId)
        val sisteVedtattBehandling = hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)

        personopplysningGrunnlagService.opprettPersonopplysningGrunnlag(behandling, sisteVedtattBehandling)

        // Oppretter vilkårsvurdering her for revurderinger som ikke har opprettet årsak søknad
        if (behandling.type == BehandlingType.REVURDERING && !behandling.erSøknad()) {
            vilkårsvurderingService.opprettVilkårsvurdering(behandling, sisteVedtattBehandling)
        }

        if (sisteVedtattBehandling != null) {
            // TODO kopier over endretutbetaling fra forrige behandling
        }
    }

    private fun hentSisteBehandlingSomErVedtatt(fagsakId: Long): Behandling? =
        behandlingRepository.finnBehandlinger(fagsakId)
            .filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.opprettetTidspunkt }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RegistrerPersonGrunnlagSteg::class.java)
    }
}
