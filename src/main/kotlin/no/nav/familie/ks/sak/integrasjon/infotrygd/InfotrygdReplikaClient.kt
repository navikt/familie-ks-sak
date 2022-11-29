package no.nav.familie.ks.sak.integrasjon.infotrygd

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.ks.sak.integrasjon.kallEksternTjeneste
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class InfotrygdReplikaClient(
    @Value("\${FAMILIE_KS_INFOTRYGD_API_URL}") private val familieKsInfotrygdUri: URI,
    @Qualifier("azure") restOperations: RestOperations
) : AbstractRestClient(restOperations, "familie-ba-infotrygd") {

    fun harKontantstøtteIInfotrygd(personIdenter: List<PersonIdent>): Boolean {
        val harKontantstøtteIInfotrygdUri =
            UriComponentsBuilder.fromUri(familieKsInfotrygdUri).pathSegment("harLøpendeKontantstotteIInfotrygd").build().toUri()
        val harKontantstøtte = kallEksternTjeneste<Boolean>(
            tjeneste = "harKontantstøtteIInfotrygd",
            uri = harKontantstøtteIInfotrygdUri,
            formål = "Sjekk om person har løpende kontantstøtte i infotrygd"
        ) {
            postForEntity(
                uri = harKontantstøtteIInfotrygdUri,
                personIdenter.tilInnsynsRequest()
            )
        }
        return harKontantstøtte
    }
}

fun List<PersonIdent>.tilInnsynsRequest(): InnsynRequest {
    return InnsynRequest(this.map { Foedselsnummer(asString = it.ident) })
}

data class Foedselsnummer(val asString: String)
data class InnsynRequest(
    val fnr: List<Foedselsnummer>
)
