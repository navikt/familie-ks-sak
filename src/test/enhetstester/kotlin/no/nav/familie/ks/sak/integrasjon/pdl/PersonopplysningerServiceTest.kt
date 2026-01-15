package no.nav.familie.ks.sak.integrasjon.pdl

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.ForelderBarnRelasjon
import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.exception.PdlPersonKanIkkeBehandlesIFagSystemÅrsak
import no.nav.familie.ks.sak.common.exception.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ks.sak.config.PersonInfoQuery
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomPersonident
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService.Companion.PERSON_HAR_FALSK_IDENTITET
import no.nav.familie.ks.sak.integrasjon.pdl.domene.FalskIdentitetPersonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlFødselsDato
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlKjoenn
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlNavn
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonData
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.falskidentitet.FalskIdentitetService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class PersonopplysningerServiceTest {
    private val pdlKlient = mockk<PdlKlient>()
    private val integrasjonService = mockk<IntegrasjonService>(relaxed = true)
    private val personidentService = mockk<PersonidentService>()
    private val falskIdentitetService = mockk<FalskIdentitetService>()
    private val personopplysningerService =
        PersonopplysningerService(
            pdlKlient = pdlKlient,
            integrasjonService = integrasjonService,
            personidentService = personidentService,
            falskIdentitetService = falskIdentitetService,
        )

    @Nested
    inner class HentPersonInfoMedRelasjonerOgRegisterinformasjon {
        @Test
        fun `Skal returnere personopplysninger med relasjon hvis det finnes`() {
            // Arrange
            val aktør = randomAktør()
            val barnAktør = randomAktør()
            val barnIdent = randomPersonident(barnAktør)
            val fødselsdato = LocalDate.of(2000, 2, 2)

            val forelderBarnRelasjon =
                ForelderBarnRelasjon(
                    relatertPersonsIdent = barnIdent.fødselsnummer,
                    relatertPersonsRolle = FORELDERBARNRELASJONROLLE.BARN,
                )

            val pdlPersonData =
                PdlPersonData(
                    forelderBarnRelasjon = listOf(forelderBarnRelasjon),
                    folkeregisteridentifikator = emptyList(),
                    foedselsdato = listOf(PdlFødselsDato(fødselsdato.toString())),
                    bostedsadresse = emptyList(),
                )

            val pdlRelasjonData =
                PdlPersonData(
                    navn = listOf(PdlNavn("Fornavn", null, "Etternavn")),
                    folkeregisteridentifikator = emptyList(),
                    foedselsdato = listOf(PdlFødselsDato(fødselsdato.toString())),
                    bostedsadresse = emptyList(),
                )

            every { pdlKlient.hentPerson(aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON) } returns pdlPersonData
            every { personidentService.hentAktør(barnIdent.fødselsnummer) } returns barnAktør
            every { integrasjonService.sjekkTilgangTilPerson(barnAktør.aktivFødselsnummer()) } returns Tilgang(barnIdent.fødselsnummer, true)
            every { pdlKlient.hentPerson(barnAktør, PersonInfoQuery.ENKEL) } returns pdlRelasjonData
            every { pdlKlient.hentAdressebeskyttelse(any()) } returns emptyList()

            // Act
            val pdlPersonInfo = personopplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(aktør)

            // Assert
            assertThat(pdlPersonInfo.forelderBarnRelasjoner).hasSize(1)

            val barnRelasjon = pdlPersonInfo.forelderBarnRelasjoner.single()

            assertThat(barnRelasjon.navn).isEqualTo("Fornavn Etternavn")
            assertThat(barnRelasjon.aktør).isEqualTo(barnAktør)
        }

        @Test
        fun `Dersom det kastes PdlPersonKanIkkeBehandlesIFagsystem skal man hoppe over personen i relasjonen`() {
            // Arrange
            val aktør = randomAktør()
            val barnAktør = randomAktør()
            val barnIdent = randomPersonident(barnAktør)
            val fødselsdato = LocalDate.of(2000, 2, 2)

            val forelderBarnRelasjon =
                ForelderBarnRelasjon(
                    relatertPersonsIdent = barnIdent.fødselsnummer,
                    relatertPersonsRolle = FORELDERBARNRELASJONROLLE.BARN,
                )

            val pdlPersonData =
                PdlPersonData(
                    forelderBarnRelasjon = listOf(forelderBarnRelasjon),
                    folkeregisteridentifikator = emptyList(),
                    foedselsdato = listOf(PdlFødselsDato(fødselsdato.toString())),
                    bostedsadresse = emptyList(),
                )

            val pdlRelasjonData =
                PdlPersonData(
                    navn = listOf(PdlNavn("Fornavn", null, "Etternavn")),
                    folkeregisteridentifikator = emptyList(),
                    foedselsdato = listOf(PdlFødselsDato(fødselsdato.toString())),
                    bostedsadresse = emptyList(),
                )

            every { pdlKlient.hentPerson(aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON) } returns pdlPersonData
            every { personidentService.hentAktør(barnIdent.fødselsnummer) } throws PdlPersonKanIkkeBehandlesIFagsystem(PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
            every { integrasjonService.sjekkTilgangTilPerson(barnAktør.aktivFødselsnummer()) } returns Tilgang(barnIdent.fødselsnummer, true)
            every { pdlKlient.hentPerson(barnAktør, PersonInfoQuery.ENKEL) } returns pdlRelasjonData
            every { pdlKlient.hentAdressebeskyttelse(any()) } returns emptyList()

            // Act
            val pdlPersonInfo = personopplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(aktør)

            // Assert
            assertThat(pdlPersonInfo.forelderBarnRelasjoner).isEmpty()
        }

        @Test
        fun `Skal mappe egen ansatt status basert på respons fra integrasjoner`() {
            // Arrange
            val aktør = randomAktør()
            val barnAktør = randomAktør()
            val fødselsdato = LocalDate.of(2000, 2, 2)

            val forelderBarnRelasjon =
                ForelderBarnRelasjon(
                    relatertPersonsIdent = barnAktør.aktivFødselsnummer(),
                    relatertPersonsRolle = FORELDERBARNRELASJONROLLE.BARN,
                )

            val pdlPersonData =
                PdlPersonData(
                    forelderBarnRelasjon = listOf(forelderBarnRelasjon),
                    folkeregisteridentifikator = emptyList(),
                    foedselsdato = listOf(PdlFødselsDato(fødselsdato.toString())),
                    bostedsadresse = emptyList(),
                )

            val pdlRelasjonData =
                PdlPersonData(
                    navn = listOf(PdlNavn("Fornavn", null, "Etternavn")),
                    folkeregisteridentifikator = emptyList(),
                    foedselsdato = listOf(PdlFødselsDato(fødselsdato.toString())),
                    bostedsadresse = emptyList(),
                )

            every { pdlKlient.hentPerson(aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON) } returns pdlPersonData
            every { personidentService.hentAktør(barnAktør.aktivFødselsnummer()) } returns barnAktør
            every { integrasjonService.sjekkTilgangTilPerson(barnAktør.aktivFødselsnummer()) } returns Tilgang(barnAktør.aktivFødselsnummer(), true)
            every { pdlKlient.hentPerson(barnAktør, PersonInfoQuery.ENKEL) } returns pdlRelasjonData
            every { pdlKlient.hentAdressebeskyttelse(any()) } returns emptyList()

            every {
                integrasjonService.sjekkErEgenAnsattBulk(match { it.containsAll(listOf(barnAktør.aktivFødselsnummer(), aktør.aktivFødselsnummer())) })
            } returns mapOf(barnAktør.aktivFødselsnummer() to false, aktør.aktivFødselsnummer() to true)

            // Act
            val pdlPersonInfo = personopplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(aktør)

            // Assert
            assertThat(pdlPersonInfo.erEgenAnsatt).isTrue()
            assertThat(pdlPersonInfo.forelderBarnRelasjoner.single().erEgenAnsatt).isFalse()
        }
    }

    @Test
    fun `hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon skal returnere PdlPersonInfo av typen Person dersom aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson(navn = "Ola Normann", kjønn = Kjønn.MANN)
        val barn = lagPerson(navn = "Kari Normann", kjønn = Kjønn.KVINNE)
        val søkerPersonInfoData =
            PdlPersonData(
                foedselsdato = listOf(PdlFødselsDato(person.fødselsdato.toString())),
                navn = listOf(PdlNavn("Ola", null, "Normann")),
                kjoenn = listOf(PdlKjoenn(KJOENN.MANN)),
                forelderBarnRelasjon = listOf(ForelderBarnRelasjon(relatertPersonsIdent = barn.aktør.aktivFødselsnummer(), relatertPersonsRolle = FORELDERBARNRELASJONROLLE.BARN)),
                folkeregisteridentifikator = emptyList(),
                bostedsadresse = emptyList(),
            )

        val barnPersonInfoData =
            PdlPersonData(
                foedselsdato = listOf(PdlFødselsDato(barn.fødselsdato.toString())),
                navn = listOf(PdlNavn("Kari", null, "Normann")),
                kjoenn = listOf(PdlKjoenn(KJOENN.KVINNE)),
                folkeregisteridentifikator = emptyList(),
                bostedsadresse = emptyList(),
            )

        every { pdlKlient.hentPerson(person.aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON) } returns søkerPersonInfoData
        every { pdlKlient.hentPerson(barn.aktør, PersonInfoQuery.ENKEL) } returns barnPersonInfoData
        every { integrasjonService.sjekkTilgangTilPerson(any()) } returns Tilgang(personIdent = barn.aktør.aktivFødselsnummer(), harTilgang = true)

        every { integrasjonService.sjekkErEgenAnsattBulk(any()) } returns emptyMap()

        every { personidentService.hentAktør(barn.aktør.aktivFødselsnummer()) } returns barn.aktør

        // Act
        val result = personopplysningerService.hentPdlPersonInfoMedRelasjonerOgRegisterinformasjon(person.aktør)

        // Assert
        assertThat(result is PdlPersonInfo.Person)
        val personInfoResult = (result as PdlPersonInfo.Person).personInfo
        assertThat(personInfoResult.navn).isEqualTo(person.navn)
        assertThat(personInfoResult.fødselsdato).isEqualTo(person.fødselsdato)
        assertThat(personInfoResult.kjønn).isEqualTo(person.kjønn)
        assertThat(personInfoResult.forelderBarnRelasjoner.size).isEqualTo(1)
        val barnRelasjon = personInfoResult.forelderBarnRelasjoner.first()
        assertThat(barnRelasjon.aktør).isEqualTo(barn.aktør)
        assertThat(barnRelasjon.navn).isEqualTo(barn.navn)
        assertThat(barnRelasjon.fødselsdato).isEqualTo(barn.fødselsdato)
        assertThat(barnRelasjon.kjønn).isEqualTo(barn.kjønn)
    }

    @Test
    fun `hentPersoninfoMedRelasjonerOgRegisterinformasjon skal returnere PersonInfo dersom aktør ikke har falsk identitet`() {
        // Arrange
        val søker = lagPerson(navn = "Ola Normann", kjønn = Kjønn.MANN)
        val barn = lagPerson(navn = "Kari Normann", kjønn = Kjønn.KVINNE)
        val søkerPersonInfoData =
            PdlPersonData(
                foedselsdato = listOf(PdlFødselsDato(søker.fødselsdato.toString())),
                navn = listOf(PdlNavn("Ola", null, "Normann")),
                kjoenn = listOf(PdlKjoenn(KJOENN.MANN)),
                forelderBarnRelasjon = listOf(ForelderBarnRelasjon(relatertPersonsIdent = barn.aktør.aktivFødselsnummer(), relatertPersonsRolle = FORELDERBARNRELASJONROLLE.BARN)),
                folkeregisteridentifikator = emptyList(),
                bostedsadresse = emptyList(),
            )

        val barnPersonInfoData =
            PdlPersonData(
                foedselsdato = listOf(PdlFødselsDato(barn.fødselsdato.toString())),
                navn = listOf(PdlNavn("Kari", null, "Normann")),
                kjoenn = listOf(PdlKjoenn(KJOENN.KVINNE)),
                folkeregisteridentifikator = emptyList(),
                bostedsadresse = emptyList(),
            )

        every { pdlKlient.hentPerson(søker.aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON) } returns søkerPersonInfoData
        every { pdlKlient.hentPerson(barn.aktør, PersonInfoQuery.ENKEL) } returns barnPersonInfoData
        every { integrasjonService.sjekkTilgangTilPerson(any()) } returns Tilgang(personIdent = barn.aktør.aktivFødselsnummer(), harTilgang = true)

        every { integrasjonService.sjekkErEgenAnsattBulk(any()) } returns emptyMap()

        every { personidentService.hentAktør(barn.aktør.aktivFødselsnummer()) } returns barn.aktør

        // Act
        val personInfo = personopplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(søker.aktør)

        // Assert
        assertThat(personInfo.navn).isEqualTo(søker.navn)
        assertThat(personInfo.fødselsdato).isEqualTo(søker.fødselsdato)
        assertThat(personInfo.kjønn).isEqualTo(søker.kjønn)
        assertThat(personInfo.forelderBarnRelasjoner.size).isEqualTo(1)
        val barnRelasjon = personInfo.forelderBarnRelasjoner.first()
        assertThat(barnRelasjon.aktør).isEqualTo(barn.aktør)
        assertThat(barnRelasjon.navn).isEqualTo(barn.navn)
        assertThat(barnRelasjon.fødselsdato).isEqualTo(barn.fødselsdato)
        assertThat(barnRelasjon.kjønn).isEqualTo(barn.kjønn)
    }

    @Test
    fun `hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon skal returnere PdlPersonInfo av typen FalskPerson dersom aktør har falsk identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlKlient.hentPerson(person.aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns FalskIdentitetPersonInfo()

        // Act
        val result = personopplysningerService.hentPdlPersonInfoMedRelasjonerOgRegisterinformasjon(person.aktør)

        // Assert
        assertThat(result is PdlPersonInfo.FalskPerson)
        val falskIdentitetPersonInfo = (result as PdlPersonInfo.FalskPerson).falskIdentitetPersonInfo
        assertThat(falskIdentitetPersonInfo.navn).isEqualTo("Ukjent navn")
        assertThat(falskIdentitetPersonInfo.fødselsdato).isNull()
        assertThat(falskIdentitetPersonInfo.kjønn).isEqualTo(Kjønn.UKJENT)
    }

    @Test
    fun `hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon skal kaste feil dersom pdl respons mangler nødvendige felter og aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlKlient.hentPerson(person.aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns null

        // Act & Assert
        val result = assertThrows<PdlPersonKanIkkeBehandlesIFagsystem> { personopplysningerService.hentPdlPersonInfoMedRelasjonerOgRegisterinformasjon(person.aktør) }
        assertThat(result.årsak).isEqualTo(PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
    }

    @Test
    fun `hentPersoninfoMedRelasjonerOgRegisterinformasjon skal kaste feil dersom aktør har falsk identitet`() {
        // Arrange
        val person = lagPerson()
        every { pdlKlient.hentPerson(person.aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns FalskIdentitetPersonInfo()

        // Act & Assert
        val funksjonellFeil = assertThrows<FunksjonellFeil> { personopplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(person.aktør) }
        assertThat(funksjonellFeil.message).isEqualTo(PERSON_HAR_FALSK_IDENTITET)
    }

    @Test
    fun `hentPersoninfoMedRelasjonerOgRegisterinformasjon skal kaste feil dersom pdl respons mangler nødvendige felter og aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson()
        every { pdlKlient.hentPerson(person.aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns null

        // Act & Assert
        val pdlFeil = assertThrows<PdlPersonKanIkkeBehandlesIFagsystem> { personopplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(person.aktør) }
        assertThat(pdlFeil.årsak).isEqualTo(PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
    }

    @Test
    fun `hentPdlPersoninfoEnkel skal returnere PdlPersonInfo av typen Person dersom aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson(navn = "Ola Normann", kjønn = Kjønn.MANN)
        val personInfo =
            PdlPersonData(
                foedselsdato = listOf(PdlFødselsDato(person.fødselsdato.toString())),
                navn = listOf(PdlNavn("Ola", null, "Normann")),
                kjoenn = listOf(PdlKjoenn(KJOENN.MANN)),
                folkeregisteridentifikator = emptyList(),
                bostedsadresse = emptyList(),
            )

        every { pdlKlient.hentPerson(person.aktør, PersonInfoQuery.ENKEL) } returns personInfo

        // Act
        val result = personopplysningerService.hentPdlPersonInfoEnkel(person.aktør)

        // Assert
        assertThat(result is PdlPersonInfo.Person)
        val personInfoResult = (result as PdlPersonInfo.Person).personInfo
        assertThat(personInfoResult.navn).isEqualTo(person.navn)
        assertThat(personInfoResult.fødselsdato).isEqualTo(person.fødselsdato)
        assertThat(personInfoResult.kjønn).isEqualTo(person.kjønn)
    }

    @Test
    fun `hentPersoninfoEnkel skal returnere PersonInfo dersom aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson(navn = "Ola Normann", kjønn = Kjønn.MANN)
        val personInfo =
            PdlPersonData(
                foedselsdato = listOf(PdlFødselsDato(person.fødselsdato.toString())),
                navn = listOf(PdlNavn("Ola", null, "Normann")),
                kjoenn = listOf(PdlKjoenn(KJOENN.MANN)),
                folkeregisteridentifikator = emptyList(),
                bostedsadresse = emptyList(),
            )

        every { pdlKlient.hentPerson(person.aktør, PersonInfoQuery.ENKEL) } returns personInfo

        // Act
        val result = personopplysningerService.hentPersoninfoEnkel(person.aktør)

        // Assert
        assertThat(result.navn).isEqualTo(person.navn)
        assertThat(result.fødselsdato).isEqualTo(person.fødselsdato)
        assertThat(result.kjønn).isEqualTo(person.kjønn)
    }

    @Test
    fun `hentPdlPersoninfoEnkel skal returnere PdlPersonInfo av typen FalskPerson dersom aktør har falsk identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlKlient.hentPerson(person.aktør, PersonInfoQuery.ENKEL) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns FalskIdentitetPersonInfo()

        // Act
        val result = personopplysningerService.hentPdlPersonInfoEnkel(person.aktør)

        // Assert
        assertThat(result is PdlPersonInfo.FalskPerson)
        val falskPersonInfoResult = (result as PdlPersonInfo.FalskPerson).falskIdentitetPersonInfo
        assertThat(falskPersonInfoResult.navn).isEqualTo("Ukjent navn")
        assertThat(falskPersonInfoResult.fødselsdato).isNull()
        assertThat(falskPersonInfoResult.kjønn).isEqualTo(Kjønn.UKJENT)
    }

    @Test
    fun `hentPdlPersoninfoEnkel skal kaste feil dersom pdl respons mangler fnr og aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlKlient.hentPerson(person.aktør, PersonInfoQuery.ENKEL) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns null

        // Act & Assert
        val result = assertThrows<PdlPersonKanIkkeBehandlesIFagsystem> { personopplysningerService.hentPdlPersonInfoEnkel(person.aktør) }
        assertThat(result.årsak).isEqualTo(PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
    }

    @Test
    fun `hentPersoninfoEnkel skal kaste feil dersom pdl respons mangler fnr og aktør har falsk identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlKlient.hentPerson(person.aktør, PersonInfoQuery.ENKEL) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns FalskIdentitetPersonInfo()

        // Act & Assert
        val funksjonellFeil = assertThrows<FunksjonellFeil> { personopplysningerService.hentPersoninfoEnkel(person.aktør) }
        assertThat(funksjonellFeil.message).isEqualTo(PERSON_HAR_FALSK_IDENTITET)
    }

    @Test
    fun `hentPersoninfoEnkel skal kaste feil dersom pdl respons mangler fnr og aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlKlient.hentPerson(person.aktør, PersonInfoQuery.ENKEL) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns null

        // Act & Assert
        val pdlFeil = assertThrows<PdlPersonKanIkkeBehandlesIFagsystem> { personopplysningerService.hentPersoninfoEnkel(person.aktør) }
        assertThat(pdlFeil.årsak).isEqualTo(PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
    }
}
