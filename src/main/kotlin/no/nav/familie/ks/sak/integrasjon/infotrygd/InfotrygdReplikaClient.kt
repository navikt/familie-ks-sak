package no.nav.familie.ks.sak.integrasjon.infotrygd

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
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

    fun harKontantstøtteIInfotrygd(barnIGjeldendeBehandling: List<BarnMedOpplysningerDto>): Boolean {
        val harKontantstøtteIInfotrygdUri =
            UriComponentsBuilder.fromUri(familieKsInfotrygdUri).pathSegment("harLøpendeKontantstotteIInfotrygd").build().toUri()
        val harKontantstøtte = kallEksternTjeneste<Boolean>(
            tjeneste = "harKontantstøtteIInfotrygd",
            uri = harKontantstøtteIInfotrygdUri,
            formål = "Sjekk om noen av personene i denne behandlingen har løpende kontantstøtte i infotrygd"
        ) {
            postForEntity(
                uri = harKontantstøtteIInfotrygdUri,
                barnIGjeldendeBehandling.tilInnsynsRequest()
            )
        }
        return harKontantstøtte
    }
}

fun List<BarnMedOpplysningerDto>.tilInnsynsRequest(): InnsynRequest {
    return InnsynRequest(barn = this.map { it.ident })
}

data class InnsynRequest(
    val barn: List<String>
)
