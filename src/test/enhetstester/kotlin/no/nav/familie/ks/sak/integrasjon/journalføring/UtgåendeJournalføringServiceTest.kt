package no.nav.familie.ks.sak.integrasjon.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService.Companion.genererEksternReferanseIdForJournalpost
import no.nav.familie.ks.sak.integrasjon.lagJournalpost
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import java.util.UUID

class UtgåendeJournalføringServiceTest {
    private val integrasjonClient = mockk<IntegrasjonClient>()
    private val utgåendeJournalføringService = UtgåendeJournalføringService(integrasjonClient)

    @Nested
    inner class JournalførDokument {
        @Test
        fun `skal kaste feil hvis ferdigstilling av journalpost ikke var vellykket`() {
            // Arrange
            val mocketResponse = ArkiverDokumentResponse("testId", false, emptyList())
            every { integrasjonClient.journalførDokument(any()) } returns mocketResponse

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    utgåendeJournalføringService.journalførDokument(
                        fagsakId = 1L,
                        fnr = randomFnr(),
                        brev = emptyList(),
                        eksternReferanseId = "1",
                    )
                }
            assertThat(exception.message).isEqualTo("Klarte ikke ferdigstille journalpost med id testId")
            verify(exactly = 1) { integrasjonClient.journalførDokument(any()) }
        }

        @Test
        fun `skal returnere journalpostId hvis ferdigstilling er vellykket`() {
            // Arrange
            val mocketResponse = ArkiverDokumentResponse("testId", true, emptyList())
            every { integrasjonClient.journalførDokument(any()) } returns mocketResponse

            // Act
            val journalpostId =
                utgåendeJournalføringService.journalførDokument(
                    fagsakId = 1L,
                    fnr = randomFnr(),
                    brev = emptyList(),
                    eksternReferanseId = "1",
                )

            // Assert
            assertThat(journalpostId).isEqualTo("testId")
            verify(exactly = 1) { integrasjonClient.journalførDokument(any()) }
        }

        @Test
        fun `skal returnere eksisterende journalpost hvis det allerede finnes journalpost med lik eksternId`() {
            // Arrange
            val fnr = randomFnr()
            val journalpostId = "1"
            val eksternReferanseId = UUID.randomUUID().toString()
            val journalpost = lagJournalpost(personIdent = fnr, journalpostId = journalpostId, eksternReferanseId = eksternReferanseId)

            every { integrasjonClient.journalførDokument(any()) } throws RessursException(mockk(), mockk(), HttpStatus.CONFLICT)
            every { integrasjonClient.hentJournalposterForBruker(any()) } returns listOf(journalpost)

            // Act
            val returnertJournalpostId =
                utgåendeJournalføringService.journalførDokument(
                    fagsakId = 1L,
                    fnr = fnr,
                    brev = emptyList(),
                    eksternReferanseId = eksternReferanseId,
                )

            // Assert
            assertThat(returnertJournalpostId).isEqualTo(journalpostId)
            verify(exactly = 1) { integrasjonClient.journalførDokument(any()) }
            verify(exactly = 1) { integrasjonClient.hentJournalposterForBruker(any()) }
        }
    }

    @Nested
    inner class GenererEksternReferanseIdForJournalpost {
        @BeforeEach
        fun setUp() {
            MDC.clear()
        }

        @Test
        fun `skal lage ekstern referanse id med behandling id`() {
            // Act
            val eksternReferanseId = genererEksternReferanseIdForJournalpost(fagsakId = 1L, behandlingId = 1L, tilVergeEllerFullmektig = true)

            // Assert
            assertThat(eksternReferanseId).isEqualTo("1_1_tilleggsmottaker_null")
        }

        @Test
        fun `skal lage ekstern referanse id uten behandling id`() {
            // Act
            val eksternReferanseId = genererEksternReferanseIdForJournalpost(fagsakId = 1L, behandlingId = null, tilVergeEllerFullmektig = true)

            // Assert
            assertThat(eksternReferanseId).isEqualTo("1_null_tilleggsmottaker_null")
        }

        @Test
        fun `skal lage ekstern referanse id med behandling id men uten verge eller fullmektig`() {
            // Act
            val eksternReferanseId = genererEksternReferanseIdForJournalpost(fagsakId = 1L, behandlingId = 1L, tilVergeEllerFullmektig = false)

            // Assert
            assertThat(eksternReferanseId).isEqualTo("1_1_null")
        }

        @Test
        fun `skal lage ekstern referanse id uten behandling id og uten verge eller fullmektig`() {
            // Act
            val eksternReferanseId = genererEksternReferanseIdForJournalpost(fagsakId = 1L, behandlingId = null, tilVergeEllerFullmektig = false)

            // Assert
            assertThat(eksternReferanseId).isEqualTo("1_null_null")
        }
    }
}
