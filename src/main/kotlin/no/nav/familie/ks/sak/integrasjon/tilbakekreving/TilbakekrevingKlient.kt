package no.nav.familie.ks.sak.integrasjon.tilbakekreving

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.ks.sak.integrasjon.kallEksternTjeneste
import no.nav.familie.ks.sak.integrasjon.kallEksternTjenesteRessurs
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.FAGSYSTEM
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class TilbakekrevingKlient(
    @Value("\${FAMILIE_TILBAKE_API_URL}") private val familieTilbakeUri: URI,
    @Qualifier("azure") restOperations: RestOperations
) : AbstractRestClient(restOperations, "tilbakreving") {

    fun harÅpenTilbakekrevingsbehandling(fagsakId: Long): Boolean {
        val uri = URI.create("$familieTilbakeUri/fagsystem/$FAGSYSTEM/fagsak/$fagsakId/finnesApenBehandling/v1")

        val finnesTilbakekrevingBehandlingsresponsDto: FinnesTilbakekrevingBehandlingsresponsDto = kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Sjekker om en fagsak har åpen tilbakekrevingsbehandling"
        ) { getForEntity(uri) }

        return finnesTilbakekrevingBehandlingsresponsDto.finnesÅpenBehandling
    }

    fun hentForhåndsvisningTilbakekrevingVarselbrev(forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray {
        val uri = URI.create("$familieTilbakeUri/dokument/forhandsvis-varselbrev")

        return kallEksternTjeneste(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Henter forhåndsvisning av varselbrev"
        ) {
            postForEntity(
                uri = uri,
                payload = forhåndsvisVarselbrevRequest,
                httpHeaders = HttpHeaders().apply {
                    accept = listOf(MediaType.APPLICATION_PDF)
                }
            )
        }
    }
}

data class FinnesTilbakekrevingBehandlingsresponsDto(val finnesÅpenBehandling: Boolean)
