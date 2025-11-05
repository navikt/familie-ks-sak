package no.nav.familie.ks.sak.integrasjon.infotrygd

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.commons.foedselsnummer.Kjoenn
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.integrasjon.kallEksternTjeneste
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.YearMonth

@Component
class InfotrygdReplikaClient(
    @Value("\${FAMILIE_KS_INFOTRYGD_API_URL}") private val familieKsInfotrygdUri: URI,
    @Qualifier("jwtBearer") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "familie-ks-infotrygd") {
    fun hentKontantstøttePerioderFraInfotrygd(identer: List<String>): InnsynResponse {
        val requestURI =
            UriComponentsBuilder
                .fromUri(familieKsInfotrygdUri)
                .pathSegment("hentPerioderMedKontantstotteIInfotrygd")
                .build()
                .toUri()

        val infotrygdPerioder =
            kallEksternTjeneste<InnsynResponse>(
                tjeneste = "hentPerioderMedKontantstøtteIInfotrygd",
                uri = requestURI,
                formål = "Henting av kontantstøtte perioder fra infotrygd",
            ) {
                postForEntity(uri = requestURI, InnsynRequest(barn = identer.map { it }))
            }
        return infotrygdPerioder
    }

    fun hentAlleBarnasIdenterForLøpendeFagsaker(): Set<String> {
        val requestURI =
            UriComponentsBuilder
                .fromUri(familieKsInfotrygdUri)
                .pathSegment("hentidentertilbarnmedlopendesaker")
                .build()
                .toUri()

        val barnMedLøpendeFagsak =
            kallEksternTjeneste<List<String>>(
                tjeneste = "hentidentertilbarnmedlopendesaker",
                uri = requestURI,
                formål = "Henter alle barnas identer for løpende fagsaker",
            ) {
                getForEntity(uri = requestURI)
            }
        return barnMedLøpendeFagsak.toSet()
    }

    fun List<BarnMedOpplysningerDto>.tilInnsynsRequest(): InnsynRequest = InnsynRequest(barn = this.mapNotNull { it.personnummer })
}

data class InnsynRequest(
    val barn: List<String>,
)

data class InnsynResponse(
    val data: List<StonadDto>,
)

data class StonadDto(
    val fnr: Foedselsnummer,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val belop: Int?,
    val barn: List<BarnDto>,
)

data class BarnDto(
    val fnr: Foedselsnummer,
)

data class Foedselsnummer(
    @get:JsonValue val asString: String,
) {
    companion object {}

    init {
        require("""\d{11}""".toRegex().matches(asString)) { "Ikke et gyldig fødselsnummer: $asString" }
    }

    val kjoenn: Kjoenn
        get() {
            val kjoenn = asString[8].code
            return if (kjoenn % 2 == 0) Kjoenn.KVINNE else Kjoenn.MANN
        }
}
