package no.nav.familie.ks.sak.kjerne.adopsjon

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.data.randomAktør
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class AdopsjonServiceTest {
    @MockK
    private lateinit var adopsjonRepository: AdopsjonRepository

    @InjectMockKs
    private lateinit var adopsjonService: AdopsjonService

    @Test
    fun `Skal lagre adopsjonsdato hvis det ikke finnes fra før`() {
        // Arrange
        val behandlingId = BehandlingId(1.toLong())
        val barnAktør = randomAktør()
        val adopsjonsdato = LocalDate.now().minusYears(1)

        every { adopsjonRepository.finnAdopsjonForAktørIBehandling(behandlingId.id, any()) } returns null

        val nyAdopsjonSlot = slot<Adopsjon>()
        every { adopsjonRepository.saveAndFlush(capture(nyAdopsjonSlot)) } returns mockk()

        // Act
        adopsjonService.oppdaterAdopsjonsdato(behandlingId, aktør = barnAktør, nyAdopsjonsdato = adopsjonsdato)

        // Assert
        verify(exactly = 0) { adopsjonRepository.delete(any()) }
        verify(exactly = 1) { adopsjonRepository.saveAndFlush(any()) }

        val lagretAdopsjon = nyAdopsjonSlot.captured
        assertThat(lagretAdopsjon.behandlingId).isEqualTo(behandlingId.id)
        assertThat(lagretAdopsjon.aktør).isEqualTo(barnAktør)
        assertThat(lagretAdopsjon.adopsjonsdato).isEqualTo(adopsjonsdato)
    }

    @Test
    fun `Skal slette gammel adopsjonsdato og lagre ny`() {
        // Arrange
        val behandlingId = BehandlingId(1.toLong())
        val barnAktør = randomAktør()
        val gammelAdopsjon = Adopsjon(behandlingId = behandlingId.id, aktør = barnAktør, adopsjonsdato = LocalDate.now().minusYears(2))
        val nyAdopsjonsdato = LocalDate.now().minusYears(1)

        every { adopsjonRepository.finnAdopsjonForAktørIBehandling(behandlingId.id, any()) } returns gammelAdopsjon

        val gammelAdopsjonSlot = slot<Adopsjon>()
        val nyAdopsjonSlot = slot<Adopsjon>()
        every { adopsjonRepository.delete(capture(gammelAdopsjonSlot)) } returns mockk()
        every { adopsjonRepository.flush() } returns mockk()
        every { adopsjonRepository.saveAndFlush(capture(nyAdopsjonSlot)) } returns mockk()

        // Act
        adopsjonService.oppdaterAdopsjonsdato(behandlingId, aktør = barnAktør, nyAdopsjonsdato = nyAdopsjonsdato)

        // Assert
        verify(exactly = 1) { adopsjonRepository.delete(gammelAdopsjon) }
        verify(exactly = 1) { adopsjonRepository.saveAndFlush(any()) }

        val lagretAdopsjon = nyAdopsjonSlot.captured
        assertThat(lagretAdopsjon.behandlingId).isEqualTo(behandlingId.id)
        assertThat(lagretAdopsjon.aktør).isEqualTo(barnAktør)
        assertThat(lagretAdopsjon.adopsjonsdato).isEqualTo(nyAdopsjonsdato)
    }

    @Test
    fun `Skal slette gammel adopsjonsdato hvis ny adopsjonsdato er null`() {
        // Arrange
        val behandlingId = BehandlingId(1.toLong())
        val barnAktør = randomAktør()
        val gammelAdopsjon = Adopsjon(behandlingId = behandlingId.id, aktør = barnAktør, adopsjonsdato = LocalDate.now().minusYears(2))

        every { adopsjonRepository.finnAdopsjonForAktørIBehandling(behandlingId.id, any()) } returns gammelAdopsjon

        val gammelAdopsjonSlot = slot<Adopsjon>()
        every { adopsjonRepository.delete(capture(gammelAdopsjonSlot)) } returns mockk()
        every { adopsjonRepository.flush() } returns mockk()

        // Act
        adopsjonService.oppdaterAdopsjonsdato(behandlingId, aktør = barnAktør, nyAdopsjonsdato = null)

        // Assert
        verify(exactly = 1) { adopsjonRepository.delete(gammelAdopsjon) }
        verify(exactly = 0) { adopsjonRepository.saveAndFlush(any()) }
    }

    @Test
    fun `Skal ikke gjøre noen ting hvis ny adopsjonsdato er lik eksisterende`() {
        // Arrange
        val behandlingId = BehandlingId(1.toLong())
        val barnAktør = randomAktør()
        val adopsjonsdato = LocalDate.now().minusYears(1)
        val gammelAdopsjon = Adopsjon(behandlingId = behandlingId.id, aktør = barnAktør, adopsjonsdato = adopsjonsdato)

        every { adopsjonRepository.finnAdopsjonForAktørIBehandling(behandlingId.id, any()) } returns gammelAdopsjon

        // Act
        adopsjonService.oppdaterAdopsjonsdato(behandlingId, aktør = barnAktør, nyAdopsjonsdato = adopsjonsdato)

        // Assert
        verify(exactly = 0) { adopsjonRepository.delete(any()) }
        verify(exactly = 0) { adopsjonRepository.saveAndFlush(any()) }
    }

    @Test
    fun `Skal ikke gjøre noen ting hvis det verken finnes eksisterende eller ny adopsjonsdato`() {
        // Arrange
        val behandlingId = BehandlingId(1.toLong())
        val barnAktør = randomAktør()

        every { adopsjonRepository.finnAdopsjonForAktørIBehandling(behandlingId.id, any()) } returns null

        // Act
        adopsjonService.oppdaterAdopsjonsdato(behandlingId, aktør = barnAktør, nyAdopsjonsdato = null)

        // Assert
        verify(exactly = 0) { adopsjonRepository.delete(any()) }
        verify(exactly = 0) { adopsjonRepository.saveAndFlush(any()) }
    }
}
