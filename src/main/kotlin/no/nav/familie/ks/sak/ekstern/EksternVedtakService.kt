package no.nav.familie.ks.sak.ekstern

import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import org.springframework.stereotype.Service

@Service
class EksternVedtakService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService
) {

    fun hentVedtak(fagsakId: Long): List<FagsystemVedtak> {
        val fagsak = fagsakService.hentFagsak(fagsakId)

        /**
         * TODO når vi har fått inn tilbakekreving
         * val vedtakTilbakekreving = tilbakekrevingClient.finnVedtak(fagsak.id)
         * return hentFerdigstilteBehandlinger(fagsak) + vedtakTilbakekreving
         **/

        return hentFerdigstilteBehandlinger(fagsak)
    }

    private fun hentFerdigstilteBehandlinger(fagsak: Fagsak): List<FagsystemVedtak> {
        return behandlingService.hentBehandlingerPåFagsak(fagsakId = fagsak.id)
            .filter { it.erAvsluttet() && !it.erHenlagt() }
            .map { tilFagsystemVedtak(it) }
    }

    private fun tilFagsystemVedtak(behandling: Behandling): FagsystemVedtak {
        val vedtak = vedtakService.hentAktivVedtakForBehandling(behandling.id)

        return FagsystemVedtak(
            eksternBehandlingId = behandling.id.toString(),
            behandlingstype = behandling.type.visningsnavn,
            resultat = behandling.resultat.displayName,
            vedtakstidspunkt = vedtak.vedtaksdato
                ?: error("Mangler vedtakstidspunkt for behandling=${behandling.id}"),
            fagsystemType = FagsystemType.ORDNIÆR
        )
    }
}
