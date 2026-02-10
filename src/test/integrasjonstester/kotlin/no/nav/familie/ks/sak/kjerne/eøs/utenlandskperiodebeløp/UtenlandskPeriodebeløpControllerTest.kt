package no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import jakarta.validation.ConstraintViolationException
import no.nav.familie.ks.sak.api.dto.UtenlandskPeriodebeløpDto
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration(classes = [TestConfig::class, UtenlandskPeriodebeløpController::class, ValidationAutoConfiguration::class])
@ActiveProfiles("postgres")
class UtenlandskPeriodebeløpControllerTest {
    @Autowired
    lateinit var utenlandskPeriodebeløpController: UtenlandskPeriodebeløpController

    @Autowired
    lateinit var utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository

    @Autowired
    lateinit var utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Test
    fun `Skal kaste feil dersom validering av input feiler`() {
        val exception =
            assertThrows<ConstraintViolationException> {
                utenlandskPeriodebeløpController.oppdaterUtenlandskPeriodebeløp(
                    1,
                    UtenlandskPeriodebeløpDto(1, null, null, emptyList(), beløp = (-1.0).toBigDecimal(), null, null, null),
                )
            }

        val forventedeFelterMedFeil = listOf("beløp")
        val faktiskeFelterMedFeil =
            exception.constraintViolations.map { constraintViolation ->
                constraintViolation.propertyPath
                    .toString()
                    .split(".")
                    .last()
            }

        assertThat(faktiskeFelterMedFeil).hasSize(1).containsAll(forventedeFelterMedFeil)
    }

    @Test
    fun `Skal ikke kaste feil dersom validering av input går bra`() {
        every { utenlandskPeriodebeløpRepository.getReferenceById(any()) } returns UtenlandskPeriodebeløp.NULL
        every { utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(any(), any()) } just runs

        val response =
            utenlandskPeriodebeløpController.oppdaterUtenlandskPeriodebeløp(
                1,
                UtenlandskPeriodebeløpDto(1, null, null, emptyList(), beløp = 1.0.toBigDecimal(), null, null, null),
            )

        assertEquals(HttpStatus.OK, response.statusCode)
    }
}

class TestConfig {
    @Bean
    fun utenlandskPeriodebeløpService(): UtenlandskPeriodebeløpService = mockk()

    @Bean
    fun utenlandskPeriodebeløpRepository(): UtenlandskPeriodebeløpRepository = mockk()

    @Bean
    fun personidentService(): PersonidentService = mockk()

    @Bean
    fun behandlingService(): BehandlingService = mockk(relaxed = true)

    @Bean
    fun tilgangService(): TilgangService = mockk(relaxed = true)
}
