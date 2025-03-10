package no.nav.familie.ks.sak.barnehagelister

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.BarnehagebarnService.Companion.LØPENDE_FAGSAK_STATUS
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnDtoInterface
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnInfotrygdDtoInterface
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.time.LocalDate
import java.time.LocalDateTime

class BarnehagebarnServiceTest {
    private val mockBarnehagebarnRepository = mockk<BarnehagebarnRepository>()
    private val mockInfotrygdReplikaClient = mockk<InfotrygdReplikaClient>()

    private val barnehagebarnService =
        BarnehagebarnService(
            mockInfotrygdReplikaClient,
            mockBarnehagebarnRepository,
        )

    @Nested
    inner class HentBarnehageBarnTest {
        fun `skal hente barn i løpende fagsak fra ident dersom det sendes inn som parameter`() {
            // Arrange
            val mocketPageBarnehagebarnDtoInterface = mockk<Page<BarnehagebarnDtoInterface>>()
            val barnehagebarnRequestParamsMedIdent =
                BarnehagebarnRequestParams(
                    ident = "eksisterendeIdent",
                    kunLøpendeFagsak = true,
                    kommuneNavn = null,
                )
            every {
                mockBarnehagebarnRepository.findBarnehagebarnByIdent(LØPENDE_FAGSAK_STATUS, "eksisterendeIdent", any())
            } returns mocketPageBarnehagebarnDtoInterface

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParamsMedIdent)

            // Assert
            assertThat(hentetMocketPageBarnehagebarnDtoInterface).isEqualTo(mocketPageBarnehagebarnDtoInterface)
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnByIdent(LØPENDE_FAGSAK_STATUS, "eksisterendeIdent", any()) }
        }

        @Test
        fun `skal hente barn i uavhengig av fagsak fra ident dersom det sendes inn som parameter`() {
            // Arrange
            val mocketPageBarnehagebarnDtoInterface = mockk<Page<BarnehagebarnDtoInterface>>()
            val barnehagebarnRequestParamsMedIdent =
                BarnehagebarnRequestParams(
                    ident = "eksisterendeIdent",
                    kunLøpendeFagsak = false,
                    kommuneNavn = null,
                )
            every {
                mockBarnehagebarnRepository.findBarnehagebarnByIdentUavhengigAvFagsak("eksisterendeIdent", any())
            } returns mocketPageBarnehagebarnDtoInterface

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParamsMedIdent)

            // Assert
            assertThat(hentetMocketPageBarnehagebarnDtoInterface).isEqualTo(mocketPageBarnehagebarnDtoInterface)
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnByIdentUavhengigAvFagsak("eksisterendeIdent", any()) }
        }

        @Test
        fun `skal hente barn i løpende fagsak fra kommunenavn dersom det sendes inn som parameter`() {
            // Arrange
            val mocketPageBarnehagebarnDtoInterface = mockk<Page<BarnehagebarnDtoInterface>>()
            val barnehagebarnRequestParamsMedKommuneNavn =
                BarnehagebarnRequestParams(
                    ident = null,
                    kunLøpendeFagsak = true,
                    kommuneNavn = "eksisterendeKommune",
                )
            every {
                mockBarnehagebarnRepository.findBarnehagebarnByKommuneNavn(LØPENDE_FAGSAK_STATUS, "eksisterendeKommune", any())
            } returns mocketPageBarnehagebarnDtoInterface

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParamsMedKommuneNavn)

            // Assert
            assertThat(hentetMocketPageBarnehagebarnDtoInterface).isEqualTo(mocketPageBarnehagebarnDtoInterface)
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnByKommuneNavn(LØPENDE_FAGSAK_STATUS, "eksisterendeKommune", any()) }
        }

        @Test
        fun `skal hente barn i uavhengig av fagsak fra kommunenavn dersom det sendes inn som parameter`() {
            // Arrange
            val mocketPageBarnehagebarnDtoInterface = mockk<Page<BarnehagebarnDtoInterface>>()
            val barnehagebarnRequestParamsMedKommuneNavn =
                BarnehagebarnRequestParams(
                    ident = null,
                    kunLøpendeFagsak = false,
                    kommuneNavn = "eksisterendeKommune",
                )
            every {
                mockBarnehagebarnRepository.findBarnehagebarnByKommuneNavnUavhengigAvFagsak("eksisterendeKommune", any())
            } returns mocketPageBarnehagebarnDtoInterface

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParamsMedKommuneNavn)

            // Assert
            assertThat(hentetMocketPageBarnehagebarnDtoInterface).isEqualTo(mocketPageBarnehagebarnDtoInterface)
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnByKommuneNavnUavhengigAvFagsak("eksisterendeKommune", any()) }
        }

        @Test
        fun `skal hente alle barn i løpende fagsaker dersom det sendes inn som parameter`() {
            // Arrange
            val mocketPageBarnehagebarnDtoInterface = mockk<Page<BarnehagebarnDtoInterface>>()
            val barnehagebarnRequestParams =
                BarnehagebarnRequestParams(
                    ident = null,
                    kunLøpendeFagsak = true,
                    kommuneNavn = null,
                )
            every {
                mockBarnehagebarnRepository.findBarnehagebarn(LØPENDE_FAGSAK_STATUS, any())
            } returns mocketPageBarnehagebarnDtoInterface

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams)

            // Assert
            assertThat(hentetMocketPageBarnehagebarnDtoInterface).isEqualTo(mocketPageBarnehagebarnDtoInterface)
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarn(LØPENDE_FAGSAK_STATUS, any()) }
        }

        @Test
        fun `skal hente alle barn uavhengig av fagsak dersom det sendes inn som parameter`() {
            // Arrange
            val mocketPageBarnehagebarnDtoInterface = mockk<Page<BarnehagebarnDtoInterface>>()
            val barnehagebarnRequestParams =
                BarnehagebarnRequestParams(
                    ident = null,
                    kunLøpendeFagsak = false,
                    kommuneNavn = null,
                )
            every {
                mockBarnehagebarnRepository.findAlleBarnehagebarnUavhengigAvFagsak(any())
            } returns mocketPageBarnehagebarnDtoInterface

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams)

            // Assert
            assertThat(hentetMocketPageBarnehagebarnDtoInterface).isEqualTo(mocketPageBarnehagebarnDtoInterface)
            verify(exactly = 1) { mockBarnehagebarnRepository.findAlleBarnehagebarnUavhengigAvFagsak(any()) }
        }

        @Test
        fun `skal hente barn fra infotrygd i løpende fagsak fra ident dersom det sendes inn som parameter`() {
            // Arrange
            val mocketBarnehagebarnInfotrygdDtoInterface =
                opprettMocketBarnehagebarnInfotrygdDtoInterface(
                    "ident1",
                    "endringsType",
                    "Oslo",
                    "0000",
                )
            val barnehagebarnRequestParamsMedIdent =
                BarnehagebarnRequestParams(
                    ident = "eksisterendeIdent",
                    kunLøpendeFagsak = true,
                    kommuneNavn = null,
                )

            val infotrygdIdentListe = listOf("infotrygdIdent1", "infotrygdIdent2")
            every { mockInfotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker() } returns infotrygdIdentListe

            every {
                mockBarnehagebarnRepository.findBarnehagebarnByIdentInfotrygd("eksisterendeIdent", infotrygdIdentListe, any())
            } returns PageImpl(listOf(mocketBarnehagebarnInfotrygdDtoInterface))

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehagebarnInfotrygd(barnehagebarnRequestParamsMedIdent)

            // Assert
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnByIdentInfotrygd("eksisterendeIdent", infotrygdIdentListe, any()) }
            verify(exactly = 1) { mockInfotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker() }

            assertThat(hentetMocketPageBarnehagebarnDtoInterface.content).hasSize(1)
            val barnehagebarnInfotrygdDto = hentetMocketPageBarnehagebarnDtoInterface.content.single()

            assertThat(barnehagebarnInfotrygdDto.ident).isEqualTo("ident1")
            assertThat(barnehagebarnInfotrygdDto.antallTimerIBarnehage).isEqualTo(40.0)
            assertThat(barnehagebarnInfotrygdDto.harFagsak).isTrue()
        }
    }

    @Nested
    inner class HentBarnehagebarnLøpendeAndelTest {
        @Test
        fun `skal hente alle barn med løpende andel dersom det sendes inn som parameter`() {
            // Arrange
            val dagensDato = LocalDate.now()
            val mocketPageBarnehagebarnDtoInterface = mockk<Page<BarnehagebarnDtoInterface>>()
            val barnehagebarnRequestParams =
                BarnehagebarnRequestParams(
                    ident = null,
                    kunLøpendeFagsak = false,
                    kommuneNavn = null,
                    kunLøpendeAndel = true,
                )
            every {
                mockBarnehagebarnRepository.findBarnehagebarnLøpendeAndel(dagensDato, any())
            } returns mocketPageBarnehagebarnDtoInterface

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams)

            // Assert
            assertThat(hentetMocketPageBarnehagebarnDtoInterface).isEqualTo(mocketPageBarnehagebarnDtoInterface)
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnLøpendeAndel(dagensDato, any()) }
        }

        @Test
        fun `skal hente barn med løpende andel fra kommunenavn dersom det sendes inn som parameter`() {
            // Arrange
            val dagensDato = LocalDate.now()
            val mocketPageBarnehagebarnDtoInterface = mockk<Page<BarnehagebarnDtoInterface>>()
            val barnehagebarnRequestParams =
                BarnehagebarnRequestParams(
                    ident = null,
                    kunLøpendeFagsak = false,
                    kommuneNavn = "kommune",
                    kunLøpendeAndel = true,
                )
            every {
                mockBarnehagebarnRepository.findBarnehagebarnByKommuneNavnOgLøpendeAndel("kommune", dagensDato, any())
            } returns mocketPageBarnehagebarnDtoInterface

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams)

            // Assert
            assertThat(hentetMocketPageBarnehagebarnDtoInterface).isEqualTo(mocketPageBarnehagebarnDtoInterface)
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnByKommuneNavnOgLøpendeAndel("kommune", dagensDato, any()) }
        }

        @Test
        fun `skal hente barn med løpende andel fra ident dersom det sendes inn som parameter`() {
            // Arrange
            val dagensDato = LocalDate.now()
            val mocketPageBarnehagebarnDtoInterface = mockk<Page<BarnehagebarnDtoInterface>>()
            val barnehagebarnRequestParams =
                BarnehagebarnRequestParams(
                    ident = "ident",
                    kunLøpendeFagsak = false,
                    kommuneNavn = null,
                    kunLøpendeAndel = true,
                )
            every {
                mockBarnehagebarnRepository.findBarnehagebarnByIdentOgLøpendeAndel("ident", dagensDato, any())
            } returns mocketPageBarnehagebarnDtoInterface

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams)

            // Assert
            assertThat(hentetMocketPageBarnehagebarnDtoInterface).isEqualTo(mocketPageBarnehagebarnDtoInterface)
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnByIdentOgLøpendeAndel("ident", dagensDato, any()) }
        }
    }

    @Nested
    inner class HentBarnehagebarnInfotrygdTest {
        @Test
        fun `skal hente barn fra infotrygd uavhengig av fagsak fra kommunenavn dersom det sendes inn som parameter`() {
            // Arrange
            val mocketBarnehagebarnInfotrygdDtoInterface =
                opprettMocketBarnehagebarnInfotrygdDtoInterface(
                    "ident1",
                    "endringsType",
                    "Oslo",
                    "0000",
                )
            val barnehagebarnRequestParamsMedIdent =
                BarnehagebarnRequestParams(
                    ident = null,
                    kunLøpendeFagsak = false,
                    kommuneNavn = "eksisterendeKommune",
                )

            val infotrygdIdentListe = listOf("infotrygdIdent1", "infotrygdIdent2")
            every { mockInfotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker() } returns infotrygdIdentListe

            every {
                mockBarnehagebarnRepository.findBarnehagebarnByKommuneNavnInfotrygdUavhengigAvFagsak("eksisterendeKommune", any())
            } returns PageImpl(listOf(mocketBarnehagebarnInfotrygdDtoInterface))

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehagebarnInfotrygd(barnehagebarnRequestParamsMedIdent)

            // Assert
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnByKommuneNavnInfotrygdUavhengigAvFagsak("eksisterendeKommune", any()) }
            verify(exactly = 1) { mockInfotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker() }

            assertThat(hentetMocketPageBarnehagebarnDtoInterface.content).hasSize(1)
            val barnehagebarnInfotrygdDto = hentetMocketPageBarnehagebarnDtoInterface.content.single()

            assertThat(barnehagebarnInfotrygdDto.ident).isEqualTo("ident1")
            assertThat(barnehagebarnInfotrygdDto.antallTimerIBarnehage).isEqualTo(40.0)
            assertThat(barnehagebarnInfotrygdDto.harFagsak).isFalse()
        }

        @Test
        fun `skal hente barn fra infotrygd i løpende fagsak fra kommunenavn dersom det sendes inn som parameter`() {
            // Arrange
            val mocketBarnehagebarnInfotrygdDtoInterface =
                opprettMocketBarnehagebarnInfotrygdDtoInterface(
                    "ident1",
                    "endringsType",
                    "Oslo",
                    "0000",
                )
            val barnehagebarnRequestParamsMedIdent =
                BarnehagebarnRequestParams(
                    ident = null,
                    kunLøpendeFagsak = true,
                    kommuneNavn = "eksisterendeKommune",
                )

            val infotrygdIdentListe = listOf("infotrygdIdent1", "infotrygdIdent2")
            every { mockInfotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker() } returns infotrygdIdentListe

            every {
                mockBarnehagebarnRepository.findBarnehagebarnByKommuneNavnInfotrygd("eksisterendeKommune", infotrygdIdentListe, any())
            } returns PageImpl(listOf(mocketBarnehagebarnInfotrygdDtoInterface))

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehagebarnInfotrygd(barnehagebarnRequestParamsMedIdent)

            // Assert
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnByKommuneNavnInfotrygd("eksisterendeKommune", infotrygdIdentListe, any()) }
            verify(exactly = 1) { mockInfotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker() }

            assertThat(hentetMocketPageBarnehagebarnDtoInterface.content).hasSize(1)
            val barnehagebarnInfotrygdDto = hentetMocketPageBarnehagebarnDtoInterface.content.single()

            assertThat(barnehagebarnInfotrygdDto.ident).isEqualTo("ident1")
            assertThat(barnehagebarnInfotrygdDto.antallTimerIBarnehage).isEqualTo(40.0)
            assertThat(barnehagebarnInfotrygdDto.harFagsak).isTrue()
        }

        @Test
        fun `skal hente barn fra infotrygd uavhengig av fagsak fra ident dersom det sendes inn som parameter`() {
            // Arrange
            val mocketBarnehagebarnInfotrygdDtoInterface =
                opprettMocketBarnehagebarnInfotrygdDtoInterface(
                    "ident1",
                    "endringsType",
                    "Oslo",
                    "0000",
                )
            val barnehagebarnRequestParamsMedIdent =
                BarnehagebarnRequestParams(
                    ident = "eksisterendeIdent",
                    kunLøpendeFagsak = false,
                    kommuneNavn = null,
                )

            val infotrygdIdentListe = listOf("infotrygdIdent1", "infotrygdIdent2")
            every { mockInfotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker() } returns infotrygdIdentListe

            every {
                mockBarnehagebarnRepository.findBarnehagebarnByIdentInfotrygdUavhengigAvFagsak("eksisterendeIdent", any())
            } returns PageImpl(listOf(mocketBarnehagebarnInfotrygdDtoInterface))

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehagebarnInfotrygd(barnehagebarnRequestParamsMedIdent)

            // Assert
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnByIdentInfotrygdUavhengigAvFagsak("eksisterendeIdent", any()) }
            verify(exactly = 1) { mockInfotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker() }

            assertThat(hentetMocketPageBarnehagebarnDtoInterface.content).hasSize(1)
            val barnehagebarnInfotrygdDto = hentetMocketPageBarnehagebarnDtoInterface.content.single()

            assertThat(barnehagebarnInfotrygdDto.ident).isEqualTo("ident1")
            assertThat(barnehagebarnInfotrygdDto.antallTimerIBarnehage).isEqualTo(40.0)
            assertThat(barnehagebarnInfotrygdDto.harFagsak).isFalse()
        }

        @Test
        fun `skal hente alle barn fra infotrygd i løpende fagsak dersom det sendes inn som parameter`() {
            // Arrange
            val mocketBarnehagebarnInfotrygdDtoInterface =
                opprettMocketBarnehagebarnInfotrygdDtoInterface(
                    "ident1",
                    "endringsType",
                    "Oslo",
                    "0000",
                )
            val barnehagebarnRequestParamsMedIdent =
                BarnehagebarnRequestParams(
                    ident = null,
                    kunLøpendeFagsak = true,
                    kommuneNavn = null,
                )

            val infotrygdIdentListe = listOf("infotrygdIdent1", "infotrygdIdent2")
            every { mockInfotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker() } returns infotrygdIdentListe

            every {
                mockBarnehagebarnRepository.findBarnehagebarnInfotrygd(infotrygdIdentListe, any())
            } returns PageImpl(listOf(mocketBarnehagebarnInfotrygdDtoInterface))

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehagebarnInfotrygd(barnehagebarnRequestParamsMedIdent)

            // Assert
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnInfotrygd(infotrygdIdentListe, any()) }
            verify(exactly = 1) { mockInfotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker() }

            assertThat(hentetMocketPageBarnehagebarnDtoInterface.content).hasSize(1)
            val barnehagebarnInfotrygdDto = hentetMocketPageBarnehagebarnDtoInterface.content.single()

            assertThat(barnehagebarnInfotrygdDto.ident).isEqualTo("ident1")
            assertThat(barnehagebarnInfotrygdDto.antallTimerIBarnehage).isEqualTo(40.0)
            assertThat(barnehagebarnInfotrygdDto.harFagsak).isTrue()
        }

        @Test
        fun `skal hente alle barn fra infotrygd uavhengig av fagsak dersom det sendes inn som parameter`() {
            // Arrange
            val mocketBarnehagebarnInfotrygdDtoInterface =
                opprettMocketBarnehagebarnInfotrygdDtoInterface(
                    "ident1",
                    "endringsType",
                    "Oslo",
                    "0000",
                )
            val barnehagebarnRequestParamsMedIdent =
                BarnehagebarnRequestParams(
                    ident = null,
                    kunLøpendeFagsak = false,
                    kommuneNavn = null,
                )

            val infotrygdIdentListe = listOf("infotrygdIdent1", "infotrygdIdent2")
            every { mockInfotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker() } returns infotrygdIdentListe

            every {
                mockBarnehagebarnRepository.findBarnehagebarnInfotrygdUavhengigAvFagsak(any())
            } returns PageImpl(listOf(mocketBarnehagebarnInfotrygdDtoInterface))

            // Act
            val hentetMocketPageBarnehagebarnDtoInterface = barnehagebarnService.hentBarnehagebarnInfotrygd(barnehagebarnRequestParamsMedIdent)

            // Assert
            verify(exactly = 1) { mockBarnehagebarnRepository.findBarnehagebarnInfotrygdUavhengigAvFagsak(any()) }
            verify(exactly = 1) { mockInfotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker() }

            assertThat(hentetMocketPageBarnehagebarnDtoInterface.content).hasSize(1)
            val barnehagebarnInfotrygdDto = hentetMocketPageBarnehagebarnDtoInterface.content.single()

            assertThat(barnehagebarnInfotrygdDto.ident).isEqualTo("ident1")
            assertThat(barnehagebarnInfotrygdDto.antallTimerIBarnehage).isEqualTo(40.0)
            assertThat(barnehagebarnInfotrygdDto.harFagsak).isFalse()
        }
    }

    private fun opprettMocketBarnehagebarnInfotrygdDtoInterface(
        ident: String,
        endringsType: String,
        kommuneNavn: String,
        kommuneNr: String,
    ) = mockk<BarnehagebarnInfotrygdDtoInterface>().apply {
        every { getIdent() } returns ident
        every { getFom() } returns LocalDate.now()
        every { getTom() } returns LocalDate.now()
        every { getAntallTimerIBarnehage() } returns 40.0
        every { getEndringstype() } returns endringsType
        every { getKommuneNavn() } returns kommuneNavn
        every { getKommuneNr() } returns kommuneNr
        every { getEndretTid() } returns LocalDateTime.now()
    }
}
