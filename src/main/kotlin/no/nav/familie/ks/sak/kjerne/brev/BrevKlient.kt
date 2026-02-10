package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.kallEksternTjeneste
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseDtoMedData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI

const val FAMILIE_BREV_TJENESTENAVN = "famile-brev"

@Component
class BrevKlient(
    @Value("\${FAMILIE_BREV_API_URL}") private val familieBrevUri: String,
    @Value("\${SANITY_DATASET}") private val sanityDataset: String,
    restOperations: RestOperations,
) : AbstractRestClient(restOperations, "familie-brev") {
    fun genererBrev(
        målform: String,
        brev: BrevDto,
    ): ByteArray {
        val uri = URI.create("$familieBrevUri/api/$sanityDataset/dokument/$målform/${brev.mal.apiNavn}/pdf")

        secureLogger.info("Kaller familie brev($uri) med data ${brev.data.toBrevString()}")

        return kallEksternTjeneste(
            FAMILIE_BREV_TJENESTENAVN,
            uri,
            "Hente pdf for vedtaksbrev",
        ) {
            postForEntity(uri, brev.data)
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
                postForEntity(uri, begrunnelseData)
            }
        } catch (exception: HttpClientErrorException.BadRequest) {
            log.warn("En bad request oppstod ved henting av begrunnelsestekst. Se SecureLogs for detaljer.")
            secureLogger.warn("En bad request oppstod ved henting av begrunnelsestekst", exception)
            throw FunksjonellFeil(
                "Begrunnelsen '${begrunnelseData.apiNavn}' passer ikke vedtaksperioden. Hvis du mener dette er feil ta kontakt med team BAKS.",
            )
        }
    }
}
