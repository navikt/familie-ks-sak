package no.nav.familie.ks.sak.integrasjon.pdl

import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.ks.sak.common.exception.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ks.sak.config.PersonInfoQuery
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.logger
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfoMaskert
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonData
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import org.springframework.stereotype.Service

@Service
class PersonOpplysningerService(
    private val pdlClient: PdlClient,
    private val integrasjonClient: IntegrasjonClient,
    private val personidentService: PersonidentService
) {

    fun hentPersonInfoMedRelasjonerOgRegisterinformasjon(aktør: Aktør): PdlPersonInfo {
        val pdlPersonData = hentPersoninfoMedQuery(aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON)
        val forelderBarnRelasjoner: Set<ForelderBarnRelasjonInfo> = pdlPersonData.forelderBarnRelasjon
            .mapNotNull { relasjon ->
                relasjon.relatertPersonsIdent?.let { ident ->
                    ForelderBarnRelasjonInfo(
                        aktør = personidentService.hentAktør(ident),
                        relasjonsrolle = relasjon.relatertPersonsRolle
                    )
                }
            }.toSet()

        val identerMedAdressebeskyttelse = mutableSetOf<Pair<Aktør, FORELDERBARNRELASJONROLLE>>()
        val forelderBarnRelasjonerMedAdressebeskyttelseGradering = forelderBarnRelasjoner.mapNotNull { forelderBarnRelasjon ->
            val harTilgang = integrasjonClient.sjekkTilgangTilPersoner(
                listOf(forelderBarnRelasjon.aktør.aktivFødselsnummer())
            ).harTilgang
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
                        adressebeskyttelseGradering = relasjonData.adressebeskyttelseGradering
                    )
                } catch (pdlPersonKanIkkeBehandlesIFagsystem: PdlPersonKanIkkeBehandlesIFagsystem) {
                    logger.warn("Ignorerer relasjon: ${pdlPersonKanIkkeBehandlesIFagsystem.årsak}")
                    secureLogger.warn(
                        "Ignorerer relasjon ${forelderBarnRelasjon.aktør.aktivFødselsnummer()} " +
                            "til ${aktør.aktivFødselsnummer()}: ${pdlPersonKanIkkeBehandlesIFagsystem.årsak}"
                    )
                    null
                }
            } else { // disse relasjoner har ikke tilgang til aktør-en og er en KODE6/KODE7 brukere
                identerMedAdressebeskyttelse.add(Pair(forelderBarnRelasjon.aktør, forelderBarnRelasjon.relasjonsrolle))
                null
            }
        }.toSet()

        val forelderBarnRelasjonMaskert = identerMedAdressebeskyttelse.map {
            ForelderBarnRelasjonInfoMaskert(
                relasjonsrolle = it.second,
                adressebeskyttelseGradering = hentAdressebeskyttelseSomSystembruker(it.first)
            )
        }.toSet()

        return tilPersonInfo(
            pdlPersonData,
            forelderBarnRelasjonerMedAdressebeskyttelseGradering,
            forelderBarnRelasjonMaskert
        )
    }

    fun hentAdressebeskyttelseSomSystembruker(aktør: Aktør): ADRESSEBESKYTTELSEGRADERING =
        pdlClient.hentAdressebeskyttelse(aktør).tilAdressebeskyttelse()

    fun hentPersoninfoEnkel(aktør: Aktør): PdlPersonInfo =
        tilPersonInfo(hentPersoninfoMedQuery(aktør, PersonInfoQuery.ENKEL))

    fun hentGjeldendeStatsborgerskap(aktør: Aktør): Statsborgerskap {
        return pdlClient.hentStatsborgerskapUtenHistorikk(aktør).firstOrNull() ?: UKJENT_STATSBORGERSKAP
    }

    fun hentLandkodeUtenlandskBostedsadresse(aktør: Aktør): String {
        val landkode = pdlClient.hentUtenlandskBostedsadresse(aktør)?.landkode
        return if (landkode.isNullOrEmpty()) UKJENT_LANDKODE else landkode
    }

    private fun hentPersoninfoMedQuery(aktør: Aktør, personInfoQuery: PersonInfoQuery): PdlPersonData =
        pdlClient.hentPerson(aktør, personInfoQuery)

    companion object {

        const val UKJENT_LANDKODE = "ZZ"
        val UKJENT_STATSBORGERSKAP =
            Statsborgerskap(land = "XUK", bekreftelsesdato = null, gyldigFraOgMed = null, gyldigTilOgMed = null)
    }
}
