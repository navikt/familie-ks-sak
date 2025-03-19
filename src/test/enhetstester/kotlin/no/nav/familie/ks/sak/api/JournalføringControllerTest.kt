package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.ks.sak.cucumber.mocking.mockUnleashNextMedContextService
import no.nav.familie.ks.sak.integrasjon.journalføring.InnkommendeJournalføringService
import no.nav.familie.ks.sak.integrasjon.lagTilgangsstyrtJournalpost
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class JournalføringControllerTest {
    private val innkommendeJournalføringService: InnkommendeJournalføringService = mockk()
    private val tilgangService: TilgangService = mockk()
    private val journalføringController: JournalføringController =
        JournalføringController(
            innkommendeJournalføringService = innkommendeJournalføringService,
            tilgangService = tilgangService,
            unleashNextMedContextService = mockUnleashNextMedContextService()
        )

    @Nested
    inner class HentJournalposterForBruker {
        @Test
        fun `skal returnere liste av tilgangsstyrte journalposter`() {
            // Arrange
            val personIdent = PersonIdent("123")
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
            assertThat(tilgangsstyrtJournalpost.harTilgang).isTrue
        }
    }
}
