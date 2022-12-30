package no.nav.familie.ks.sak.kjerne.tilbakekreving

import no.nav.familie.ks.sak.integrasjon.tilbakekreving.TilbakekrevingKlient
import org.springframework.stereotype.Service

// Opprettet denne for å unngå sirkular avhengihet
@Service
class TilbakekrevingBehandlingHentService(private val tilbakekrevingKlient: TilbakekrevingKlient) {

    fun hentTilbakekrevingsbehandlinger(fagsakId: Long) = tilbakekrevingKlient.hentTilbakekrevingsbehandlinger(fagsakId)
}
