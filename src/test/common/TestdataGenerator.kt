package no.nav.familie.ks.sak.data

import no.nav.commons.foedselsnummer.testutils.FoedselsnummerGenerator
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.RegistrerSøknadDto
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.Personident
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Medlemskap
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.bostedsadresse.GrBostedsadresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.sivilstand.GrSivilstand
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.statsborgerskap.GrStatsborgerskap
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

val fødselsnummerGenerator = FoedselsnummerGenerator()

fun randomFnr(): String = fødselsnummerGenerator.foedselsnummer().asString

fun randomAktørId(): String = Random.nextLong(1000_000_000_000, 31_121_299_99999).toString()

fun randomAktør(fnr: String = randomFnr()): Aktør =
    Aktør(randomAktørId()).also {
        it.personidenter.add(
            randomPersonident(it, fnr)
        )
    }

fun randomPersonident(aktør: Aktør, fnr: String = randomFnr()): Personident =
    Personident(fødselsnummer = fnr, aktør = aktør)

fun fnrTilAktør(fnr: String, toSisteSiffrer: String = "00") = Aktør(fnr + toSisteSiffrer).also {
    it.personidenter.add(Personident(fnr, aktør = it))
}

fun lagPersonopplysningGrunnlag(
    behandlingId: Long,
    søkerPersonIdent: String,
    barnasIdenter: List<String> = emptyList(), // FGB med register søknad steg har ikke barnasidenter
    barnasFødselsdatoer: List<LocalDate> = barnasIdenter.map { LocalDate.of(2019, 1, 1) },
    søkerAktør: Aktør = fnrTilAktør(søkerPersonIdent).also {
        it.personidenter.add(
            Personident(
                fødselsnummer = søkerPersonIdent,
                aktør = it,
                aktiv = søkerPersonIdent == it.personidenter.first().fødselsnummer
            )
        )
    },
    barnAktør: List<Aktør> = barnasIdenter.map { fødselsnummer ->
        fnrTilAktør(fødselsnummer).also {
            it.personidenter.add(
                Personident(
                    fødselsnummer = fødselsnummer,
                    aktør = it,
                    aktiv = fødselsnummer == it.personidenter.first().fødselsnummer
                )
            )
        }
    }
): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)

    val søker = Person(
        aktør = søkerAktør,
        type = PersonType.SØKER,
        personopplysningGrunnlag = personopplysningGrunnlag,
        fødselsdato = LocalDate.of(2019, 1, 1),
        navn = "",
        kjønn = Kjønn.KVINNE
    ).also { søker ->
        søker.statsborgerskap =
            mutableListOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = søker))
        søker.bostedsadresser = mutableListOf()
        søker.sivilstander = mutableListOf(GrSivilstand(type = SIVILSTAND.GIFT, person = søker))
    }
    personopplysningGrunnlag.personer.add(søker)

    barnAktør.mapIndexed { index, aktør ->
        personopplysningGrunnlag.personer.add(
            Person(
                aktør = aktør,
                type = PersonType.BARN,
                personopplysningGrunnlag = personopplysningGrunnlag,
                fødselsdato = barnasFødselsdatoer.get(index),
                navn = "",
                kjønn = Kjønn.MANN
            ).also { barn ->
                barn.statsborgerskap =
                    mutableListOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = barn))
                barn.bostedsadresser = mutableListOf()
                barn.sivilstander = mutableListOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = barn))
            }
        )
    }
    return personopplysningGrunnlag
}

fun lagFagsak(aktør: Aktør = randomAktør(randomFnr())) = Fagsak(aktør = aktør)

fun lagBehandling(
    fagsak: Fagsak = lagFagsak(),
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    opprettetÅrsak: BehandlingÅrsak,
    kategori: BehandlingKategori = BehandlingKategori.NASJONAL
): Behandling = Behandling(
    fagsak = fagsak,
    type = type,
    opprettetÅrsak = opprettetÅrsak,
    kategori = kategori
).initBehandlingStegTilstand()

fun lagRegistrerSøknadDto() = RegistrerSøknadDto(
    søknad = SøknadDto(
        søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = randomFnr()),
        barnaMedOpplysninger = listOf(BarnMedOpplysningerDto(ident = randomFnr())),
        endringAvOpplysningerBegrunnelse = ""
    ),
    bekreftEndringerViaFrontend = true
)

fun lagPdlPersonInfo(enkelPersonInfo: Boolean = false, erBarn: Boolean = false) = PdlPersonInfo(
    fødselsdato = if (erBarn) LocalDate.now().minusYears(1) else LocalDate.of(1987, 5, 1),
    navn = "John Doe",
    kjønn = KJOENN.MANN,
    forelderBarnRelasjoner = if (enkelPersonInfo) emptySet() else setOf(lagForelderBarnRelasjon()),
    bostedsadresser = listOf(lagBostedsadresse()),
    sivilstander = listOf(lagSivilstand()),
    statsborgerskap = listOf(lagStatsborgerskap())
)

fun lagForelderBarnRelasjon(): ForelderBarnRelasjonInfo = ForelderBarnRelasjonInfo(
    aktør = randomAktør(),
    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
    navn = "Ny barn",
    fødselsdato = LocalDate.now().minusYears(1)
)

fun lagBostedsadresse(): Bostedsadresse = Bostedsadresse(
    gyldigFraOgMed = LocalDate.of(2015, 1, 1),
    vegadresse = Vegadresse(
        matrikkelId = 1234,
        husnummer = "3",
        husbokstav = null,
        bruksenhetsnummer = null,
        adressenavn = "OTTO SVERDRUPS VEG",
        kommunenummer = "1560",
        postnummer = "6650",
        tilleggsnavn = null
    )
)

fun lagSivilstand(): Sivilstand = Sivilstand(type = SIVILSTAND.UGIFT, gyldigFraOgMed = LocalDate.of(2004, 12, 2))

fun lagStatsborgerskap(land: String = "NOR"): Statsborgerskap = Statsborgerskap(
    land = land,
    gyldigFraOgMed = LocalDate.of(1987, 9, 1),
    gyldigTilOgMed = null,
    bekreftelsesdato = LocalDate.of(1987, 9, 1)
)

fun lagInitieltTilkjentYtelse(behandling: Behandling) =
    TilkjentYtelse(behandling = behandling, opprettetDato = LocalDate.now(), endretDato = LocalDate.now())

fun lagAndelTilkjentYtelse(
    tilkjentYtelse: TilkjentYtelse? = null,
    behandling: Behandling,
    aktør: Aktør? = null
) = AndelTilkjentYtelse(
    behandlingId = behandling.id,
    tilkjentYtelse = tilkjentYtelse ?: lagInitieltTilkjentYtelse(behandling),
    aktør = aktør ?: behandling.fagsak.aktør,
    kalkulertUtbetalingsbeløp = 1054,
    stønadFom = YearMonth.now().minusMonths(1),
    stønadTom = YearMonth.now().plusMonths(8),
    type = YtelseType.ORDINÆR_KONTANTSTØTTE,
    sats = 1054,
    prosent = BigDecimal(100),
    nasjonaltPeriodebeløp = 1054
)

fun lagPerson(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    aktør: Aktør,
    personType: PersonType = PersonType.SØKER
): Person {
    val person = Person(
        type = personType,
        fødselsdato = LocalDate.now().minusYears(30),
        kjønn = Kjønn.KVINNE,
        personopplysningGrunnlag = personopplysningGrunnlag,
        aktør = aktør
    )
    person.bostedsadresser = mutableListOf(GrBostedsadresse.fraBostedsadresse(lagBostedsadresse(), person))
    person.statsborgerskap =
        mutableListOf(GrStatsborgerskap.fraStatsborgerskap(lagStatsborgerskap(), Medlemskap.NORDEN, person))
    person.sivilstander = mutableListOf(GrSivilstand.fraSivilstand(lagSivilstand(), person))

    return person
}
