package no.nav.familie.ks.sak.fake

import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE.BARN
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE.FAR
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE.MEDMOR
import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.pdl.PdlClient
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfoMaskert
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate
import kotlin.collections.emptySet

class FakePersonopplysningerService(
    pdlClient: PdlClient,
    integrasjonService: IntegrasjonService,
    personidentService: PersonidentService,
) : PersonopplysningerService(
        pdlClient,
        integrasjonService,
        personidentService,
    ) {
    init {
        settPersoninfoMedRelasjonerForPredefinerteTestpersoner()
    }

    override fun hentPersonInfoMedRelasjonerOgRegisterinformasjon(aktør: Aktør): PdlPersonInfo {
        validerFødselsnummer(aktør.aktivFødselsnummer())
        sjekkPersonIkkeFunnet(aktør.aktivFødselsnummer())

        return personInfo[aktør.aktivFødselsnummer()] ?: personInfo.getValue(INTEGRASJONER_FNR)
    }

    override fun hentPersoninfoEnkel(aktør: Aktør): PdlPersonInfo =
        personInfo[aktør.aktivFødselsnummer()]
            ?: personInfo.getValue(INTEGRASJONER_FNR)

    override fun hentAdressebeskyttelseSomSystembruker(aktør: Aktør): ADRESSEBESKYTTELSEGRADERING =
        if (aktør.aktivFødselsnummer() == BARN_DET_IKKE_GIS_TILGANG_TIL_FNR) {
            ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
        } else {
            personInfo[aktør.aktivFødselsnummer()]?.adressebeskyttelseGradering ?: ADRESSEBESKYTTELSEGRADERING.UGRADERT
        }

    override fun hentGjeldendeStatsborgerskap(aktør: Aktør): Statsborgerskap =
        personInfo[aktør.aktivFødselsnummer()]?.statsborgerskap?.firstOrNull()
            ?: Statsborgerskap(
                "NOR",
                LocalDate.of(1990, 1, 25),
                LocalDate.of(1990, 1, 25),
                null,
            )

    override fun hentLandkodeAlpha2UtenlandskBostedsadresse(aktør: Aktør): String = personerMedLandkode[aktør.aktivFødselsnummer()] ?: "NO"

    companion object {
        val mockBarnBehandlingFnr = "21131777001"
        val mockBarnBehandling =
            PdlPersonInfo(
                fødselsdato = LocalDate.now(),
                navn = "ARTIG MIDTPUNKT",
                kjønn = KJOENN.KVINNE,
                adressebeskyttelseGradering = null,
                bostedsadresser = emptyList(),
                sivilstander = emptyList(),
                opphold = emptyList(),
                statsborgerskap = emptyList(),
            )

        val mockSøkerBehandling =
            PdlPersonInfo(
                fødselsdato = LocalDate.parse("1962-08-04"),
                navn = "LEALAUS GYNGEHEST",
                kjønn = KJOENN.KVINNE,
                forelderBarnRelasjoner =
                    setOf(
                        ForelderBarnRelasjonInfo(
                            aktør = randomAktør(mockBarnBehandlingFnr),
                            relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                            navn = null,
                            fødselsdato = null,
                            adressebeskyttelseGradering =
                            null,
                        ),
                    ),
                forelderBarnRelasjonerMaskert = emptySet(),
                adressebeskyttelseGradering = null,
                bostedsadresser = emptyList(),
                sivilstander = listOf(Sivilstand(type = SIVILSTANDTYPE.UGIFT, gyldigFraOgMed = null)),
                opphold = emptyList(),
                statsborgerskap = emptyList(),
            )

        val mockSøkerBehandlingFnr = "04136226623"

        val personInfo: MutableMap<String, PdlPersonInfo> =
            mutableMapOf(
                mockBarnBehandlingFnr to mockBarnBehandling,
                mockSøkerBehandlingFnr to mockSøkerBehandling,
            )

        val personInfoIkkeFunnet: MutableSet<String> = mutableSetOf()

        val personerMedLandkode: MutableMap<String, String> = mutableMapOf()

        fun sjekkPersonIkkeFunnet(personIdent: String) {
            if (personInfoIkkeFunnet.contains(personIdent)) {
                throw notFoundException()
            }
        }

        fun validerFødselsnummer(fødselsnummer: String) {
            try {
                Fødselsnummer(fødselsnummer)
            } catch (e: IllegalStateException) {
                throw notFoundException()
            }
        }

        private fun notFoundException() =
            HttpClientErrorException(
                HttpStatus.NOT_FOUND,
                "Fant ikke forespurte data på person.",
            )

        fun leggTilRelasjonIPersonInfo(
            personIdent: String,
            relatertPersonsIdent: String,
            relatertPersonsRelasjonsrolle: FORELDERBARNRELASJONROLLE,
        ) {
            personInfo[personIdent] =
                personInfo[personIdent]!!.copy(
                    forelderBarnRelasjoner =
                        personInfo[personIdent]!!.forelderBarnRelasjoner +
                            ForelderBarnRelasjonInfo(
                                aktør = randomAktør(relatertPersonsIdent),
                                relasjonsrolle = relatertPersonsRelasjonsrolle,
                                navn = personInfo.getValue(relatertPersonsIdent).navn,
                                fødselsdato = personInfo.getValue(relatertPersonsIdent).fødselsdato,
                                adressebeskyttelseGradering = personInfo.getValue(relatertPersonsIdent).adressebeskyttelseGradering,
                            ),
                )
        }

        fun leggTilRelasjonIPersonInfoMaskert(
            personIdent: String,
            maskertPersonsRelasjonsrolle: FORELDERBARNRELASJONROLLE,
        ) {
            personInfo[personIdent] =
                personInfo[personIdent]!!.copy(
                    forelderBarnRelasjonerMaskert =
                        personInfo[personIdent]!!.forelderBarnRelasjonerMaskert +
                            ForelderBarnRelasjonInfoMaskert(
                                relasjonsrolle = maskertPersonsRelasjonsrolle,
                                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG,
                            ),
                )
        }

        fun settPersoninfoMedRelasjonerForPredefinerteTestpersoner() {
            val (søker1, søker2, søker3) = søkerFnr
            val (barn1, barn2) = barnFnr

            personInfo[søker1] = personInfoSøker1
            personInfo[søker2] = personInfoSøker2
            personInfo[søker3] = personInfoSøker3
            personInfo[barn1] = personInfoBarn1
            personInfo[barn2] = personInfoBarn2
            personInfo[INTEGRASJONER_FNR] = personInfoIntegrasjonerFnr

            leggTilRelasjonIPersonInfo(søker1, barn1, BARN)
            leggTilRelasjonIPersonInfo(søker1, barn2, BARN)
            leggTilRelasjonIPersonInfo(søker1, søker2, MEDMOR)

            leggTilRelasjonIPersonInfo(søker2, barn1, BARN)
            leggTilRelasjonIPersonInfo(søker2, barn2, BARN)
            leggTilRelasjonIPersonInfo(søker2, søker1, FAR)

            leggTilRelasjonIPersonInfo(søker3, barn1, BARN)
            leggTilRelasjonIPersonInfo(søker3, barn2, BARN)
            leggTilRelasjonIPersonInfo(søker3, søker1, FAR)
            leggTilRelasjonIPersonInfoMaskert(søker3, BARN)

            leggTilRelasjonIPersonInfo(INTEGRASJONER_FNR, barn1, BARN)
            leggTilRelasjonIPersonInfo(INTEGRASJONER_FNR, barn2, BARN)
            leggTilRelasjonIPersonInfo(INTEGRASJONER_FNR, søker2, MEDMOR)
        }
    }
}

private const val BARN_DET_IKKE_GIS_TILGANG_TIL_FNR = "12345678912"
private const val INTEGRASJONER_FNR: String = "10000111111"

private val søkerFnr = arrayOf("12345678910", "11223344556", "12345678911")
private val barnFnr = arrayOf(randomFnr(), randomFnr())

private val bostedsadresse =
    Bostedsadresse(
        matrikkeladresse =
            Matrikkeladresse(
                matrikkelId = 123L,
                bruksenhetsnummer = "H301",
                tilleggsnavn = "navn",
                postnummer = "0202",
                kommunenummer = "2231",
            ),
    )

private val bostedsadresseHistorikk =
    mutableListOf(
        Bostedsadresse(
            angittFlyttedato = LocalDate.now().minusDays(15),
            gyldigTilOgMed = null,
            matrikkeladresse =
                Matrikkeladresse(
                    matrikkelId = 123L,
                    bruksenhetsnummer = "H301",
                    tilleggsnavn = "navn",
                    postnummer = "0202",
                    kommunenummer = "2231",
                ),
        ),
        Bostedsadresse(
            angittFlyttedato = LocalDate.now().minusYears(1),
            gyldigTilOgMed = LocalDate.now().minusDays(16),
            matrikkeladresse =
                Matrikkeladresse(
                    matrikkelId = 123L,
                    bruksenhetsnummer = "H301",
                    tilleggsnavn = "navn",
                    postnummer = "0202",
                    kommunenummer = "2231",
                ),
        ),
    )

private val sivilstandHistorisk =
    listOf(
        Sivilstand(type = SIVILSTANDTYPE.GIFT, gyldigFraOgMed = LocalDate.now().minusMonths(8)),
        Sivilstand(type = SIVILSTANDTYPE.SKILT, gyldigFraOgMed = LocalDate.now().minusMonths(4)),
    )

private val personInfoSøker1 =
    PdlPersonInfo(
        fødselsdato = LocalDate.of(1990, 2, 19),
        kjønn = KJOENN.KVINNE,
        navn = "Mor Moresen",
        bostedsadresser = bostedsadresseHistorikk,
        sivilstander = sivilstandHistorisk,
        statsborgerskap =
            listOf(
                Statsborgerskap(
                    land = "DNK",
                    bekreftelsesdato = LocalDate.now().minusYears(1),
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = null,
                ),
            ),
    )

private val personInfoBarn1 =
    PdlPersonInfo(
        fødselsdato = LocalDate.now().withDayOfMonth(10).minusYears(6),
        bostedsadresser = mutableListOf(bostedsadresse),
        sivilstander =
            listOf(
                Sivilstand(
                    type = SIVILSTANDTYPE.UOPPGITT,
                    gyldigFraOgMed = LocalDate.now().minusMonths(8),
                ),
            ),
        kjønn = KJOENN.MANN,
        navn = "Gutten Barnesen",
    )

private val personInfoBarn2 =
    PdlPersonInfo(
        fødselsdato = LocalDate.now().withDayOfMonth(18).minusYears(2),
        bostedsadresser = mutableListOf(bostedsadresse),
        sivilstander =
            listOf(
                Sivilstand(
                    type = SIVILSTANDTYPE.GIFT,
                    gyldigFraOgMed = LocalDate.now().minusMonths(8),
                ),
            ),
        kjønn = KJOENN.KVINNE,
        navn = "Jenta Barnesen",
        adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.FORTROLIG,
    )

private val personInfoSøker2 =
    PdlPersonInfo(
        fødselsdato = LocalDate.of(1995, 2, 19),
        bostedsadresser = mutableListOf(),
        sivilstander =
            listOf(
                Sivilstand(
                    type = SIVILSTANDTYPE.GIFT,
                    gyldigFraOgMed = LocalDate.now().minusMonths(8),
                ),
            ),
        kjønn = KJOENN.MANN,
        navn = "Far Faresen",
    )

private val personInfoSøker3 =
    PdlPersonInfo(
        fødselsdato = LocalDate.of(1985, 7, 10),
        bostedsadresser = mutableListOf(),
        sivilstander =
            listOf(
                Sivilstand(
                    type = SIVILSTANDTYPE.GIFT,
                    gyldigFraOgMed = LocalDate.now().minusMonths(8),
                ),
            ),
        kjønn = KJOENN.KVINNE,
        navn = "Moder Jord",
        adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
    )

private val personInfoIntegrasjonerFnr =
    PdlPersonInfo(
        fødselsdato = LocalDate.of(1965, 2, 19),
        bostedsadresser = mutableListOf(bostedsadresse),
        kjønn = KJOENN.KVINNE,
        navn = "Mor Integrasjon person",
        sivilstander = sivilstandHistorisk,
    )
