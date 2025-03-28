package no.nav.familie.ks.sak.barnehagelister

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.epost.EpostService
import no.nav.familie.ks.sak.data.lagBarnehageBarn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class BarnehagelisteVarslingServiceIntegrasjonsTest(
    @Autowired private val barnehageBarnRepository: BarnehagebarnRepository,
) : OppslagSpringRunnerTest() {
    private val epostService = mockk<EpostService>()
    private val barnehagelisteVarslingService = BarnehagelisteVarslingService(barnehageBarnRepository, epostService)

    @BeforeEach
    fun setup() {
        clearMocks(epostService)
        every { epostService.sendEpostVarslingBarnehagelister(any(), any()) } just runs
    }

    @Test
    fun `sendVarslingOmNyBarnehagelisteTilEnhet sender epost for hver nye kommune som blir funnet`() {
        // Arrange
        val barnehageBarnSendtForLengeSiden =
            lagBarnehageBarn(
                endretTidspunkt = LocalDateTime.now().minusMonths(2),
                kommuneNr = "0301",
            )
        val barnehagebarnSendtIlaSisteDøgnFraKommuneSomHarBlittSendtTidligere =
            lagBarnehageBarn(
                endretTidspunkt = LocalDateTime.now().minusHours(2),
                kommuneNr = "0301",
            )
        val barnehageBarnSendtIlaSisteDøgn =
            lagBarnehageBarn(
                endretTidspunkt = LocalDateTime.now().minusHours(2),
                kommuneNr = "0302",
            )
        barnehageBarnRepository.saveAll(
            listOf(
                barnehageBarnSendtForLengeSiden,
                barnehagebarnSendtIlaSisteDøgnFraKommuneSomHarBlittSendtTidligere,
                barnehageBarnSendtIlaSisteDøgn,
            ),
        )

        // Act
        barnehagelisteVarslingService.sendVarslingOmNyBarnehagelisteTilEnhet()

        // Assert
        val kommuneSlot = slot<Set<String>>()
        verify(exactly = 1) { epostService.sendEpostVarslingBarnehagelister(any(), capture(kommuneSlot)) }
        assertThat(kommuneSlot.captured).isEqualTo(setOf("0302"))
    }

    @Test
    fun `sendVarslingOmNyBarnehagelisteTilEnhet sender ikke epost hvis det ikke er noen nye kommuner`() {
        // Arrange
        val barnehageBarnSendtForLengeSiden =
            lagBarnehageBarn(
                endretTidspunkt = LocalDateTime.now().minusMonths(2),
                kommuneNr = "0301",
            )
        val barnehagebarnSendtIlaSisteDøgnFraKommuneSomHarBlittSendtTidligere =
            lagBarnehageBarn(
                endretTidspunkt = LocalDateTime.now().minusHours(2),
                kommuneNr = "0301",
            )

        barnehageBarnRepository.saveAll(
            listOf(
                barnehageBarnSendtForLengeSiden,
                barnehagebarnSendtIlaSisteDøgnFraKommuneSomHarBlittSendtTidligere,
            ),
        )

        // Act
        barnehagelisteVarslingService.sendVarslingOmNyBarnehagelisteTilEnhet()

        // Assert
        val kommuneSlot = slot<Set<String>>()
        verify(exactly = 0) { epostService.sendEpostVarslingBarnehagelister(any(), capture(kommuneSlot)) }
    }
}
