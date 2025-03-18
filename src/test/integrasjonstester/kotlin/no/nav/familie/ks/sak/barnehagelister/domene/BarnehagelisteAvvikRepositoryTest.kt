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
        val barnehagebarnDtoInterface = barnehagebarnRepository.finnAlleBarnehagebarn(pageable = Pageable.unpaged()).toSet()

        assertThat(barnehagebarnDtoInterface.size).isEqualTo(3L)
        val barnMedAvvik = barnehagebarnDtoInterface.filter { it.getAvvik() == true }
        assertThat(barnMedAvvik.size).isEqualTo(1)

        val barnMedAvvikMenIkkeLøpendeAndel = barnehagebarnDtoInterface.filter { it.getAvvik() == null }
        assertThat(barnMedAvvikMenIkkeLøpendeAndel.size).isEqualTo(1)
    }

    @Test
    @Sql(scripts = ["/barnehagelister/løpende-andeler.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    fun `findBarnehagebarnLøpendeAndel finner kun barn som har løpende andel`() {
        val barnehagebarnDtoInterface = barnehagebarnRepository.finnAlleBarnehagebarnMedLøpendAndel(pageable = Pageable.unpaged()).toSet()

        assertThat(barnehagebarnDtoInterface.size).isEqualTo(2L)
        val barnMedAvvik = barnehagebarnDtoInterface.filter { it.getAvvik() == true }
        assertThat(barnMedAvvik.size).isEqualTo(1)
    }

    @Test
    @Sql(scripts = ["/barnehagelister/løpende-andeler.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    fun `findBarnehagebarnByIdentOgLøpendeAndel finner kun barn som har løpende andel på ident`() {
        val barnehagebarnDtoInterface = barnehagebarnRepository.finnBarnehagebarnByIdentMedLøpendeAndel(pageable = Pageable.unpaged(), ident = "12345678901").toSet()

        assertThat(barnehagebarnDtoInterface.size).isEqualTo(1L)
        val barnMedAvvik = barnehagebarnDtoInterface.filter { it.getAvvik() == true }
        assertThat(barnMedAvvik.size).isEqualTo(1)

        val barnMedRiktigIdent = barnehagebarnDtoInterface.find { it.getIdent() == "12345678901" }
        assertThat(barnMedRiktigIdent).isNotNull
    }

    @Test
    @Sql(scripts = ["/barnehagelister/løpende-andeler.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    fun `findBarnehagebarnByKommuneNavnOgLøpendeAndel finner kun barn som har løpende andel i riktig kommune`() {
        val barnehagebarnDtoInterface = barnehagebarnRepository.finnBarnehagebarnByKommuneNavnMedLøpendeAndel(pageable = Pageable.unpaged(), kommuneNavn = "OSLO").toSet()

        assertThat(barnehagebarnDtoInterface.size).isEqualTo(1L)
        val barnMedAvvik = barnehagebarnDtoInterface.filter { it.getAvvik() == true }
        assertThat(barnMedAvvik.size).isEqualTo(1)

        val barnFraOslo = barnehagebarnDtoInterface.find { it.getKommuneNavn() == "OSLO" }
        assertThat(barnFraOslo).isNotNull
    }
}
