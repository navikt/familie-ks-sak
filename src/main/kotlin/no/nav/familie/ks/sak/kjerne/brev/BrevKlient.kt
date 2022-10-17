package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.ks.sak.common.kallEksternTjeneste
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.BegrunnelseDtoMedData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

val FAMILIE_BREV_TJENESTENAVN = "famile-brev"

@Component
class BrevKlient(
    @Value("\${FAMILIE_BREV_API_URL}") private val familieBrevUri: String,
    @Value("\${SANITY_DATASET}") private val sanityDataset: String,
    restOperations: RestOperations
) : AbstractRestClient(restOperations, "familie-brev") {

    fun genererBrev(målform: String, brev: BrevDto): ByteArray {
        val uri = URI.create("$familieBrevUri/api/$sanityDataset/dokument/$målform/${brev.mal.apiNavn}/pdf")

        secureLogger.info("Kaller familie brev($uri) med data ${brev.data.toBrevString()}")
        return kallEksternTjeneste(FAMILIE_BREV_TJENESTENAVN, uri, "Hente pdf for vedtaksbrev") {
            postForEntity(uri, brev.data)
        }
    }

    @Cacheable("begrunnelsestekst", cacheManager = "shortCache")
    fun hentBegrunnelsestekst(begrunnelseData: BegrunnelseDtoMedData): String {
        val uri = URI.create("$familieBrevUri/ks-sak/begrunnelser/${begrunnelseData.apiNavn}/tekst/")
        secureLogger.info("Kaller familie brev($uri) med data $begrunnelseData")

        return kallEksternTjeneste(FAMILIE_BREV_TJENESTENAVN, uri, "Henter begrunnelsestekst") {
            postForEntity(uri, begrunnelseData)
        }
    }
}
