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
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "familie-ks-infotrygd") {
    fun harKontantstøtteIInfotrygd(barnIGjeldendeBehandling: List<BarnMedOpplysningerDto>): Boolean {
        val harKontantstøtteIInfotrygdUri =
            UriComponentsBuilder
                .fromUri(familieKsInfotrygdUri)
                .pathSegment("harLopendeKontantstotteIInfotrygd")
                .build()
                .toUri()
        val harKontantstøtte =
            kallEksternTjeneste<Boolean>(
                tjeneste = "harKontantstøtteIInfotrygd",
                uri = harKontantstøtteIInfotrygdUri,
                formål = "Sjekk om noen av personene i denne behandlingen har løpende kontantstøtte i infotrygd",
            ) {
                postForEntity(uri = harKontantstøtteIInfotrygdUri, barnIGjeldendeBehandling.tilInnsynsRequest())
            }
        return harKontantstøtte
    }

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

    fun hentAlleBarnasIdenterForLøpendeFagsaker(): List<String> {
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
        return barnMedLøpendeFagsak
    }

    fun hentAlleSøkereOgBarnidenterForLøpendeFagsakerIInfotrygd(): List<SøkerOgBarn> {
        val requestURI =
            UriComponentsBuilder.fromUri(familieKsInfotrygdUri)
                .pathSegment("hent-sokere-og-barn-med-loepende-kontantstøtte")
                .build().toUri()

        return kallEksternTjeneste(
            tjeneste = "Infotrygd replika",
            uri = requestURI,
            formål = "Henter alle henter søkere og barn med løpende kontantstøtte",
        ) { getForEntity(uri = requestURI) }
    }

    fun List<BarnMedOpplysningerDto>.tilInnsynsRequest(): InnsynRequest {
        return InnsynRequest(barn = this.mapNotNull { it.personnummer })
    }
}

data class InnsynRequest(val barn: List<String>)

data class InnsynResponse(val data: List<StonadDto>)

data class StonadDto(
    val fnr: Foedselsnummer,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val belop: Int?,
    val barn: List<BarnDto>,
)

data class BarnDto(val fnr: Foedselsnummer)

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

data class SøkerOgBarn(
    val søkerIdent: String,
    val barnIdenter: List<String>,
)
