package no.nav.familie.ks.sak.no.nav.familie.ks.sak.barnehagelister.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.test.context.jdbc.Sql

class BarnehagelisteAvvikRepositoryTest(
    @Autowired
    val barnehagebarnRepository: BarnehagebarnRepository,
) : OppslagSpringRunnerTest() {
    @Test
    @Sql(scripts = ["/barnehagelister/avvik-antall-timer-og-perioder.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    fun `findAlleBarnehagebarnUavhengigAvFagsak med tre barn der ett har avvik på antall timer og ett på feil periode skal kun gi avvik på de barna`() {
        val barnehagebarnDtoInterface = barnehagebarnRepository.findAlleBarnehagebarnUavhengigAvFagsak(pageable = Pageable.unpaged()).toSet()

        assertThat(barnehagebarnDtoInterface.size).isEqualTo(3L)
        val barnMedAvvik = barnehagebarnDtoInterface.filter { it.getAvvik() == true }
        assertThat(barnMedAvvik.size).isEqualTo(2)
        assertThat(barnMedAvvik.find { it.getAntallTimerIBarnehage() == 20.0 }).isEqualTo(1)
    }
}
