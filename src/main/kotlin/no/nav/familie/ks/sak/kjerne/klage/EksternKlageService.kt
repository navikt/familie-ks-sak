package no.nav.familie.ks.sak.kjerne.klage

import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.ks.sak.integrasjon.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import org.springframework.stereotype.Service

@Service
class EksternKlageService(
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val tilbakekrevingKlient: TilbakekrevingKlient,
) {
    fun hentFagsystemVedtak(fagsakId: Long): List<FagsystemVedtak> {
        val behandlinger = behandlingService.hentFerdigstilteBehandlinger(fagsakId)

        val ferdigstilteKsVedtak = behandlinger.map { tilFagsystemVedtak(it) }
        val vedtakTilbakekreving = tilbakekrevingKlient.hentTilbakekrevingsvedtak(fagsakId)

        return ferdigstilteKsVedtak + vedtakTilbakekreving
    }

    private fun tilFagsystemVedtak(behandling: Behandling): FagsystemVedtak {
        val vedtak = vedtakService.hentAktivVedtakForBehandling(behandling.id)

        return FagsystemVedtak(
            eksternBehandlingId = behandling.id.toString(),
            behandlingstype = behandling.type.visningsnavn,
            resultat = behandling.resultat.displayName,
            vedtakstidspunkt = vedtak.vedtaksdato ?: error("Mangler vedtakstidspunkt for behandling=${behandling.id}"),
            fagsystemType = FagsystemType.ORDNIÆR,
            regelverk = behandling.kategori.tilRegelverk(),
        )
    }
}