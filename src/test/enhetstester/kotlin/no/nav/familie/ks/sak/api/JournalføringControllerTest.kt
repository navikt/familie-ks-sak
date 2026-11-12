package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.journalføring.InnkommendeJournalføringService
import no.nav.familie.ks.sak.integrasjon.lagTilgangsstyrtJournalpost
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

class JournalføringControllerTest {
    private val innkommendeJournalføringService: InnkommendeJournalføringService = mockk()
    private val tilgangService: TilgangService = mockk()
    private val journalføringController: JournalføringController =
        JournalføringController(
            innkommendeJournalføringService = innkommendeJournalføringService,
            tilgangService = tilgangService,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandling(any(), any()) }
    }

    @Nested
    inner class HentJournalposterForBruker {
        @Test
        fun `skal returnere liste av tilgangsstyrte journalposter`() {
            // Arrange
            val personIdent = PersonIdent(randomFnr())
            val journalpostId = "1"

            every { innkommendeJournalføringService.hentJournalposterForBruker(personIdent.ident) } returns listOf(lagTilgangsstyrtJournalpost(personIdent.ident, journalpostId = journalpostId, harTilgang = true))

            // Act
            val responseEntity = journalføringController.hentJournalposterForBruker(personIdentBody = personIdent)

            // Assert
            assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(responseEntity.body).isNotNull
            assertThat(responseEntity.body!!.data).isNotNull
            val journalposter = responseEntity.body!!.data!!
            assertThat(journalposter).hasSize(1)
            val tilgangsstyrtJournalpost = journalposter.single()
            assertThat(tilgangsstyrtJournalpost.journalpost.journalpostId).isEqualTo("1")
            assertThat(tilgangsstyrtJournalpost.journalpostTilgang.harTilgang).isTrue
        }
    }

    @Nested
    inner class Tilgangskontroll {
        private val fødselsnummer = randomFnr()
        private val journalpostId = "1"
        private val dokumentId = "2"

        @BeforeEach
        fun setUp() {
            every { innkommendeJournalføringService.hentDokumentIJournalpost(journalpostId, dokumentId) } returns ByteArray(0)
            every { innkommendeJournalføringService.hentJournalposterForBruker(fødselsnummer) } returns emptyList()
        }

        @Test
        fun `skal kreve veilederrolle ved henting av journalposter for bruker`() {
            // Act
            journalføringController.hentJournalposterForBruker(PersonIdent(fødselsnummer))

            // Assert
            verify(exactly = 1) {
                tilgangService.validerTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente journalposter for bruker")
            }
        }

        @Test
        fun `skal ikke hente journalposter for bruker når kaller mangler veilederrolle`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente journalposter for bruker")
            } throws RolleTilgangskontrollFeil("Krever VEILEDER")

            // Act & Assert
            assertThrows<RolleTilgangskontrollFeil> { journalføringController.hentJournalposterForBruker(PersonIdent(fødselsnummer)) }

            verify(exactly = 0) { innkommendeJournalføringService.hentJournalposterForBruker(any()) }
        }

        @Test
        fun `skal kreve veilederrolle for å hente dokument`() {
            // Act
            journalføringController.hentDokumentIJournalpost(journalpostId, dokumentId)

            // Assert
            verify(exactly = 1) { tilgangService.validerTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente dokument") }
        }

        @Test
        fun `skal kreve veilederrolle for å hente dokument som PDF`() {
            // Act
            journalføringController.hentDokumentIJournalpostSomPdf(journalpostId, dokumentId)

            // Assert
            verify(exactly = 1) { tilgangService.validerTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente dokument") }
        }

        @Test
        fun `skal ikke hente dokument når kaller mangler veilederrolle`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente dokument")
            } throws RolleTilgangskontrollFeil("Krever VEILEDER")

            // Act & Assert
            assertThrows<RolleTilgangskontrollFeil> { journalføringController.hentDokumentIJournalpost(journalpostId, dokumentId) }
            assertThrows<RolleTilgangskontrollFeil> { journalføringController.hentDokumentIJournalpostSomPdf(journalpostId, dokumentId) }

            verify(exactly = 0) { innkommendeJournalføringService.hentDokumentIJournalpost(any(), any()) }
        }
    }
}
