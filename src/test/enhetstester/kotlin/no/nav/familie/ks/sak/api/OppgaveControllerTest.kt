package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.ks.sak.api.dto.FinnOppgaveDto
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.datagenerator.lagTestJournalpost
import no.nav.familie.ks.sak.datagenerator.lagTestOppgaveDTO
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.journalføring.InnkommendeJournalføringService
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PersonInfo
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OppgaveControllerTest {
    private val oppgaveService = mockk<OppgaveService>()
    private val personOpplysningerService = mockk<PersonopplysningerService>()
    private val personidentService = mockk<PersonidentService>()
    private val fagsakService = mockk<FagsakService>()
    private val integrasjonService = mockk<IntegrasjonService>()
    private val tilgangService = mockk<TilgangService>()
    private val innkommendeJournalføringService = mockk<InnkommendeJournalføringService>()
    private val oppgaveController =
        OppgaveController(
            oppgaveService = oppgaveService,
            personOpplysningerService = personOpplysningerService,
            personidentService = personidentService,
            fagsakService = fagsakService,
            integrasjonService = integrasjonService,
            tilgangService = tilgangService,
            innkommendeJournalføringService = innkommendeJournalføringService,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandling(any(), any()) }
    }

    @Nested
    inner class HentOppgaver {
        private val finnOppgaveDto =
            FinnOppgaveDto(
                behandlingstema = null,
                behandlingstype = null,
                oppgavetype = null,
                enhet = null,
                saksbehandler = null,
                journalpostId = null,
                tilordnetRessurs = null,
                tildeltRessurs = null,
                opprettetFomTidspunkt = null,
                opprettetTomTidspunkt = null,
                fristFomDato = null,
                fristTomDato = null,
                aktivFomDato = null,
                aktivTomDato = null,
                limit = null,
                offset = null,
            )

        @Test
        fun `skal kreve veilederrolle`() {
            // Arrange
            every { oppgaveService.hentOppgaver(any()) } returns FinnOppgaveResponseDto(0, emptyList())

            // Act
            oppgaveController.hentOppgaver(finnOppgaveDto)

            // Assert
            verify(exactly = 1) { tilgangService.validerTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente oppgaver") }
        }

        @Test
        fun `skal gi tilgangsfeil og ikke hente oppgaver når kaller mangler veilederrolle`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente oppgaver")
            } throws RolleTilgangskontrollFeil("Krever VEILEDER")

            // Act & Assert
            assertThrows<RolleTilgangskontrollFeil> { oppgaveController.hentOppgaver(finnOppgaveDto) }

            verify(exactly = 0) { oppgaveService.hentOppgaver(any()) }
        }
    }

    @Nested
    inner class HentDataForManuellJournalføring {
        private val person = lagPerson()
        private val oppgave = lagTestOppgaveDTO(oppgaveId = 1L).copy(aktoerId = person.aktør.aktørId)

        @BeforeEach
        fun setUp() {
            every { oppgaveService.hentOppgave(1L) } returns oppgave
            every { personidentService.hentAktør(person.aktør.aktørId) } returns person.aktør
            every { integrasjonService.hentJournalpost(oppgave.journalpostId!!) } returns
                lagTestJournalpost(personIdent = person.aktør.aktivFødselsnummer(), journalpostId = oppgave.journalpostId!!, avsenderMottakerIdType = null, kanal = "NAV_NO")
            every { personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(person.aktør) } returns
                PersonInfo(fødselsdato = person.fødselsdato, navn = person.navn, kjønn = person.kjønn)
            every { fagsakService.finnMinimalFagsakForPerson(person.aktør.aktørId) } returns null
        }

        @Test
        fun `skal kreve veilederrolle ved henting av data for manuell journalføring`() {
            // Act
            val respons = oppgaveController.hentDataForManuellJournalføring(1L)

            // Assert
            verify(exactly = 1) {
                tilgangService.validerTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente data for manuell journalføring")
            }
            val personInfo = respons.body?.data?.person
            assertThat(personInfo?.navn).isEqualTo(person.navn)
        }

        @Test
        fun `skal ikke hente oppgave når kaller mangler veilederrolle`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente data for manuell journalføring")
            } throws RolleTilgangskontrollFeil("Krever VEILEDER")

            // Act & Assert
            assertThrows<RolleTilgangskontrollFeil> { oppgaveController.hentDataForManuellJournalføring(1L) }

            verify(exactly = 0) { oppgaveService.hentOppgave(any()) }
        }
    }
}
