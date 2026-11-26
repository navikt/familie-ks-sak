package no.nav.familie.ks.sak.kjerne.brev.mottaker

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.BrevmottakerDto
import no.nav.familie.ks.sak.api.dto.Bruker
import no.nav.familie.ks.sak.api.dto.BrukerMedUtenlandskAdresse
import no.nav.familie.ks.sak.api.dto.Dødsbo
import no.nav.familie.ks.sak.api.dto.FullmektigEllerVerge
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBrevmottakerDto
import no.nav.familie.ks.sak.datagenerator.lagBrevmottakerDb
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BrevmottakerServiceTest {
    private val brevmottakerHenter = mockk<BrevmottakerHenter>()
    private val brevmottakerOppretter = mockk<BrevmottakerOppretter>()
    private val brevmottakerSletter = mockk<BrevmottakerSletter>()
    private val brevmottakerService =
        BrevmottakerService(
            brevmottakerHenter = brevmottakerHenter,
            brevmottakerOppretter = brevmottakerOppretter,
            brevmottakerSletter = brevmottakerSletter,
        )

    @Nested
    inner class OpprettBrevmottaker {
        @Test
        fun `skal opprette brevmottaker og returnere lagret brevmottaker`() {
            // Arrange
            val brevmottaker = lagBrevmottakerDb(behandlingId = 1)
            val lagretBrevmottaker = brevmottaker.copy(id = 1)
            every { brevmottakerOppretter.opprettBrevmottaker(brevmottaker) } returns lagretBrevmottaker

            // Act
            val resultat = brevmottakerService.opprettBrevmottaker(brevmottaker)

            // Assert
            assertThat(resultat).isEqualTo(lagretBrevmottaker)
            verify(exactly = 1) { brevmottakerOppretter.opprettBrevmottaker(brevmottaker) }
        }
    }

    @Nested
    inner class SlettBrevmottaker {
        @Test
        fun `skal slette brevmottaker på angitt behandling`() {
            // Arrange
            every { brevmottakerSletter.slettBrevmottaker(behandlingId = 2, id = 1) } just runs

            // Act
            brevmottakerService.slettBrevmottaker(behandlingId = 2, id = 1)

            // Assert
            verify(exactly = 1) { brevmottakerSletter.slettBrevmottaker(behandlingId = 2, id = 1) }
        }
    }

    @Nested
    inner class HentBrevmottakere {
        @Test
        fun `skal hente brevmottakere for behandling`() {
            // Arrange
            val brevmottakere = listOf(lagBrevmottakerDb(behandlingId = 1))
            every { brevmottakerHenter.hentBrevmottakere(1) } returns brevmottakere

            // Act
            val resultat = brevmottakerService.hentBrevmottakere(1)

            // Assert
            assertThat(resultat).isEqualTo(brevmottakere)
            verify(exactly = 1) { brevmottakerHenter.hentBrevmottakere(1) }
        }
    }

    @Nested
    inner class LagMottakereFraBrevMottakere {
        @Test
        fun `skal returnere bruker når det ikke finnes manuelle brevmottakere`() {
            // Arrange
            val brevmottakere = emptyList<BrevmottakerDto>()

            // Act
            val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere)

            // Assert
            assertThat(mottakerInfo).singleElement().isInstanceOf(Bruker::class.java)
            assertThat(mottakerInfo.single().manuellAdresseInfo).isNull()
        }

        @Test
        fun `skal lage mottakere når brevmottaker er FULLMEKTIG og bruker har norsk adresse`() {
            // Arrange
            val brevmottakere = listOf(lagBrevmottakerDto(type = MottakerType.FULLMEKTIG, navn = "Fullmektig navn"))

            // Act
            val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere)

            // Assert
            assertThat(mottakerInfo).hasSize(2)
            assertThat(mottakerInfo.first()).isInstanceOf(Bruker::class.java)
            assertThat(mottakerInfo.first().manuellAdresseInfo).isNull()
            assertThat(mottakerInfo.last()).isInstanceOf(FullmektigEllerVerge::class.java)
            assertThat(mottakerInfo.last().navn).isEqualTo("Fullmektig navn")
            assertThat(mottakerInfo.last().manuellAdresseInfo).isNotNull()
        }

        @Test
        fun `skal lage mottakere når brevmottaker er FULLMEKTIG og bruker har utenlandsk adresse`() {
            // Arrange
            val brevmottakere =
                listOf(
                    lagBrevmottakerDto(type = MottakerType.FULLMEKTIG, navn = "Fullmektig navn"),
                    lagBrevmottakerDto(
                        type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                        poststed = "Munchen",
                        landkode = "DE",
                    ),
                )

            // Act
            val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere)

            // Assert
            assertThat(mottakerInfo).hasSize(2)
            assertThat(mottakerInfo.first()).isInstanceOf(BrukerMedUtenlandskAdresse::class.java)
            assertThat(mottakerInfo.first().navn).isEmpty()
            assertThat(mottakerInfo.first().manuellAdresseInfo?.landkode).isEqualTo("DE")
            assertThat(mottakerInfo.last().navn).isEqualTo("Fullmektig navn")
            assertThat(mottakerInfo.last().manuellAdresseInfo).isNotNull()
        }

        @Test
        fun `skal lage mottakere når brevmottaker er VERGE og bruker har utenlandsk adresse`() {
            // Arrange
            val brevmottakere =
                listOf(
                    lagBrevmottakerDto(type = MottakerType.VERGE, navn = "Verge navn"),
                    lagBrevmottakerDto(
                        type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                        poststed = "Munchen",
                        landkode = "DE",
                    ),
                )

            // Act
            val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere)

            // Assert
            assertThat(mottakerInfo).hasSize(2)
            assertThat(mottakerInfo.first()).isInstanceOf(BrukerMedUtenlandskAdresse::class.java)
            assertThat(mottakerInfo.first().navn).isEmpty()
            assertThat(mottakerInfo.first().manuellAdresseInfo?.landkode).isEqualTo("DE")
            assertThat(mottakerInfo.last().navn).isEqualTo("Verge navn")
            assertThat(mottakerInfo.last().manuellAdresseInfo).isNotNull()
        }

        @Test
        fun `skal lage mottakere når bruker har utenlandsk adresse`() {
            // Arrange
            val brevmottakere =
                listOf(
                    lagBrevmottakerDto(
                        type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                        poststed = "Munchen",
                        landkode = "DE",
                    ),
                )

            // Act
            val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere)

            // Assert
            assertThat(mottakerInfo).hasSize(1)
            assertThat(mottakerInfo.single()).isInstanceOf(BrukerMedUtenlandskAdresse::class.java)
            assertThat(mottakerInfo.single().navn).isEmpty()
            assertThat(mottakerInfo.single().manuellAdresseInfo?.landkode).isEqualTo("DE")
        }

        @Test
        fun `skal lage mottakere når bruker har dødsbo`() {
            // Arrange
            val brevmottakere =
                listOf(
                    lagBrevmottakerDto(
                        type = MottakerType.DØDSBO,
                        navn = "Dødsbo navn",
                        poststed = "Munchen",
                        landkode = "DE",
                    ),
                )

            // Act
            val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere)

            // Assert
            assertThat(mottakerInfo).hasSize(1)
            assertThat(mottakerInfo.single()).isInstanceOf(Dødsbo::class.java)
            assertThat(mottakerInfo.single().navn).isEqualTo("Dødsbo navn")
            assertThat(mottakerInfo.single().manuellAdresseInfo?.landkode).isEqualTo("DE")
        }

        @Test
        fun `skal kaste feil når brevmottakere inneholder ugyldig kombinasjon`() {
            // Arrange
            val brevmottakere =
                listOf(
                    lagBrevmottakerDto(type = MottakerType.VERGE),
                    lagBrevmottakerDto(type = MottakerType.FULLMEKTIG),
                )

            // Act & Assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere)
                }
            assertThat(feil.frontendFeilmelding).contains("kan ikke kombineres")
        }

        @Test
        fun `skal kaste feil når bruker har flere utenlandske adresser`() {
            // Arrange
            val brevmottakere =
                listOf(
                    lagBrevmottakerDto(type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE, landkode = "DE"),
                    lagBrevmottakerDto(type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE, landkode = "SE"),
                )

            // Act & Assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere)
                }
            assertThat(feil.frontendFeilmelding).isEqualTo("Mottakerfeil: Det er registrert mer enn en utenlandsk adresse tilhørende bruker")
        }
    }
}
