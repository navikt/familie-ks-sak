package no.nav.familie.ks.sak.no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.BarnehagebarnService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import java.time.LocalDate

class BarnehagebarnServiceHentBarnIntegrasjonsTest(
    @Autowired private val barnehagebarnService: BarnehagebarnService,
) : OppslagSpringRunnerTest() {
    @Test
    @Sql(
        scripts = ["/barnehagelister/barnehagebarn-uten-fagsak.sql"],
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    )
    fun `hentBarnehageBarn henter et barn uten info hvis barnet ikke har en fagsak enda`() {
        // Arrange
        val barnehagebarnRequestParams1 =
            BarnehagebarnRequestParams(
                ident = "98765432109",
                kommuneNavn = null,
                kunLøpendeAndel = false,
            )

        // Act
        val barnehagebarn =
            barnehagebarnService.hentBarnehagebarnForVisning(barnehagebarnRequestParams = barnehagebarnRequestParams1).content

        // Assert
        assertThat(barnehagebarn).hasSize(1)
    }

    @Test
    @Sql(
        scripts = ["/barnehagelister/barnehagebarn-med-forskjellig-ident.sql"],
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    )
    fun `hentBarnehageBarn henter kun barn med riktig ident`() {
        // Arrange
        val barnehagebarnRequestParams1 =
            BarnehagebarnRequestParams(
                ident = "12345678901",
                kommuneNavn = null,
                kunLøpendeAndel = false,
            )

        // Act
        val barnehagebarn =
            barnehagebarnService.hentBarnehagebarnForVisning(barnehagebarnRequestParams = barnehagebarnRequestParams1).content

        // Assert
        assertThat(barnehagebarn.size).isEqualTo(2)
        assertThat(barnehagebarn.map { it.ident }.toSet().single()).isEqualTo("12345678901")
    }

    @Test
    @Sql(
        scripts = ["/barnehagelister/barnehagebarn-med-forskjellig-kommune.sql"],
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    )
    fun `hentBarnehageBarn henter kun barn med riktig kommune`() {
        // Arrange
        val barnehagebarnRequestParams1 =
            BarnehagebarnRequestParams(
                ident = null,
                kommuneNavn = "Oslo",
                kunLøpendeAndel = false,
            )

        // Act
        val barnehagebarn =
            barnehagebarnService.hentBarnehagebarnForVisning(barnehagebarnRequestParams = barnehagebarnRequestParams1).content

        // Assert
        assertThat(barnehagebarn.size).isEqualTo(2)
        assertThat(barnehagebarn.map { it.kommuneNavn.lowercase() }.toSet().single()).isEqualTo("oslo")
    }

    @Test
    @Sql(
        scripts = ["/barnehagelister/barnehagebarn-komplekst-case.sql"],
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    )
    fun `hentBarnehageBarn henter alle barn hvis det ikke er oppgitt noen ident eller kommune`() {
        // Arrange
        val barnehagebarnRequestParams1 =
            BarnehagebarnRequestParams(
                ident = null,
                kommuneNavn = null,
                kunLøpendeAndel = false,
            )

        // Act
        val barnehagebarn =
            barnehagebarnService.hentBarnehagebarnForVisning(barnehagebarnRequestParams = barnehagebarnRequestParams1).content

        // Assert
        assertThat(barnehagebarn.size).isEqualTo(4)
        val barnUtenLøpendeAndeler = barnehagebarn.find { it.ident == "45678901234" }
        assertThat(barnUtenLøpendeAndeler?.avvik).`as`("\nForventet avvik: null\n men var: ${barnUtenLøpendeAndeler?.avvik}\n")
    }

    @Test
    @Sql(
        scripts = ["/barnehagelister/barnehagebarn-med-løpende-andeler.sql"],
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    )
    fun `hentBarnehageBarn henter kun barn med løpende andeler`() {
        // Arrange
        val barnehagebarnRequestParams1 =
            BarnehagebarnRequestParams(
                ident = null,
                kommuneNavn = null,
                kunLøpendeAndel = true,
            )

        // Act
        val barnehagebarn =
            barnehagebarnService.hentBarnehagebarnForVisning(barnehagebarnRequestParams = barnehagebarnRequestParams1).content

        // Assert
        assertThat(barnehagebarn.size).`as`("Forventet 2 barn, fikk ${barnehagebarn.size}").isEqualTo(2)
        assertThat(barnehagebarn.map { it.ident }.toSet()).isEqualTo(setOf("12345678901"))
    }

    @Test
    @Sql(
        scripts = ["/barnehagelister/barnehagebarn-med-avvik.sql"],
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    )
    fun `hentBarnehageBarn med riktig avvik`() {
        // Arrange
        val barnehagebarnRequestParams1 =
            BarnehagebarnRequestParams(
                ident = null,
                kommuneNavn = null,
                kunLøpendeAndel = false,
            )

        // Act
        val barnehagebarn =
            barnehagebarnService.hentBarnehagebarnForVisning(barnehagebarnRequestParams = barnehagebarnRequestParams1).content

        // Assert
        assertThat(barnehagebarn.size).`as`("Forventet 4 barn, fikk ${barnehagebarn.size}").isEqualTo(4)

        val alleBarnMedAvvik = barnehagebarn.filter { it.avvik == true }
        assertThat(alleBarnMedAvvik.size).isEqualTo(3)
        assertThat(alleBarnMedAvvik.map { it.ident to it.fom }.toSet())
            .`as`("Barnehagebarn med avvik")
            .isEqualTo(
                setOf(
                    ("12345678901" to LocalDate.now().minusMonths(3)),
                    ("12345678901" to LocalDate.now()),
                    ("23456789012" to LocalDate.now().minusMonths(3)),
                ),
            )
    }

    @Test
    @Sql(
        scripts = ["/barnehagelister/barnehagebarn-med-fulltid-barnehageplass.sql"],
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    )
    fun `hentBarnehageBarn oppgir ikke avvik så lenge barnet har fulltidsplass selv hvis det er forskjellig antall timer`() {
        // Arrange
        val barnehagebarnRequestParams =
            BarnehagebarnRequestParams(
                ident = null,
                kommuneNavn = null,
                kunLøpendeAndel = false,
            )

        // Act
        val barnehagebarn =
            barnehagebarnService.hentBarnehagebarnForVisning(barnehagebarnRequestParams = barnehagebarnRequestParams).content

        // Assert
        assertThat(barnehagebarn.size).`as`("Forventet 1 barn, fikk ${barnehagebarn.size}").isEqualTo(1)

        assertThat(barnehagebarn.single().avvik).`as`("Forventet ikke avvik").isEqualTo(false)
    }

    @Test
    @Sql(
        scripts = ["/barnehagelister/barnehagebarn-forskjellig-endret-tid.sql"],
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    )
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
        val barnehagebarnRequestParamsAndreSide =
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
        val barnehagebarnSide1 =
            barnehagebarnService.hentBarnehagebarnForVisning(barnehagebarnRequestParams = barnehagebarnRequestParamsFørsteSide).content
        val barnehagebarnSide2 =
            barnehagebarnService.hentBarnehagebarnForVisning(barnehagebarnRequestParams = barnehagebarnRequestParamsAndreSide).content

        // Assert
        assertThat(barnehagebarnSide1.size).`as`("Side 1").isEqualTo(2)
        assertThat(barnehagebarnSide2.size).`as`("Side 2").isEqualTo(1)

        assertThat(barnehagebarnSide1.first().ident)
            .`as`("Barn som ble endret for lengst siden:")
            .isEqualTo("23456789012")
        assertThat(barnehagebarnSide2.last().ident).`as`("Barn som ble endret nyligst:").isEqualTo("12345678901")
    }

    @Test
    @Sql(
        scripts = ["/barnehagelister/barnehagebarn-19mnd-lopende-andel.sql"],
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    )
    fun `hentBarnehageBarn henter barn som fyller 19 mnd i inneværende mnd med løpende andel`() {
        // Arrange
        val params =
            BarnehagebarnRequestParams(
                ident = null,
                kommuneNavn = null,
                kunLøpendeAndel = true,
            )

        // Act
        val barnehagebarn =
            barnehagebarnService.hentBarnehagebarnForVisning(barnehagebarnRequestParams = params).content

        // Assert
        assertThat(barnehagebarn.size).`as`("Forventet 1 barn, fikk ${barnehagebarn.size}").isEqualTo(1)

        assertThat(barnehagebarn.map { it.ident }).containsExactly("34567890123")
    }

    @Test
    @Sql(
        scripts = ["/barnehagelister/barnehagebarn-lopende-andel-i-ikke-aktiv-behandling.sql"],
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    )
    fun `hentBarnehageBarn henter barn med løpende andel i behandling som ikke er aktiv`() {
        // Arrange
        val params =
            BarnehagebarnRequestParams(
                ident = null,
                kommuneNavn = null,
                kunLøpendeAndel = true,
            )

        // Act
        val barnehagebarn =
            barnehagebarnService.hentBarnehagebarnForVisning(barnehagebarnRequestParams = params).content

        // Assert
        assertThat(barnehagebarn.size).`as`("Forventet 1 barn, fikk ${barnehagebarn.size}").isEqualTo(1)

        assertThat(barnehagebarn.map { it.ident }).containsExactly("34567890123")
    }
}
