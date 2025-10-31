package no.nav.familie.ks.sak.fake

import no.nav.familie.kontrakter.felles.tilbakekreving.Behandling
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.ks.sak.integrasjon.tilbakekreving.TilbakekrevingKlient
import org.springframework.web.client.RestOperations
import java.net.URI

class FakeTilbakekrevingKlient(
    restOperations: RestOperations,
) : TilbakekrevingKlient(URI.create("http://tilbakekreving"), restOperations) {
    var errorVedHentingAvBehandlinger = false
    var returnerteBehandlinger = emptyList<Behandling>()

    override fun opprettTilbakekrevingBehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): String = "id1"

    override fun harÅpenTilbakekrevingsbehandling(fagsakId: Long): Boolean = false

    override fun hentTilbakekrevingsbehandlinger(fagsakId: Long): List<Behandling> {
        if (errorVedHentingAvBehandlinger) throw IllegalStateException("Feilet")
        return returnerteBehandlinger
    }

    fun reset() {
        errorVedHentingAvBehandlinger = false
        returnerteBehandlinger = emptyList<Behandling>()
    }
}
