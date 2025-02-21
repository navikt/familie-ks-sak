package no.nav.familie.ks.sak.integrasjon.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.hamcrest.CoreMatchers.`is` as Is

internal class UtgåendeJournalføringServiceTest {
    private val integrasjonClient = mockk<IntegrasjonClient>()

    private val utgåendeJournalføringService = UtgåendeJournalføringService(integrasjonClient)

    @BeforeEach
    fun setUp() {
        MDC.clear()
    }

    @Test
    fun `journalførDokument skal kaste feil hvis ferdigstilling av journalpost ikke var vellykket`() {
        val mocketResponse = ArkiverDokumentResponse("testId", false, emptyList())
        val fnr = randomFnr()
        val fagsakId = 1L

        every { integrasjonClient.journalførDokument(any()) } returns mocketResponse

        val feil =
            assertThrows<Feil> {
                utgåendeJournalføringService.journalførDokument(
                    fagsakId = fagsakId,
                    fnr = fnr,
                    brev = emptyList(),
                )
            }

        assertThat(feil.message, Is("Klarte ikke ferdigstille journalpost med id testId"))

        verify(exactly = 1) { integrasjonClient.journalførDokument(any()) }
    }

    @Test
    fun `journalførDokument skal returnere journalpostId hvis ferdigstilling er vellykket`() {
        val mocketResponse = ArkiverDokumentResponse("testId", true, emptyList())
        val fnr = randomFnr()
        val fagsakId = 1L

        every { integrasjonClient.journalførDokument(any()) } returns mocketResponse

        val journalpostId =
            utgåendeJournalføringService.journalførDokument(
                fagsakId = fagsakId,
                fnr = fnr,
                brev = emptyList(),
            )

        assertThat(journalpostId, Is("testId"))

        verify(exactly = 1) { integrasjonClient.journalførDokument(any()) }
    }

    @Test
    fun `journalførDokument skal returnere eksisterende journalpost hvis det allerede finnes journalpost med lik eksternId`() {
        val mocketEksisterendeJournalpost = mockk<Journalpost>(relaxed = true)
        val fnr = randomFnr()
        val fagsakId = 1L

        every { integrasjonClient.journalførDokument(any()) } throws
            RessursException(
                mockk(),
                mockk(),
                HttpStatus.CONFLICT,
            )
        every { integrasjonClient.hentJournalposterForBruker(any()) } returns listOf(mocketEksisterendeJournalpost)
        every { mocketEksisterendeJournalpost.eksternReferanseId } returns "1_null_null"

        val journalpostId =
            utgåendeJournalføringService.journalførDokument(
                fagsakId = fagsakId,
                fnr = fnr,
                brev = emptyList(),
            )

        assertThat(journalpostId, Is(mocketEksisterendeJournalpost.journalpostId))

        verify(exactly = 1) { integrasjonClient.journalførDokument(any()) }
        verify(exactly = 1) { integrasjonClient.hentJournalposterForBruker(any()) }
        verify(exactly = 1) { mocketEksisterendeJournalpost.eksternReferanseId }
    }
}
