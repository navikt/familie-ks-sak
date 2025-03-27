package no.nav.familie.ks.sak.no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.barnehagelister.epost.EpostService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class EpostServiceTest(
    @Autowired private val epostService: EpostService,
) : OppslagSpringRunnerTest() {
    @Test
    fun `test med prodverdier`() {
        epostService.sendEpostVarslingBarnehagelister("fredrik.markus.pfeil@nav.no", listOf("hei"))
    }
}
