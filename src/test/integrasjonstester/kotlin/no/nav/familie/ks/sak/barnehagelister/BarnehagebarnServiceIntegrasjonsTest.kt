package no.nav.familie.ks.sak.no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.BarnehagebarnService
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class BarnehagebarnServiceIntegrasjonsTest(
    @Autowired private val barnehagebarnService: BarnehagebarnService,
) : OppslagSpringRunnerTest() {
    @Nested
    inner class BarnehageBarnMottattTidligereTest {
        val barnehagebarn =
            Barnehagebarn(
                ident = "12345678901",
                fom = LocalDate.now(),
                tom = LocalDate.now().plusMonths(3),
                antallTimerIBarnehage = 35.0,
                endringstype = "",
                kommuneNr = "1234",
                kommuneNavn = "OSLO",
                arkivReferanse = UUID.randomUUID().toString(),
            )

        @Test
        fun `erBarnehageBarnMottattTidligere returnerer false hvis det ikke er noe barnehagebarn med samme ident`() {
            // Act
            val erBarnehagebarnMottattTidligere = barnehagebarnService.erBarnehageBarnMottattTidligere(barnehagebarn)

            // Assert
            assertThat(erBarnehagebarnMottattTidligere).isFalse()
        }

        @Test
        fun `erBarnehageBarnMottattTidligere returnerer true hvis fom, tom og arkivReferanse er like`() {
            // Arrange
            val barnehagebarnMedLikFomTomOgArkivReferanse =
                barnehagebarn.copy(
                    antallTimerIBarnehage = 15.0,
                    endringstype = "",
                    kommuneNavn = "VIKEN",
                    kommuneNr = "4321",
                )

            barnehagebarnService.lagreBarnehageBarn(barnehagebarn)

            // Act
            val erBarnehagebarnMottattTidligere = barnehagebarnService.erBarnehageBarnMottattTidligere(barnehagebarnMedLikFomTomOgArkivReferanse)

            // Assert
            assertThat(erBarnehagebarnMottattTidligere).isTrue()
        }

        @Test
        fun `erBarnehageBarnMottattTidligere returnerer false hvis fom er ulik`() {
            // Arrange
            val barnehagebarnMedUlikFom = barnehagebarn.copy(fom = LocalDate.now().minusMonths(1))

            barnehagebarnService.lagreBarnehageBarn(barnehagebarn)

            // Act
            val erBarnehagebarnMottattTidligere = barnehagebarnService.erBarnehageBarnMottattTidligere(barnehagebarnMedUlikFom)

            // Assert
            assertThat(erBarnehagebarnMottattTidligere).isFalse()
        }

        @Test
        fun `erBarnehageBarnMottattTidligere returnerer false hvis tom er ulik`() {
            // Arrange
            val barnehagebarnMedUlikFom = barnehagebarn.copy(tom = LocalDate.now().plusMonths(1))

            barnehagebarnService.lagreBarnehageBarn(barnehagebarn)

            // Act
            val erBarnehagebarnMottattTidligere = barnehagebarnService.erBarnehageBarnMottattTidligere(barnehagebarnMedUlikFom)

            // Assert
            assertThat(erBarnehagebarnMottattTidligere).isFalse()
        }

        @Test
        fun `erBarnehageBarnMottattTidligere returnerer false hvis arkivReferanse er ulik`() {
            // Arrange
            val barnehagebarnMedUlikFom = barnehagebarn.copy(arkivReferanse = UUID.randomUUID().toString())

            barnehagebarnService.lagreBarnehageBarn(barnehagebarn)

            // Act
            val erBarnehagebarnMottattTidligere = barnehagebarnService.erBarnehageBarnMottattTidligere(barnehagebarnMedUlikFom)

            // Assert
            assertThat(erBarnehagebarnMottattTidligere).isFalse()
        }
    }

    @Nested
    inner class HentBarnehageBarn {
        @Test
        @Sql(scripts = ["/barnehagelister/avvik-antall-timer-og-perioder.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        fun `hentBarnehageBarn henter kun barn med riktig ident`() {
            // Arrange
            // SQL setter inn tre barn med ident = 12345678901. Ett av dem er fra en tidligere periode og det andre fra en tidligere innsending, men ellers duplikat
            // Forventer at duplikat ikke dukker opp

            val barnehagebarnRequestParams1 =
                BarnehagebarnRequestParams(
                    ident = "12345678901",
                    kommuneNavn = null,
                    kunLøpendeAndel = false,
                )

            // Act
            val barnehagebarn = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams = barnehagebarnRequestParams1).content

            // Assert
            assertThat(barnehagebarn.size).isEqualTo(2)
            assertThat(barnehagebarn.map { it.ident }.toSet().single()).isEqualTo("12345678901")
            assertThat(barnehagebarn.map { it.fom }.toSet()).isEqualTo(setOf(LocalDate.now().minusMonths(3), LocalDate.now()))
            assertThat(barnehagebarn.find { it.ident == "12345678901" && it.fom == LocalDate.now() }?.endretTid).isAfter(LocalDateTime.now().minusHours(2))
        }

        @Test
        @Sql(scripts = ["/barnehagelister/avvik-antall-timer-og-perioder.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        fun `hentBarnehageBarn henter kun barn med riktig kommune`() {
            // Arrange
            // SQL setter inn tre barn med kommune = Oslo. Ett av dem er fra en tidligere periode og det andre fra en tidligere innsending, men ellers duplikat
            // Forventer at duplikat ikke dukker opp

            val barnehagebarnRequestParams1 =
                BarnehagebarnRequestParams(
                    ident = null,
                    kommuneNavn = "Oslo",
                    kunLøpendeAndel = false,
                )

            // Act
            val barnehagebarn = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams = barnehagebarnRequestParams1).content

            // Assert
            assertThat(barnehagebarn.size).isEqualTo(2)
            assertThat(barnehagebarn.map { it.kommuneNavn }.toSet().single()).isEqualTo("Oslo")
            assertThat(barnehagebarn.map { it.fom }.toSet()).isEqualTo(setOf(LocalDate.now().minusMonths(3), LocalDate.now()))
        }

        @Test
        @Sql(scripts = ["/barnehagelister/avvik-antall-timer-og-perioder.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        fun `hentBarnehageBarn henter alle barn hvis det ikke er oppgitt noen ident eller kommune`() {
            // Arrange
            // SQL setter inn 5 barn, tre med samme ident. Ett av dem er fra en tidligere periode og det andre fra en tidligere innsending, men ellers duplikat
            // Forventer at duplikat ikke dukker opp

            val barnehagebarnRequestParams1 =
                BarnehagebarnRequestParams(
                    ident = null,
                    kommuneNavn = null,
                    kunLøpendeAndel = false,
                )

            // Act
            val barnehagebarn = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams = barnehagebarnRequestParams1).content

            // Assert
            assertThat(barnehagebarn.size).isEqualTo(4)
        }

        @Test
        @Sql(scripts = ["/barnehagelister/avvik-antall-timer-og-perioder.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        fun `hentBarnehageBarn henter kun barn med løpende andeler`() {
            // Arrange
            val barnehagebarnRequestParams1 =
                BarnehagebarnRequestParams(
                    ident = null,
                    kommuneNavn = null,
                    kunLøpendeAndel = true,
                )

            // Act
            val barnehagebarn = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams = barnehagebarnRequestParams1).content

            // Assert
            assertThat(barnehagebarn.size).isEqualTo(3)
            assertThat(barnehagebarn.map { it.ident }.toSet()).isEqualTo(setOf("12345678901", "23456789012"))
        }

        @Test
        @Sql(scripts = ["/barnehagelister/avvik-antall-timer-og-perioder.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        fun `hentBarnehageBarn gir paginering med flere sider`() {
            // Arrange
            val barnehagebarnRequestParamsFørsteSide =
                BarnehagebarnRequestParams(
                    ident = null,
                    kommuneNavn = null,
                    kunLøpendeAndel = false,
                    limit = 2,
                    sortBy = "endrettidspunkt",
                    sortAsc = true,
                    offset = 0,
                )
            val barnehagebarnRequestParamsTredjeSide =
                BarnehagebarnRequestParams(
                    ident = null,
                    kommuneNavn = null,
                    kunLøpendeAndel = false,
                    limit = 2,
                    sortBy = "endrettidspunkt",
                    sortAsc = true,
                    offset = 1,
                )

            // Act
            val barnehagebarnSide1 = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams = barnehagebarnRequestParamsFørsteSide).content
            val barnehagebarnSide3 = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams = barnehagebarnRequestParamsTredjeSide).content

            // Assert
            assertThat(barnehagebarnSide1.size).isEqualTo(2)
            assertThat(barnehagebarnSide3.size).isEqualTo(2)

            assertThat(barnehagebarnSide1.first().ident).isEqualTo("23456789012")
            assertThat(barnehagebarnSide3.last().ident).isEqualTo("12345678901")
        }
    }
}
