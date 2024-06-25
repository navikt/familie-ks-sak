package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.TilbakestillStegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakestillBehandlingService(
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilbakestillStegService: TilbakestillStegService,
) {
    @Transactional
    fun tilbakestillBehandlingTilVilkårsvurdering(behandlingId: Long) {
        resettBehandlingTilTidligereSteg(behandlingId, BehandlingSteg.VILKÅRSVURDERING)
    }

    @Transactional
    fun tilbakestillBehandlingTilBehandlingsresultat(behandlingId: Long) {
        resettBehandlingTilTidligereSteg(behandlingId, BehandlingSteg.BEHANDLINGSRESULTAT)
    }

    private fun resettBehandlingTilTidligereSteg(
        behandlingId: Long,
        behandlingSteg: BehandlingSteg,
    ) {
        // Sletter vedtaksperioder
        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(behandlingId)

        // Sletter tilbakekreving
        tilbakekrevingRepository.findByBehandlingId(behandlingId)?.let { tilbakekrevingRepository.deleteById(it.id) }

        // tilbakefører Behandling til gitt behandlingSteg kun når steget eksisterer
        val behandling = behandlingRepository.hentAktivBehandling(behandlingId)
        if (behandling.behandlingStegTilstand.any { it.behandlingSteg == behandlingSteg }) {
            tilbakestillStegService.tilbakeførSteg(behandlingId, behandlingSteg)
        }
    }
}
