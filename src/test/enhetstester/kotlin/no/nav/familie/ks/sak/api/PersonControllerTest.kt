package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.ks.sak.api.dto.PersonInfoDto
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.FalskIdentitetPersonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PersonInfo
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.sikkerhet.PersonTilgangService
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

class PersonControllerTest {
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val personidentService = mockk<PersonidentService>()
    private val personopplysningGrunnlagService = mockk<PersonopplysningGrunnlagService>()
    private val personTilgangService = mockk<PersonTilgangService>()
    private val behandlingService = mockk<BehandlingService>()
    private val tilgangService = mockk<TilgangService>()
    private val personController =
        PersonController(
            personidentService = personidentService,
            personOpplysningerService = personopplysningerService,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
            personTilgangService = personTilgangService,
            tilgangService = tilgangService,
            behandlingService = behandlingService,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandling(any(), any()) }
    }

    @Nested
    inner class HentPerson {
        @Test
        fun `skal returnere fullstendig informasjon om person basert på data fra PDL når saksbehandler har tilgang`() {
            // Arrange
            val person = lagPerson()
            val personInfo =
                PersonInfo(
                    fødselsdato = person.fødselsdato,
                    navn = person.navn,
                    kjønn = person.kjønn,
                )
            val pdlPersonInfo = PdlPersonInfo.Person(personInfo = personInfo)

            every { personidentService.hentAktør(person.aktør.aktivFødselsnummer()) } returns person.aktør
            every { personTilgangService.hentMaskertPersonInfoVedManglendeTilgang(person.aktør) } returns null
            every { personopplysningerService.hentPdlPersonInfoMedRelasjonerOgRegisterinformasjon(person.aktør) } returns pdlPersonInfo

            // Act
            val respons = personController.hentPerson(PersonIdent(person.aktør.aktivFødselsnummer()))

            // Assert
            assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(respons.body).isNotNull
            assertThat(respons.body!!.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(respons.body!!.data).isNotNull

            val data: PersonInfoDto = respons.body!!.data!!
            assertThat(data.navn).isEqualTo(person.navn)
            assertThat(data.fødselsdato).isEqualTo(person.fødselsdato)
            assertThat(data.kjønn).isEqualTo(person.kjønn)
        }

        @Test
        fun `skal returnere informasjon om person basert på eksisterende personopplysninger i database dersom person har falsk identitet`() {
            // Arrange
            val person = lagPerson()
            val falskIdentitetPersonInfo =
                FalskIdentitetPersonInfo(
                    fødselsdato = person.fødselsdato,
                    navn = person.navn,
                    kjønn = person.kjønn,
                )
            val pdlPersonInfo = PdlPersonInfo.FalskPerson(falskIdentitetPersonInfo = falskIdentitetPersonInfo)

            every { personidentService.hentAktør(person.aktør.aktivFødselsnummer()) } returns person.aktør
            every { personTilgangService.hentMaskertPersonInfoVedManglendeTilgang(person.aktør) } returns null
            every { personopplysningerService.hentPdlPersonInfoMedRelasjonerOgRegisterinformasjon(person.aktør) } returns pdlPersonInfo

            // Act
            val respons = personController.hentPerson(PersonIdent(person.aktør.aktivFødselsnummer()))

            // Assert
            assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(respons.body).isNotNull
            assertThat(respons.body!!.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(respons.body!!.data).isNotNull

            val data: PersonInfoDto = respons.body!!.data!!
            assertThat(data.navn).isEqualTo(person.navn)
            assertThat(data.fødselsdato).isEqualTo(person.fødselsdato)
            assertThat(data.kjønn).isEqualTo(person.kjønn)
        }

        @Test
        fun `skal returnere maskert informasjon om person dersom saksbehandler ikke har tilgang`() {
            // Arrange
            val person = lagPerson()

            val restPersonInfo =
                PersonInfoDto(
                    personIdent = person.aktør.aktivFødselsnummer(),
                    adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG,
                    harTilgang = false,
                )

            every { personidentService.hentAktør(person.aktør.aktivFødselsnummer()) } returns person.aktør
            every { personTilgangService.hentMaskertPersonInfoVedManglendeTilgang(person.aktør) } returns restPersonInfo

            // Act
            val respons = personController.hentPerson(PersonIdent(person.aktør.aktivFødselsnummer()))

            // Assert
            assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(respons.body).isNotNull
            assertThat(respons.body!!.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(respons.body!!.data).isNotNull

            val data: PersonInfoDto = respons.body!!.data!!
            assertThat(data).isEqualTo(restPersonInfo)
        }
    }

    @Nested
    inner class Tilgangskontroll {
        private val person = lagPerson()
        private val personIdent = PersonIdent(person.aktør.aktivFødselsnummer())

        @BeforeEach
        fun setUp() {
            every { personidentService.hentAktør(personIdent.ident) } returns person.aktør
            every { personTilgangService.hentMaskertPersonInfoVedManglendeTilgang(person.aktør) } returns null
            every { personopplysningerService.hentPdlPersonInfoMedRelasjonerOgRegisterinformasjon(person.aktør) } returns
                PdlPersonInfo.Person(personInfo = PersonInfo(fødselsdato = person.fødselsdato, navn = person.navn, kjønn = person.kjønn))
            every { personopplysningerService.hentPersoninfoEnkel(person.aktør) } returns
                PersonInfo(fødselsdato = person.fødselsdato, navn = person.navn, kjønn = person.kjønn)
        }

        @Test
        fun `skal kreve veilederrolle ved henting av person`() {
            // Act
            personController.hentPerson(personIdent)

            // Assert
            verify(exactly = 1) { tilgangService.validerTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente person") }
        }

        @Test
        fun `skal kreve veilederrolle ved enkel henting av person`() {
            // Act
            personController.hentPersonEnkel(personIdent)

            // Assert
            verify(exactly = 1) { tilgangService.validerTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente person") }
        }

        @Test
        fun `skal ikke slå opp person når kaller mangler veilederrolle`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente person")
            } throws RolleTilgangskontrollFeil("Krever VEILEDER")

            // Act & Assert
            assertThrows<RolleTilgangskontrollFeil> { personController.hentPerson(personIdent) }
            assertThrows<RolleTilgangskontrollFeil> { personController.hentPersonEnkel(personIdent) }

            verify(exactly = 0) { personidentService.hentAktør(any()) }
        }
    }
}
