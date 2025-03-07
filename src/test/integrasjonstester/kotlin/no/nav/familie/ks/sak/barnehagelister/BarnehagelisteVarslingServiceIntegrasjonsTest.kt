package no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.data.lagBarnehageBarn
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class BarnehagelisteVarslingServiceIntegrasjonsTest(
    @Autowired private val barnehagelisteVarslingService: BarnehagelisteVarslingService,
    @Autowired private val barnehageBarnRepository: BarnehagebarnRepository,
) : OppslagSpringRunnerTest() {
    @Test
    fun `finnKommunerSendtInnSisteDøgn finner kommuner som har sendt inn barn det siste døgnet`() {
        // Arrange
        val barnehageBarnSendtForLengeSiden =
            lagBarnehageBarn(
                endretTidspunkt = LocalDateTime.now().minusMonths(2),
                kommuneNavn = "Oslo",
            )
        val barnehageBarnSendtIlaSisteDøgn =
            lagBarnehageBarn(
                endretTidspunkt = LocalDateTime.now().minusHours(2),
                kommuneNavn = "Bergen",
            )
        barnehageBarnRepository.saveAll(
            listOf(
                barnehageBarnSendtForLengeSiden,
                barnehageBarnSendtIlaSisteDøgn,
            ),
        )

        // Act
        val kommunerMedNyeInnsendinger = barnehagelisteVarslingService.finnKommunerSendtInnSisteDøgn()

        // Assert
        assertThat(kommunerMedNyeInnsendinger).isEqualTo(listOf(barnehageBarnSendtIlaSisteDøgn.kommuneNavn))
    }

    @Test
    fun `finnEnhetForKommune finner riktig enhet for en gitt kommune`() {
        // Act
        val faktiskEnhet = barnehagelisteVarslingService.finnEnhetForKommune("0301")

        // Assert
        AssertionsForClassTypes.assertThat(faktiskEnhet).isEqualTo("4812")
    }
}
