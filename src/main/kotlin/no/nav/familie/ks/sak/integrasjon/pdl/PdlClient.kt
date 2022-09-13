package no.nav.familie.ks.sak.integrasjon.pdl

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import no.nav.familie.ks.sak.common.kallEksternTjeneste
import no.nav.familie.ks.sak.config.PdlConfig
import no.nav.familie.ks.sak.config.PdlConfig.Companion.hentAdressebeskyttelseQuery
import no.nav.familie.ks.sak.config.PdlConfig.Companion.hentIdenterQuery
import no.nav.familie.ks.sak.config.PdlConfig.Companion.httpHeaders
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlAdressebeskyttelseResponse
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlBaseResponse
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlHentIdenterResponse
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlIdent
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonRequest
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonRequestVariables
import no.nav.familie.ks.sak.kjerne.personident.Aktør
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

    private val pdlUri = pdlConfig.pdlUri

    override val pingUri: URI get() = pdlUri

    @Cacheable("identer", cacheManager = "shortCache")
    fun hentIdenter(personIdent: String, historikk: Boolean): List<PdlIdent> {
        val pdlPersonRequest = lagPdlPersonRequest(personIdent, hentIdenterQuery)

        val pdlResponse: PdlBaseResponse<PdlHentIdenterResponse> = kallEksternTjeneste(
            tjeneste = "pdl",
            uri = pdlUri,
            formål = "Hent identer"
        ) {
            postForEntity(
                pdlUri,
                pdlPersonRequest,
                httpHeaders()
            )
        }

        val pdlIdenter = feilsjekkOgReturnerData(ident = personIdent, pdlResponse = pdlResponse) { it.pdlIdenter }
        return if (historikk) pdlIdenter.identer.map { it } else pdlIdenter.identer.filter { !it.historisk }.map { it }
    }

    @Cacheable("adressebeskyttelse", cacheManager = "shortCache")
    fun hentAdressebeskyttelse(aktør: Aktør): List<Adressebeskyttelse> {
        val pdlPersonRequest = lagPdlPersonRequest(aktør.aktivFødselsnummer(), hentAdressebeskyttelseQuery)
        val pdlResponse: PdlBaseResponse<PdlAdressebeskyttelseResponse> = kallEksternTjeneste(
            tjeneste = "pdl",
            uri = pdlUri,
            formål = "Hent adressebeskyttelse"
        ) {
            postForEntity(pdlUri, pdlPersonRequest, httpHeaders())
        }

        return feilsjekkOgReturnerData(ident = aktør.aktivFødselsnummer(), pdlResponse = pdlResponse) {
            it.person?.adressebeskyttelse
        }
    }

    private fun lagPdlPersonRequest(aktivFødselsnummer: String, query: String) = PdlPersonRequest(
        variables = PdlPersonRequestVariables(aktivFødselsnummer),
        query = query
    )
}
