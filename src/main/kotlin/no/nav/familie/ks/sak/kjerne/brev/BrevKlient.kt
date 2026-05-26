package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.kallEksternTjeneste
import no.nav.familie.ks.sak.integrasjon.secureLogger
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseDtoMedData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

const val FAMILIE_BREV_TJENESTENAVN = "famile-brev"

@Component
class BrevKlient(
    @Value("\${FAMILIE_BREV_API_URL}") private val familieBrevUri: String,
    @Value("\${SANITY_DATASET}") private val sanityDataset: String,
    @Qualifier("utenAuthRestClient") private val restClient: RestClient,
) {
    fun genererBrev(
        målform: String,
        brev: BrevDto,
    ): ByteArray {
        val uri = URI.create("$familieBrevUri/api/$sanityDataset/dokument/$målform/${brev.mal.apiNavn}/pdf")

        secureLogger.info("Kaller familie brev($uri) med data ${brev.data.toBrevString()}")

        return try {
            kallEksternTjeneste(
                FAMILIE_BREV_TJENESTENAVN,
                uri,
                "Hente pdf for vedtaksbrev",
            ) {
                restClient
                    .post()
                    .uri(uri)
                    .body(brev.data)
                    .retrieve()
                    .body<ByteArray>()!!
            }
        } catch (exception: HttpClientErrorException.BadRequest) {
            logger.warn("En bad request oppstod ved generering av brev. Se SecureLogs for detaljer.")
            secureLogger.warn("En bad request oppstod ved generering av brev.", exception)
            throw FunksjonellFeil(
                "Det oppsto en feil ved generering av brev. Sjekk at begrunnelsene som er valgt er riktige og kontakt brukerstøtte hvis problemet vedvarer.",
            )
        }
    }

    @Cacheable("begrunnelsestekst", cacheManager = "shortCache")
    fun hentBegrunnelsestekst(begrunnelseData: BegrunnelseDtoMedData): String {
        try {
            val uri = URI.create("$familieBrevUri/ks-sak/begrunnelser/${begrunnelseData.apiNavn}/tekst/")

            secureLogger.info("Kaller familie brev($uri) med data $begrunnelseData")

            return kallEksternTjeneste(
                FAMILIE_BREV_TJENESTENAVN,
                uri,
                "Henter begrunnelsestekst",
            ) {
                restClient
                    .post()
                    .uri(uri)
                    .body(begrunnelseData)
                    .retrieve()
                    .body<String>()!!
            }
        } catch (exception: HttpClientErrorException.BadRequest) {
            logger.warn("En bad request oppstod ved henting av begrunnelsestekst. Se SecureLogs for detaljer.")
            secureLogger.warn("En bad request oppstod ved henting av begrunnelsestekst", exception)
            throw FunksjonellFeil(
                "Begrunnelsen '${begrunnelseData.apiNavn}' passer ikke vedtaksperioden. Hvis du mener dette er feil ta kontakt med team BAKS.",
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BrevKlient::class.java)
    }
}
