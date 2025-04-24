package no.nav.familie.ks.sak.no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.BarnehagebarnPagineringService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import java.time.LocalDate
import java.time.LocalDateTime

class BarnehagebarnPagineringServiceIntegrasjonsTest(
    @Autowired private val barnehagebarnPagineringService: BarnehagebarnPagineringService,
) : OppslagSpringRunnerTest() {
    // TODO: test barn uten fagsak
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
        val barnehagebarn = barnehagebarnPagineringService.hentPaginerteBarnehageBarn(barnehagebarnRequestParams = barnehagebarnRequestParams1).content

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
        val barnehagebarn = barnehagebarnPagineringService.hentPaginerteBarnehageBarn(barnehagebarnRequestParams = barnehagebarnRequestParams1).content

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
        val barnehagebarn = barnehagebarnPagineringService.hentPaginerteBarnehageBarn(barnehagebarnRequestParams = barnehagebarnRequestParams1).content

        // Assert
        assertThat(barnehagebarn.size).isEqualTo(4)
        val barnUtenLøpendeAndeler = barnehagebarn.find { it.ident == "45678901234" }
        assertThat(barnUtenLøpendeAndeler?.avvik).`as`("\nForventet avvik: null\n men var: ${barnUtenLøpendeAndeler?.avvik}\n")
    }

    @Test
    @Sql(scripts = ["/barnehagelister/avvik-antall-timer-og-perioder.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    fun `hentBarnehageBarn henter kun barn med løpende andeler og med riktig avvik`() {
        // Arrange
        val barnehagebarnRequestParams1 =
            BarnehagebarnRequestParams(
                ident = null,
                kommuneNavn = null,
                kunLøpendeAndel = true,
            )

        // Act
        val barnehagebarn = barnehagebarnPagineringService.hentPaginerteBarnehageBarn(barnehagebarnRequestParams = barnehagebarnRequestParams1).content

        // Assert
        assertThat(barnehagebarn.size).isEqualTo(3)
        assertThat(barnehagebarn.map { it.ident }.toSet()).isEqualTo(setOf("12345678901", "23456789012"))

        val barnMedAvvik = barnehagebarn.find { it.ident == "12345678901" }
        val barnUtenAvvik = barnehagebarn.find { it.ident == "23456789012" }
        assertThat(barnMedAvvik?.avvik).`as`("\nForventet avvik: true\n men var: ${barnMedAvvik?.avvik}\n").isTrue()
        assertThat(barnUtenAvvik?.avvik).`as`("\nForventet avvik: false\n men var: ${barnUtenAvvik?.avvik}\n").isFalse()
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
        val barnehagebarnSide1 = barnehagebarnPagineringService.hentPaginerteBarnehageBarn(barnehagebarnRequestParams = barnehagebarnRequestParamsFørsteSide).content
        val barnehagebarnSide3 = barnehagebarnPagineringService.hentPaginerteBarnehageBarn(barnehagebarnRequestParams = barnehagebarnRequestParamsTredjeSide).content

        // Assert
        assertThat(barnehagebarnSide1.size).isEqualTo(2)
        assertThat(barnehagebarnSide3.size).isEqualTo(2)

        assertThat(barnehagebarnSide1.first().ident).isEqualTo("23456789012")
        assertThat(barnehagebarnSide3.last().ident).isEqualTo("12345678901")
    }
}
