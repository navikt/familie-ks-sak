package no.nav.familie.ks.sak.barnehagelister

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.epost.EpostService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

class BarnehagelisteVarslingServiceIntegrasjonsTest(
    @Autowired private val barnehagebarnRepository: BarnehagebarnRepository,
) : OppslagSpringRunnerTest() {
    private val barnehageBarnService =
        BarnehagebarnService(
            barnehagebarnRepository = barnehagebarnRepository,
            andelTilkjentYtelseRepository = mockk(),
            fagsakRepository = mockk(),
            behandlingRepository = mockk(),
            vilkårsvurderingService = mockk(),
            taskService = mockk(),
        )
    private val epostService = mockk<EpostService>()
    private val barnehagelisteVarslingService =
        BarnehagelisteVarslingService(barnehageBarnService, epostService)

    @BeforeEach
    fun setup() {
        clearMocks(epostService)
        every { epostService.sendEpostVarslingBarnehagelister(any(), any()) } just runs
    }

    @Test
    @Sql(
        scripts = ["/barnehagelister/barnehagebarn-sendt-inn-siste-døgn.sql"],
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    )
    fun `sendVarslingOmNyBarnehagelisteTilEnhet sender epost når en kommune sender inn for første gang ila én måned`() {
        // Act
        barnehagelisteVarslingService.sendVarslingOmNyBarnehagelisteTilEnhet()

        // Assert
        verify(exactly = 1) {
            epostService.sendEpostVarslingBarnehagelister(
                BarnehagelisteVarslingService.`KONTAKT_E-POST_BERGEN`,
                setOf("Grünerløkka", "Sagene", "Moss"),
            )
        }
    }

    @Test
    @Sql(
        scripts = ["/barnehagelister/barnehagebarn-sendt-inn-for-mer-enn-ett-døgn-siden.sql"],
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    )
    fun `sendVarslingOmNyBarnehagelisteTilEnhet sender ikke epost hvis det ikke er blitt sendt inn noe ila det siste døgnet`() {
        // Act
        barnehagelisteVarslingService.sendVarslingOmNyBarnehagelisteTilEnhet()

        // Assert
        verify(exactly = 0) { epostService.sendEpostVarslingBarnehagelister(any(), any()) }
    }
}
