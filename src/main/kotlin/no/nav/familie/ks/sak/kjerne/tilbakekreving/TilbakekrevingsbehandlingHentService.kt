package no.nav.familie.ks.sak.kjerne.tilbakekreving

import no.nav.familie.ks.sak.integrasjon.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import org.springframework.stereotype.Service

// Opprettet denne for å unngå sirkular avhengihet
@Service
class TilbakekrevingsbehandlingHentService(
    private val tilbakekrevingKlient: TilbakekrevingKlient,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
) {

    fun hentTilbakekrevingsbehandlinger(fagsakId: Long) = tilbakekrevingKlient.hentTilbakekrevingsbehandlinger(fagsakId)

    fun hentTilbakekreving(behandlingId: Long) = tilbakekrevingRepository.findByBehandlingId(behandlingId)
}
