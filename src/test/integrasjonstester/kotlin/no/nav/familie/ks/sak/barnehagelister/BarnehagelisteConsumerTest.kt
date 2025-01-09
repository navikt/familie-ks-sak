package no.nav.familie.ks.sak.no.nav.familie.ks.sak.barnehagelister

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.barnehagelister.BarnehagebarnService
import no.nav.familie.ks.sak.barnehagelister.BarnehagelisteConsumer
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.support.Acknowledgment
import java.time.LocalDate
import java.util.UUID

class BarnehagelisteConsumerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var barnehagebarnRepository: BarnehagebarnRepository

    @Autowired
    private lateinit var barnehagebarnService: BarnehagebarnService

    @Autowired
    private lateinit var barnehagelisteConsumer: BarnehagelisteConsumer

    val acknowledgment = mockk<Acknowledgment>()

    @BeforeEach
    fun beforeAll() {
        every { acknowledgment.acknowledge() } just runs
    }

    @Test
    fun `skal ikke lagre ned barnehagebarn hvis det har samme ident, fom, tom og arkivreferanse som et allerede eksisterende barnehagebarn`() {
        // Arrange
        barnehagebarnRepository.save(barnehagebarn)

        val barnehagebarnMedLikFomTomOgArkivReferanse =
            barnehagebarn.copy(
                antallTimerIBarnehage = 15.0,
                endringstype = "",
                kommuneNavn = "VIKEN",
                kommuneNr = "4321",
            )

        val melding = objectMapper.writeValueAsString(barnehagebarnMedLikFomTomOgArkivReferanse)

        // Act
        barnehagelisteConsumer.listen(melding, acknowledgment)

        // Assert
        val barnehagebarnInDb = barnehagebarnRepository.findAllByIdent(barnehagebarn.ident).single()
        assertThat(barnehagebarnInDb).isEqualTo(barnehagebarn)

        verify(exactly = 1) { acknowledgment.acknowledge() }
    }

    @Test
    fun `skal lagre ned nytt barnehagebarn hvis det ikke er noe barn fra før`() {
        // Arrange
        val melding = objectMapper.writeValueAsString(barnehagebarn)

        // Act
        barnehagelisteConsumer.listen(melding, acknowledgment)

        // Assert
        val barnehagebarnInDb = barnehagebarnRepository.findAllByIdent(barnehagebarn.ident).single()
        assertThat(barnehagebarnInDb).isEqualTo(barnehagebarn)

        verify(exactly = 1) { acknowledgment.acknowledge() }
    }

    @ParameterizedTest
    @MethodSource("provideBarnehagebarn")
    fun `skal lagre ned nytt barnehagebarn hvis det er forskjellig fom, tom eller arkivreferanse`(nyttBarnehagebarn: Barnehagebarn) {
        // Arrange
        barnehagebarnService.lagreBarnehageBarn(barnehagebarn)

        val melding = objectMapper.writeValueAsString(nyttBarnehagebarn)

        // Act
        barnehagelisteConsumer.listen(melding, acknowledgment)

        // Assert
        val barnehagebarnInDb = barnehagebarnRepository.findAllByIdent(barnehagebarn.ident)
        assertThat(barnehagebarnInDb).hasSize(2)

        verify(exactly = 1) { acknowledgment.acknowledge() }
    }

    companion object {
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

        @JvmStatic
        fun provideBarnehagebarn(): List<Barnehagebarn> =
            listOf(
                barnehagebarn.copy(id = UUID.randomUUID(), fom = LocalDate.now().plusMonths(4)),
                barnehagebarn.copy(id = UUID.randomUUID(), tom = null),
                barnehagebarn.copy(id = UUID.randomUUID(), arkivReferanse = UUID.randomUUID().toString()),
            )
    }
}
