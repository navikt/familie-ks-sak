package no.nav.familie.ks.sak.integrasjon.pdl

import com.neovisionaries.i18n.CountryCode
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.ks.sak.common.exception.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ks.sak.config.PersonInfoQuery
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.logger
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfoMaskert
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonData
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import org.springframework.stereotype.Service
import kotlin.collections.getOrDefault

@Service
class PersonopplysningerService(
    private val pdlClient: PdlClient,
    private val integrasjonService: IntegrasjonService,
    private val personidentService: PersonidentService,
    private val integrasjonClient: IntegrasjonClient,
) {
    fun hentPersonInfoMedRelasjonerOgRegisterinformasjon(aktør: Aktør): PdlPersonInfo {
        val pdlPersonData = hentPersoninfoMedQuery(aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON)
        val relasjonsidenter = pdlPersonData.forelderBarnRelasjon.mapNotNull { it.relatertPersonsIdent }
        val egenAnsattPerIdent = integrasjonClient.sjekkErEgenAnsattBulk(listOf(aktør.aktivFødselsnummer()) + relasjonsidenter)

        val forelderBarnRelasjoner: Set<ForelderBarnRelasjonInfo> =
            pdlPersonData.forelderBarnRelasjon
                .mapNotNull { relasjon ->
                    val ident = relasjon.relatertPersonsIdent ?: return@mapNotNull null

                    try {
                        ForelderBarnRelasjonInfo(
                            aktør = personidentService.hentAktør(ident),
                            relasjonsrolle = relasjon.relatertPersonsRolle,
                            erEgenAnsatt = egenAnsattPerIdent.getOrDefault(relasjon.relatertPersonsIdent, null),
                        )
                    } catch (e: PdlPersonKanIkkeBehandlesIFagsystem) {
                        logger.warn("Person kunne ikke bli lagret ned grunnet manglende folkeregisteridentifikator, se securelogger")
                        secureLogger.warn("Person med ident $ident ble ikke lagret ned grunnet manglende folkeregisteridentifikator", e)
                        null
                    }
                }.toSet()

        val identerMedAdressebeskyttelse = mutableSetOf<Pair<Aktør, FORELDERBARNRELASJONROLLE>>()
        val forelderBarnRelasjonerMedAdressebeskyttelseGradering =
            forelderBarnRelasjoner
                .mapNotNull { forelderBarnRelasjon ->
                    val harTilgang =
                        integrasjonService.sjekkTilgangTilPerson(forelderBarnRelasjon.aktør.aktivFødselsnummer()).harTilgang

                    if (harTilgang) {
                        try {
                            // henter alle aktive forelder barn relasjoner med adressebeskyttelse gradering
                            // disse relasjoner har tilgang til aktør-en og er ikke en KODE6/KODE7 brukere
                            val relasjonData = hentPersoninfoEnkel(forelderBarnRelasjon.aktør)
                            ForelderBarnRelasjonInfo(
                                aktør = forelderBarnRelasjon.aktør,
                                relasjonsrolle = forelderBarnRelasjon.relasjonsrolle,
                                fødselsdato = relasjonData.fødselsdato,
                                navn = relasjonData.navn,
                                adressebeskyttelseGradering = relasjonData.adressebeskyttelseGradering,
                                erEgenAnsatt = egenAnsattPerIdent.getOrDefault(forelderBarnRelasjon.aktør.aktivFødselsnummer(), null),
                            )
                        } catch (pdlPersonKanIkkeBehandlesIFagsystem: PdlPersonKanIkkeBehandlesIFagsystem) {
                            logger.warn("Ignorerer relasjon: ${pdlPersonKanIkkeBehandlesIFagsystem.årsak}")
                            secureLogger.warn(
                                "Ignorerer relasjon ${forelderBarnRelasjon.aktør.aktivFødselsnummer()} " +
                                    "til ${aktør.aktivFødselsnummer()}: ${pdlPersonKanIkkeBehandlesIFagsystem.årsak}",
                            )
                            null
                        }
                    } else { // disse relasjoner har ikke tilgang til aktør-en og er en KODE6/KODE7 brukere
                        identerMedAdressebeskyttelse.add(Pair(forelderBarnRelasjon.aktør, forelderBarnRelasjon.relasjonsrolle))
                        null
                    }
                }.toSet()

        val forelderBarnRelasjonMaskert =
            identerMedAdressebeskyttelse
                .map {
                    ForelderBarnRelasjonInfoMaskert(
                        relasjonsrolle = it.second,
                        adressebeskyttelseGradering = hentAdressebeskyttelseSomSystembruker(it.first),
                    )
                }.toSet()

        val personInfo =
            tilPersonInfo(
                pdlPersonData,
                forelderBarnRelasjonerMedAdressebeskyttelseGradering,
                forelderBarnRelasjonMaskert,
            )

        return personInfo.copy(erEgenAnsatt = egenAnsattPerIdent.getOrDefault(aktør.aktivFødselsnummer(), null))
    }

    fun hentAdressebeskyttelseSomSystembruker(aktør: Aktør): ADRESSEBESKYTTELSEGRADERING = pdlClient.hentAdressebeskyttelse(aktør).tilAdressebeskyttelse()

    fun hentPersoninfoEnkel(aktør: Aktør): PdlPersonInfo = tilPersonInfo(hentPersoninfoMedQuery(aktør, PersonInfoQuery.ENKEL))

    fun hentGjeldendeStatsborgerskap(aktør: Aktør): Statsborgerskap = pdlClient.hentStatsborgerskapUtenHistorikk(aktør).firstOrNull() ?: UKJENT_STATSBORGERSKAP

    fun hentLandkodeUtenlandskBostedsadresse(aktør: Aktør): String {
        val landkode = pdlClient.hentUtenlandskBostedsadresse(aktør)?.landkode
        return if (landkode.isNullOrEmpty()) UKJENT_LANDKODE else landkode
    }

    private fun hentPersoninfoMedQuery(
        aktør: Aktør,
        personInfoQuery: PersonInfoQuery,
    ): PdlPersonData = pdlClient.hentPerson(aktør, personInfoQuery)

    fun hentLandkodeAlpha2UtenlandskBostedsadresse(aktør: Aktør): String {
        val landkode = pdlClient.hentUtenlandskBostedsadresse(aktør)?.landkode

        if (landkode.isNullOrEmpty()) return UKJENT_LANDKODE

        return if (landkode.length == 3) {
            if (landkode == PDL_UKJENT_LANDKODE) {
                UKJENT_LANDKODE
            } else {
                CountryCode.getByAlpha3Code(landkode.uppercase()).alpha2
            }
        } else {
            landkode
        }
    }

    fun hentIdenterMedStrengtFortroligAdressebeskyttelse(personIdenter: List<String>): List<String> {
        val adresseBeskyttelseBolk = pdlClient.hentAdressebeskyttelseBolk(personIdenter)
        return adresseBeskyttelseBolk
            .filter { (_, person) ->
                person.adressebeskyttelse.any { adressebeskyttelse ->
                    adressebeskyttelse.gradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG ||
                        adressebeskyttelse.gradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND
                }
            }.map { it.key }
    }

    companion object {
        const val PDL_UKJENT_LANDKODE = "XUK"
        const val UKJENT_LANDKODE = "ZZ"
        val UKJENT_STATSBORGERSKAP =
            Statsborgerskap(land = "XUK", bekreftelsesdato = null, gyldigFraOgMed = null, gyldigTilOgMed = null)
    }
}
