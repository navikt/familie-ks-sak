package no.nav.familie.ks.sak.integrasjon.pdl

import com.neovisionaries.i18n.CountryCode
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.exception.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ks.sak.config.PersonInfoQuery
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.logger
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfoMaskert
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonData
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PersonInfo
import no.nav.familie.ks.sak.kjerne.falskidentitet.FalskIdentitetService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import org.springframework.stereotype.Service
import kotlin.collections.getOrDefault

@Service
class PersonopplysningerService(
    private val pdlKlient: PdlKlient,
    private val integrasjonService: IntegrasjonService,
    private val personidentService: PersonidentService,
    private val falskIdentitetService: FalskIdentitetService,
) {
    fun hentPdlPersonInfoMedRelasjonerOgRegisterinformasjon(aktør: Aktør): PdlPersonInfo {
        val pdlPersoninfo = hentPersoninfoMedQuery(aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON)
        val personinfo =
            when (pdlPersoninfo) {
                is PdlPersonInfo.Person -> pdlPersoninfo.personInfo
                is PdlPersonInfo.FalskPerson -> return PdlPersonInfo.FalskPerson(pdlPersoninfo.falskIdentitetPersonInfo)
            }
        return PdlPersonInfo.Person(personinfo.medRelasjonerOgEgenAnsattInfo(aktør))
    }

    fun hentPersonInfoMedRelasjonerOgRegisterinformasjon(aktør: Aktør): PersonInfo {
        val pdlPersonInfo = hentPersoninfoMedQuery(aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON)
        val personInfo =
            when (pdlPersonInfo) {
                is PdlPersonInfo.Person -> pdlPersonInfo.personInfo
                is PdlPersonInfo.FalskPerson -> throw FunksjonellFeil(PERSON_HAR_FALSK_IDENTITET)
            }
        return personInfo.medRelasjonerOgEgenAnsattInfo(aktør)
    }

    private fun PersonInfo.medRelasjonerOgEgenAnsattInfo(aktør: Aktør): PersonInfo {
        val relasjonsidenter = this.forelderBarnRelasjoner.map { it.aktør.aktivFødselsnummer() }
        val egenAnsattPerIdent = integrasjonService.sjekkErEgenAnsattBulk(setOf(aktør.aktivFødselsnummer()) + relasjonsidenter)

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
                            val relasjonData = hentPdlPersonInfoEnkel(forelderBarnRelasjon.aktør).personInfoBase()
                            ForelderBarnRelasjonInfo(
                                aktør = forelderBarnRelasjon.aktør,
                                relasjonsrolle = forelderBarnRelasjon.relasjonsrolle,
                                fødselsdato = relasjonData.fødselsdato,
                                navn = relasjonData.navn,
                                kjønn = relasjonData.kjønn,
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

        return this.copy(
            forelderBarnRelasjoner = forelderBarnRelasjonerMedAdressebeskyttelseGradering,
            forelderBarnRelasjonerMaskert = forelderBarnRelasjonMaskert,
            erEgenAnsatt = egenAnsattPerIdent.getOrDefault(aktør.aktivFødselsnummer(), null),
        )
    }

    fun hentAdressebeskyttelseSomSystembruker(aktør: Aktør): ADRESSEBESKYTTELSEGRADERING = pdlKlient.hentAdressebeskyttelse(aktør).tilAdressebeskyttelse()

    fun hentPdlPersonInfoEnkel(aktør: Aktør): PdlPersonInfo = hentPersoninfoMedQuery(aktør, PersonInfoQuery.ENKEL)

    fun hentPersoninfoEnkel(aktør: Aktør): PersonInfo {
        val pdlPersonInfo = hentPersoninfoMedQuery(aktør, PersonInfoQuery.ENKEL)
        when (pdlPersonInfo) {
            is PdlPersonInfo.Person -> return pdlPersonInfo.personInfo
            else -> throw FunksjonellFeil(PERSON_HAR_FALSK_IDENTITET)
        }
    }

    fun hentGjeldendeStatsborgerskap(aktør: Aktør): Statsborgerskap = pdlKlient.hentStatsborgerskapUtenHistorikk(aktør).firstOrNull() ?: UKJENT_STATSBORGERSKAP

    fun hentLandkodeUtenlandskBostedsadresse(aktør: Aktør): String {
        val landkode = pdlKlient.hentUtenlandskBostedsadresse(aktør)?.landkode
        return if (landkode.isNullOrEmpty()) UKJENT_LANDKODE else landkode
    }

    private fun hentPersoninfoMedQuery(
        aktør: Aktør,
        personInfoQuery: PersonInfoQuery,
    ): PdlPersonInfo =
        try {
            PdlPersonInfo.Person(tilPersonInfo(pdlPersonData = pdlKlient.hentPerson(aktør, personInfoQuery), personInfoQuery = personInfoQuery))
        } catch (e: PdlPersonKanIkkeBehandlesIFagsystem) {
            val falskIdentitet = falskIdentitetService.hentFalskIdentitet(aktør)
            if (falskIdentitet != null) {
                PdlPersonInfo.FalskPerson(
                    falskIdentitetPersonInfo = falskIdentitet,
                )
            } else {
                throw e
            }
        }

    fun tilPersonInfo(
        pdlPersonData: PdlPersonData,
        personInfoQuery: PersonInfoQuery,
    ): PersonInfo =
        when (personInfoQuery) {
            PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON -> tilPersonInfoMedRelasjoner(pdlPersonData)
            else -> tilPersonInfo(pdlPersonData)
        }

    fun tilPersonInfoMedRelasjoner(pdlPersonData: PdlPersonData): PersonInfo {
        val forelderBarnRelasjoner: Set<ForelderBarnRelasjonInfo> =
            pdlPersonData.forelderBarnRelasjon
                .mapNotNull { relasjon ->
                    val ident = relasjon.relatertPersonsIdent ?: return@mapNotNull null

                    try {
                        ForelderBarnRelasjonInfo(
                            aktør = personidentService.hentAktør(ident),
                            relasjonsrolle = relasjon.relatertPersonsRolle,
                        )
                    } catch (e: PdlPersonKanIkkeBehandlesIFagsystem) {
                        logger.warn("Person kunne ikke bli lagret ned grunnet manglende folkeregisteridentifikator, se securelogger")
                        secureLogger.warn("Person med ident $ident ble ikke lagret ned grunnet manglende folkeregisteridentifikator", e)
                        null
                    }
                }.toSet()
        return tilPersonInfo(pdlPersonData).copy(forelderBarnRelasjoner = forelderBarnRelasjoner)
    }

    fun hentLandkodeAlpha2UtenlandskBostedsadresse(aktør: Aktør): String {
        val landkode = pdlKlient.hentUtenlandskBostedsadresse(aktør)?.landkode

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
        val adresseBeskyttelseBolk = pdlKlient.hentAdressebeskyttelseBolk(personIdenter)
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
        const val PERSON_HAR_FALSK_IDENTITET = "Person har falsk identitet."

        val UKJENT_STATSBORGERSKAP =
            Statsborgerskap(land = "XUK", bekreftelsesdato = null, gyldigFraOgMed = null, gyldigTilOgMed = null)
    }
}
