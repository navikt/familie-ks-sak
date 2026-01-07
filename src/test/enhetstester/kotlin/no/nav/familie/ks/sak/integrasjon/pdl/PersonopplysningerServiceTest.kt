package no.nav.familie.ks.sak.integrasjon.pdl

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.ForelderBarnRelasjon
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.common.exception.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ks.sak.config.PersonInfoQuery
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomPersonident
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlFødselsDato
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlNavn
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonData
import no.nav.familie.ks.sak.kjerne.falskidentitet.FalskIdentitetService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
            every { personidentService.hentAktør(barnIdent.fødselsnummer) } throws PdlPersonKanIkkeBehandlesIFagsystem("test")
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
}
