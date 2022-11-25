package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakestillBehandlingService(
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val stegService: StegService
) {

    @Transactional
    fun tilbakestillBehandlingTilVilkårsvurdering(behandlingId: Long) {
        resettBehandlingTilTidligereSteg(behandlingId, BehandlingSteg.VILKÅRSVURDERING)
    }

    @Transactional
    fun tilbakestillBehandlingTilBehandlingsresultat(behandlingId: Long) {
        resettBehandlingTilTidligereSteg(behandlingId, BehandlingSteg.BEHANDLINGSRESULTAT)
    }

    private fun resettBehandlingTilTidligereSteg(behandlingId: Long, behandlingSteg: BehandlingSteg) {
        // Sletter vedtaksperioder
        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(behandlingId)

        // TODO slett tilbakekreving

        // tilbakefører Behandling til gitt behandlingSteg
        stegService.tilbakeførSteg(behandlingId, behandlingSteg)
    }
}
