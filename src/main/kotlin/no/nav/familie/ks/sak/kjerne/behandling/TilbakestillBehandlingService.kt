package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakestillBehandlingService(
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val stegService: StegService,
    private val tilbakekrevingRepository: TilbakekrevingRepository
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

        // Sletter tilbakekreving
        tilbakekrevingRepository.findByBehandlingId(behandlingId)?.let { tilbakekrevingRepository.deleteById(it.id) }

        // tilbakefører Behandling til gitt behandlingSteg
        stegService.tilbakeførSteg(behandlingId, behandlingSteg)
    }
}
