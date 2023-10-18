package no.nav.familie.ks.sak.integrasjon.pdl

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.ks.sak.common.exception.PdlNotFoundException
import no.nav.familie.ks.sak.config.PdlConfig
import no.nav.familie.ks.sak.config.PdlConfig.Companion.hentAdressebeskyttelseQuery
import no.nav.familie.ks.sak.config.PdlConfig.Companion.hentBostedsadresseUtenlandskQuery
import no.nav.familie.ks.sak.config.PdlConfig.Companion.hentIdenterQuery
import no.nav.familie.ks.sak.config.PdlConfig.Companion.hentStatsborgerskapUtenHistorikkQuery
import no.nav.familie.ks.sak.config.PdlConfig.Companion.httpHeaders
import no.nav.familie.ks.sak.config.PersonInfoQuery
import no.nav.familie.ks.sak.integrasjon.kallEksternTjeneste
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlAdressebeskyttelseResponse
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlBaseRespons
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlHentIdenterResponse
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlHentPersonResponse
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlIdent
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonData
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonRequest
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonRequestVariables
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlStatsborgerskapResponse
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlUtenlandskAdresssePersonUtenlandskAdresse
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlUtenlandskAdressseResponse
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class PdlClient(
    pdlConfig: PdlConfig,
    @Qualifier("azureClientCredential") val restTemplate: RestOperations,
) : AbstractPingableRestClient(restTemplate, "pdl.personinfo") {
    private val pdlUri = pdlConfig.pdlUri

    override val pingUri: URI get() = pdlUri

    @Cacheable("identer", cacheManager = "shortCache")
    fun hentIdenter(
        personIdent: String,
        historikk: Boolean,
    ): List<PdlIdent> {
        val pdlPersonRequest = lagPdlPersonRequest(personIdent, hentIdenterQuery)

        val pdlRespons: PdlBaseRespons<PdlHentIdenterResponse> =
            kallEksternTjeneste(
                tjeneste = "pdl",
                uri = pdlUri,
                formål = "Hent identer",
            ) {
                postForEntity(
                    pdlUri,
                    pdlPersonRequest,
                    httpHeaders(),
                )
            }

        val pdlIdenter = feilsjekkOgReturnerData(ident = personIdent, pdlRespons = pdlRespons) { it.pdlIdenter }
        return if (historikk) pdlIdenter.identer.map { it } else pdlIdenter.identer.filter { !it.historisk }.map { it }
    }

    @Cacheable("adressebeskyttelse", cacheManager = "shortCache")
    fun hentAdressebeskyttelse(aktør: Aktør): List<Adressebeskyttelse> {
        val pdlPersonRequest = lagPdlPersonRequest(aktør.aktivFødselsnummer(), hentAdressebeskyttelseQuery)
        val pdlRespons: PdlBaseRespons<PdlAdressebeskyttelseResponse> =
            kallEksternTjeneste(
                tjeneste = "pdl",
                uri = pdlUri,
                formål = "Hent adressebeskyttelse",
            ) {
                postForEntity(pdlUri, pdlPersonRequest, httpHeaders())
            }

        return feilsjekkOgReturnerData(ident = aktør.aktivFødselsnummer(), pdlRespons = pdlRespons) {
            it.person?.adressebeskyttelse
        }
    }

    @Cacheable("personopplysninger", cacheManager = "shortCache")
    fun hentPerson(
        aktør: Aktør,
        personInfoQuery: PersonInfoQuery,
    ): PdlPersonData {
        val pdlPersonRequest = lagPdlPersonRequest(aktør.aktivFødselsnummer(), personInfoQuery.query)

        val pdlRespons: PdlBaseRespons<PdlHentPersonResponse> =
            kallEksternTjeneste(
                tjeneste = "pdl",
                uri = pdlUri,
                formål = "Hent person med query ${personInfoQuery.name}",
            ) {
                postForEntity(
                    pdlUri,
                    pdlPersonRequest,
                    httpHeaders(),
                )
            }
        return feilsjekkOgReturnerData(ident = aktør.aktivFødselsnummer(), pdlRespons = pdlRespons) { pdlPerson ->
            pdlPerson.person?.validerOmPersonKanBehandlesIFagsystem() ?: throw PdlNotFoundException()
            pdlPerson.person
        }
    }

    fun hentStatsborgerskapUtenHistorikk(aktør: Aktør): List<Statsborgerskap> {
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(aktør.aktivFødselsnummer()),
                query = hentStatsborgerskapUtenHistorikkQuery,
            )

        val pdlResponse: PdlBaseRespons<PdlStatsborgerskapResponse> =
            kallEksternTjeneste(
                tjeneste = "pdl",
                uri = pdlUri,
                formål = "Hent statsborgerskap uten historikk",
            ) { postForEntity(pdlUri, pdlPersonRequest, httpHeaders()) }

        return feilsjekkOgReturnerData(
            ident = aktør.aktivFødselsnummer(),
            pdlRespons = pdlResponse,
        ) {
            it.person!!.statsborgerskap
        }
    }

    fun hentUtenlandskBostedsadresse(aktør: Aktør): PdlUtenlandskAdresssePersonUtenlandskAdresse? {
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(aktør.aktivFødselsnummer()),
                query = hentBostedsadresseUtenlandskQuery,
            )
        val pdlResponse: PdlBaseRespons<PdlUtenlandskAdressseResponse> =
            kallEksternTjeneste(
                tjeneste = "pdl",
                uri = pdlUri,
                formål = "Hent utenlandsk bostedsadresse",
            ) {
                postForEntity(pdlUri, pdlPersonRequest, httpHeaders())
            }

        val bostedsadresser =
            feilsjekkOgReturnerData(
                ident = aktør.aktivFødselsnummer(),
                pdlRespons = pdlResponse,
            ) {
                it.person!!.bostedsadresse
            }
        return bostedsadresser.firstOrNull { bostedsadresse -> bostedsadresse.utenlandskAdresse != null }?.utenlandskAdresse
    }

    private fun lagPdlPersonRequest(
        aktivFødselsnummer: String,
        query: String,
    ) = PdlPersonRequest(
        variables = PdlPersonRequestVariables(aktivFødselsnummer),
        query = query,
    )
}
