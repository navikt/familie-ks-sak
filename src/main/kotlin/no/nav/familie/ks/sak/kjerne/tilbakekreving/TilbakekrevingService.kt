package no.nav.familie.ks.sak.kjerne.tilbakekreving

import no.nav.familie.ks.sak.integrasjon.tilbakekreving.TilbakekrevingKlient
import org.springframework.stereotype.Service

@Service
class TilbakekrevingService(private val tilbakekrevingKlient: TilbakekrevingKlient) {

    fun harÅpenTilbakekreving(fagsakId: Long): Boolean =
        tilbakekrevingKlient.harÅpenTilbakekrevingsbehandling(fagsakId)
}
