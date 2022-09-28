package no.nav.familie.ks.sak.data

import no.nav.commons.foedselsnummer.testutils.FoedselsnummerGenerator
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.RegisterSøknadDto
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDTO
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.Personident
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.Medlemskap
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.sivilstand.GrSivilstand
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.statsborgerskap.GrStatsborgerskap
import java.time.LocalDate
import kotlin.random.Random

val fødselsnummerGenerator = FoedselsnummerGenerator()

fun randomFnr(): String = fødselsnummerGenerator.foedselsnummer().asString

fun randomAktør(fnr: String = randomFnr()): Aktør =
    Aktør(Random.nextLong(1000_000_000_000, 31_121_299_99999).toString()).also {
        it.personidenter.add(randomPersonident(it, fnr))
    }

fun randomPersonident(aktør: Aktør, fnr: String = randomFnr()): Personident =
    Personident(fødselsnummer = fnr, aktør = aktør)

fun fnrTilAktør(fnr: String, toSisteSiffrer: String = "00") = Aktør(fnr + toSisteSiffrer).also {
    it.personidenter.add(Personident(fnr, aktør = it))
}

fun lagPersonopplysningGrunnlag(
    behandlingId: Long,
    søkerPersonIdent: String,
    barnasIdenter: List<String>,
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

fun lagRegisterSøknadDto() = RegisterSøknadDto(
    søknad = SøknadDTO(
        søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = randomFnr()),
        barnaMedOpplysninger = listOf(BarnMedOpplysningerDto(ident = randomFnr())),
        endringAvOpplysningerBegrunnelse = ""
    ),
    bekreftEndringerViaFrontend = true
)
