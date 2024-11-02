package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.Sak
import no.nav.familie.ks.sak.integrasjon.lagJournalføringRequestDto
import no.nav.familie.ks.sak.integrasjon.lagJournalpost
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class JournalføringRequestDtoTest {
    @Nested
    inner class TilOppdaterJournalpostRequestDto {
        @Test
        fun `Skal beholde originalt avsender mottaker type dersom kanal er EESSI`() {
            // Arrange
            val sak =
                Sak(
                    arkivsaksnummer = "arkivsaksnummer",
                    arkivsaksystem = "arkivsaksystem",
                    fagsakId = "1",
                    sakstype = "sakstype",
                    fagsaksystem = "BA",
                )

            val journalpost =
                lagJournalpost(
                    personIdent = "testIdent",
                    journalpostId = "1",
                    avsenderMottakerIdType = AvsenderMottakerIdType.UTL_ORG,
                    kanal = "EESSI",
                )
            val journalføringRequestDto = lagJournalføringRequestDto(NavnOgIdentDto("testbruker", "testIdent"))

            // Act
            val oppdaterJournalpostRequestDto = journalføringRequestDto.tilOppdaterJournalpostRequestDto(sak, journalpost)

            // Assert
            assertThat(oppdaterJournalpostRequestDto.avsenderMottaker?.idType).isEqualTo(AvsenderMottakerIdType.UTL_ORG)
        }

        @Test
        fun `Skal sette AvsenderMottakerIdType i AvsenderMottaker til FNR dersom ident er fylt ut`() {
            // Arrange
            val sak =
                Sak(
                    arkivsaksnummer = "arkivsaksnummer",
                    arkivsaksystem = "arkivsaksystem",
                    fagsakId = "1",
                    sakstype = "sakstype",
                    fagsaksystem = "BA",
                )

            val journalpost =
                lagJournalpost(
                    personIdent = "testIdent",
                    journalpostId = "1",
                    avsenderMottakerIdType = AvsenderMottakerIdType.UTL_ORG,
                    kanal = "NAV_NO",
                )

            val journalføringRequestDto = lagJournalføringRequestDto(NavnOgIdentDto("testbruker", "testIdent"))

            // Act
            val oppdaterJournalpostRequestDto = journalføringRequestDto.tilOppdaterJournalpostRequestDto(sak, journalpost)

            // Assert
            assertThat(oppdaterJournalpostRequestDto.avsenderMottaker?.idType).isEqualTo(AvsenderMottakerIdType.FNR)
        }

        @Test
        fun `Skal sette AvsenderMottakerIdType i AvsenderMottaker til null dersom ident er blank`() {
            // Arrange
            val sak =
                Sak(
                    arkivsaksnummer = "arkivsaksnummer",
                    arkivsaksystem = "arkivsaksystem",
                    fagsakId = "1",
                    sakstype = "sakstype",
                    fagsaksystem = "BA",
                )

            val journalpost =
                lagJournalpost(
                    personIdent = "",
                    journalpostId = "1",
                    avsenderMottakerIdType = AvsenderMottakerIdType.UTL_ORG,
                    kanal = "NAV_NO",
                )

            val journalføringRequestDto = lagJournalføringRequestDto(NavnOgIdentDto("", ""))

            // Act
            val oppdaterJournalpostRequestDto = journalføringRequestDto.tilOppdaterJournalpostRequestDto(sak, journalpost)

            // Assert
            assertThat(oppdaterJournalpostRequestDto.avsenderMottaker?.idType).isNull()
        }
    }
}
