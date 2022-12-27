package no.nav.familie.ks.sak.integrasjon.tilbakekreving

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.ks.sak.integrasjon.kallEksternTjenesteRessurs
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.FAGSYSTEM
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
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

        val finnesBehandlingsresponsDto: FinnesBehandlingsresponsDto = kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Sjekker om en fagsak har åpen tilbakekrevingsbehandling"
        ) { getForEntity(uri) }

        return finnesBehandlingsresponsDto.finnesÅpenBehandling
    }
}

data class FinnesBehandlingsresponsDto(val finnesÅpenBehandling: Boolean)
