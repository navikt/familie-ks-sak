package no.nav.familie.ks.sak.integrasjon.pdl

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.ks.sak.common.kallEksternTjeneste
import no.nav.familie.ks.sak.config.PdlConfig
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlBaseResponse
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlHentIdenterResponse
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlIdent
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonRequest
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonRequestVariables
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class PdlClient(
    private val pdlConfig: PdlConfig,
    @Qualifier("azureClientCredential") val restTemplate: RestOperations
) : AbstractPingableRestClient(restTemplate, "pdl.personinfo") {

    override val pingUri: URI get() = pdlConfig.pdlUri

    @Cacheable("identer", cacheManager = "shortCache")
    fun hentIdenter(personIdent: String, historikk: Boolean): List<PdlIdent> {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(personIdent),
            query = PdlConfig.hentIdenterQuery
        )

        val pdlResponse: PdlBaseResponse<PdlHentIdenterResponse> = kallEksternTjeneste(
            tjeneste = "pdl",
            uri = pdlConfig.pdlUri,
            formål = "Hent identer"
        ) {
            postForEntity(
                pdlConfig.pdlUri,
                pdlPersonRequest,
                PdlConfig.httpHeaders()
            )
        }

        val pdlIdenter = feilsjekkOgReturnerData(ident = personIdent, pdlResponse = pdlResponse) { it.pdlIdenter }
        return if (historikk) pdlIdenter.identer.map { it } else pdlIdenter.identer.filter { !it.historisk }.map { it }
    }
}
