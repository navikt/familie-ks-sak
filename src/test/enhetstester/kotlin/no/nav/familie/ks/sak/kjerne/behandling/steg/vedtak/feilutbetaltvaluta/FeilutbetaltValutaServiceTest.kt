package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import org.hamcrest.CoreMatchers.`is` as Is

class FeilutbetaltValutaServiceTest {
    private val feilutbetaltValutaRepository = mockk<FeilutbetaltValutaRepository>()
    private val loggService = mockk<LoggService>()
    private val personopplysningGrunnlagService = mockk<PersonopplysningGrunnlagService>()

    private val feilutbetaltValutaService =
        FeilutbetaltValutaService(
            feilutbetaltValutaRepository = feilutbetaltValutaRepository,
            loggService = loggService,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
        )

    @Test
    fun `hentFeilutbetaltValuta - skal returnere feilutbetaltValuta dersom det finnes en med oppgitt id`() {
        // Arrange
        val feilutbetaltValutaSomSkalFinnes = mockk<FeilutbetaltValuta>()

        every { feilutbetaltValutaRepository.finnFeilutbetaltValuta(any()) } returns feilutbetaltValutaSomSkalFinnes

        // Act
        val feilutbetaltValuta = feilutbetaltValutaService.hentFeilutbetaltValuta(0)

        // Assert
        assertThat(feilutbetaltValuta, Is(feilutbetaltValutaSomSkalFinnes))
    }

    @Test
    fun `hentAlleFeilutbetaltValutaForBehandling - skal returnere alle feilutbetaltValuta for behandling`() {
        // Arrange
        every { feilutbetaltValutaRepository.finnFeilutbetalteValutaForBehandling(0) } returns listOf(mockk(), mockk())

        // Act
        val alleFeilutbetaltValutaForBehandling =
            feilutbetaltValutaService.hentAlleFeilutbetaltValutaForBehandling(0)

        // Assert
        assertThat(alleFeilutbetaltValutaForBehandling.size, Is(2))
    }

    @Test
    fun `hentFeilutbetaltValuta - skal kaste feil dersom feilutbetaltValuta med oppgitt id ikke finnes`() {
        // Arrange
        every { feilutbetaltValutaRepository.finnFeilutbetaltValuta(any()) } returns null

        // Act & Assert
        val feil = assertThrows<Feil> { feilutbetaltValutaService.hentFeilutbetaltValuta(0) }

        // Assert
        assertThat(feil.message, Is("Finner ikke feilutbetalt valuta med id=0"))
    }

    @Test
    fun `leggTilFeilutbetaltValuta - skal lagre feilutbetaltValuta og lagre logg på det`() {
        // Arrange
        val feilutbetaltValuta =
            FeilutbetaltValuta(
                behandlingId = 0,
                fom = LocalDate.of(2020, 12, 12),
                tom = LocalDate.of(2022, 12, 12),
                feilutbetaltBeløp = 0,
                id = 0,
            )

        every { feilutbetaltValutaRepository.save(any()) } returnsArgument 0
        every { loggService.opprettFeilutbetaltValutaLagtTilLogg(feilutbetaltValuta) } returns mockk()

        // Act
        val lagtTilFeilutbetaltValuta = feilutbetaltValutaService.leggTilFeilutbetaltValuta(feilutbetaltValuta)

        // Assert
        assertThat(lagtTilFeilutbetaltValuta, Is(feilutbetaltValuta))

        verify(exactly = 1) { feilutbetaltValutaRepository.save(any()) }
        verify(exactly = 1) { loggService.opprettFeilutbetaltValutaLagtTilLogg(feilutbetaltValuta) }
    }
}
