package no.nav.familie.ks.sak.barnehagelister

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnDtoInterface
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import java.time.LocalDate

class BarnehagebarnServiceTest {
    private val mockBarnehagebarnRepository = mockk<BarnehagebarnRepository>()

    private val barnehagebarnService =
        BarnehagebarnService(
            mockBarnehagebarnRepository,
        )

    @Nested
    inner class HentBarnehageBarnTest {
        @Test
        fun `skal hente barn i uavhengig av løpende andel fra ident dersom det sendes inn som parameter`() {
            // Arrange
            val mocketPageBarnehagebarnDtoInterface = mockk<Page<BarnehagebarnDtoInterface>>()
            val barnehagebarnRequestParamsMedIdent =
                BarnehagebarnRequestParams(
                    ident = "eksisterendeIdent",
                    kunLøpendeAndel = false,
                    kommuneNavn = null,
                )
            every {
                mockBarnehagebarnRepository.findBarnehagebarnByIdentUavhengigAvLøpendeAndel("eksisterendeIdent", any())
            } returns mocketPageBarnehagebarnDtoInterface

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParamsMedIdent)

            // Assert
            assertThat(hentetMocketPageBarnehagebarnDtoInterface).isEqualTo(mocketPageBarnehagebarnDtoInterface)
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnByIdentUavhengigAvLøpendeAndel("eksisterendeIdent", any()) }
        }

        @Test
        fun `skal hente barn i uavhengig av løpende andel fra kommunenavn dersom det sendes inn som parameter`() {
            // Arrange
            val mocketPageBarnehagebarnDtoInterface = mockk<Page<BarnehagebarnDtoInterface>>()
            val barnehagebarnRequestParamsMedKommuneNavn =
                BarnehagebarnRequestParams(
                    ident = null,
                    kunLøpendeAndel = false,
                    kommuneNavn = "eksisterendeKommune",
                )
            every {
                mockBarnehagebarnRepository.findBarnehagebarnByKommuneNavnUavhengigAvLøpendeAndel("eksisterendeKommune", any())
            } returns mocketPageBarnehagebarnDtoInterface

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParamsMedKommuneNavn)

            // Assert
            assertThat(hentetMocketPageBarnehagebarnDtoInterface).isEqualTo(mocketPageBarnehagebarnDtoInterface)
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnByKommuneNavnUavhengigAvLøpendeAndel("eksisterendeKommune", any()) }
        }

        @Test
        fun `skal hente alle barn uavhengig av løpende andel dersom det sendes inn som parameter`() {
            // Arrange
            val mocketPageBarnehagebarnDtoInterface = mockk<Page<BarnehagebarnDtoInterface>>()
            val barnehagebarnRequestParams =
                BarnehagebarnRequestParams(
                    ident = null,
                    kunLøpendeAndel = false,
                    kommuneNavn = null,
                )
            every {
                mockBarnehagebarnRepository.findAlleBarnehagebarnUavhengigAvLøpendeAndel(any())
            } returns mocketPageBarnehagebarnDtoInterface

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams)

            // Assert
            assertThat(hentetMocketPageBarnehagebarnDtoInterface).isEqualTo(mocketPageBarnehagebarnDtoInterface)
            verify(exactly = 1) { mockBarnehagebarnRepository.findAlleBarnehagebarnUavhengigAvLøpendeAndel(any()) }
        }
    }

    @Test
    fun `skal hente alle barn med løpende andel dersom det sendes inn som parameter`() {
        // Arrange
        val dagensDato = LocalDate.now()
        val mocketPageBarnehagebarnDtoInterface = mockk<Page<BarnehagebarnDtoInterface>>()
        val barnehagebarnRequestParams =
            BarnehagebarnRequestParams(
                ident = null,
                kunLøpendeAndel = true,
                kommuneNavn = null,
            )
        every {
            mockBarnehagebarnRepository.findBarnehagebarn(dagensDato, any())
        } returns mocketPageBarnehagebarnDtoInterface

        // Act
        val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams)

        // Assert
        assertThat(hentetMocketPageBarnehagebarnDtoInterface).isEqualTo(mocketPageBarnehagebarnDtoInterface)
        verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarn(dagensDato, any()) }
    }

    @Test
    fun `skal hente barn med løpende andel fra kommunenavn dersom det sendes inn som parameter`() {
        // Arrange
        val dagensDato = LocalDate.now()
        val mocketPageBarnehagebarnDtoInterface = mockk<Page<BarnehagebarnDtoInterface>>()
        val barnehagebarnRequestParams =
            BarnehagebarnRequestParams(
                ident = null,
                kunLøpendeAndel = true,
                kommuneNavn = "kommune",
            )
        every {
            mockBarnehagebarnRepository.findBarnehagebarnByKommuneNavn("kommune", dagensDato, any())
        } returns mocketPageBarnehagebarnDtoInterface

        // Act
        val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams)

        // Assert
        assertThat(hentetMocketPageBarnehagebarnDtoInterface).isEqualTo(mocketPageBarnehagebarnDtoInterface)
        verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnByKommuneNavn("kommune", dagensDato, any()) }
    }

    @Test
    fun `skal hente barn med løpende andel fra ident dersom det sendes inn som parameter`() {
        // Arrange
        val dagensDato = LocalDate.now()
        val mocketPageBarnehagebarnDtoInterface = mockk<Page<BarnehagebarnDtoInterface>>()
        val barnehagebarnRequestParams =
            BarnehagebarnRequestParams(
                ident = "ident",
                kunLøpendeAndel = true,
                kommuneNavn = null,
            )
        every {
            mockBarnehagebarnRepository.findBarnehagebarnByIdent("ident", dagensDato, any())
        } returns mocketPageBarnehagebarnDtoInterface

        // Act
        val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams)

        // Assert
        assertThat(hentetMocketPageBarnehagebarnDtoInterface).isEqualTo(mocketPageBarnehagebarnDtoInterface)
        verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnByIdent("ident", dagensDato, any()) }
    }

    @Nested
    inner class HentAlleKommuner {
        @Test
        fun `skal hente alle kommuner vi har mottat barnehageliste for`() {
            // Arrange

            val kommuner = setOf("Oslo kommune", "Voss kommune", "Frogn kommune")

            every { mockBarnehagebarnRepository.hentAlleKommuner() } returns kommuner

            // Act
            val alleKommuner = barnehagebarnService.hentAlleKommuner()

            // Assert
            assertThat(alleKommuner).hasSize(3)
            assertThat(alleKommuner).usingRecursiveComparison().isEqualTo(kommuner)
        }
    }
}
