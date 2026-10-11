package no.nav.familie.ks.sak.integrasjon.familieintegrasjon

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.hamcrest.CoreMatchers.`is` as Is

internal class IntegrasjonServiceTest {
    private val integrasjonKlient = mockk<IntegrasjonKlient>()
    private val integrasjonService = IntegrasjonService(integrasjonKlient)

    @Test
    fun `hentJournalpost skal returnere journalpost fra familie-integrasjoner`() {
        // Arrange
        val mocketJournalPost = mockk<Journalpost>()

        every { integrasjonKlient.hentJournalpost("test") } returns mocketJournalPost

        // Act
        val hentetJournalPost = integrasjonService.hentJournalpost("test")

        // Assert
        verify(exactly = 1) { integrasjonKlient.hentJournalpost("test") }

        assertThat(hentetJournalPost, Is(mocketJournalPost))
    }
}
