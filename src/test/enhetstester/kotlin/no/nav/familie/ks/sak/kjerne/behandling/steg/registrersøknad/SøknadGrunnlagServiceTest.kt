package no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene.SøknadGrunnlag
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene.SøknadGrunnlagRepository
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.hamcrest.CoreMatchers.`is` as Is

class SøknadGrunnlagServiceTest {
    private val søknadGrunnlagRepository = mockk<SøknadGrunnlagRepository>()

    private val søknadGrunnlagService = SøknadGrunnlagService(søknadGrunnlagRepository)

    @Test
    fun `lagreOgDeaktiverGammel - skal hente eksisterende aktiv søknad tilknyttet behandlingId og sette den til inaktiv og deretter lagre ny`() {
        // Arrange
        val deaktivertSøknadSlot = slot<SøknadGrunnlag>()
        val nySøknadSlot = slot<SøknadGrunnlag>()

        val nySøknadGrunnlag =
            // Act
            SøknadGrunnlag(
                behandlingId = 0,
                aktiv = true,
                søknad = "",
            )

        val gammelSøknadGrunnlag =
            SøknadGrunnlag(
                behandlingId = 0,
                aktiv = true,
                søknad = "",
            )

        every { søknadGrunnlagRepository.finnAktiv(any()) } returns gammelSøknadGrunnlag
        every { søknadGrunnlagRepository.saveAndFlush(capture(deaktivertSøknadSlot)) } returns mockk()
        every { søknadGrunnlagRepository.save(capture(nySøknadSlot)) } returns nySøknadGrunnlag

        søknadGrunnlagService.lagreOgDeaktiverGammel(nySøknadGrunnlag)

        // Assert
        assertFalse(deaktivertSøknadSlot.captured.aktiv)
        assertTrue(nySøknadSlot.captured.aktiv)
    }

    @Test
    fun `finnAktiv - skal hente aktiv søknad tilknyttet behandlingId når den finnes`() {
        // Arrange
        val søknadGrunnlag =
            // Act
            SøknadGrunnlag(
                behandlingId = 0,
                aktiv = true,
                søknad = "",
            )
        every { søknadGrunnlagRepository.finnAktiv(any()) } returns søknadGrunnlag

        val aktivSøknad = søknadGrunnlagService.finnAktiv(0L)

        // Assert
        assertNotNull(aktivSøknad)
    }

    @Test
    fun `finnAktiv - skal returnere null dersom søknad tilknyttet behandlingId ikke finnes`() {
        // Arrange
        every { søknadGrunnlagRepository.finnAktiv(any()) } returns null

        val aktivSøknad = søknadGrunnlagService.finnAktiv(404L)

        // Assert
        assertNull(aktivSøknad)
        // Act
    }

    @Test
    fun `hentAktiv - skal hente aktiv søknad tilknyttet behandlingId når den finnes`() {
        // Arrange
        val søknadGrunnlag =
            // Act
            SøknadGrunnlag(
                behandlingId = 0,
                aktiv = true,
                søknad = "",
            )
        every { søknadGrunnlagRepository.finnAktiv(any()) } returns søknadGrunnlag

        val aktivSøknad = søknadGrunnlagService.hentAktiv(0L)

        // Assert
        assertNotNull(aktivSøknad)
    }

    @Test
    fun `hentAktiv - skal kaste feil dersom søknad tilknyttet behandlingId ikke finnes`() {
        // Arrange
        every { søknadGrunnlagRepository.finnAktiv(0L) } returns null

        val feil =
            // Act
            // Assert
            assertThrows<Feil> {
                søknadGrunnlagService.hentAktiv(0L)
            }

        assertThat(feil.message, Is("Fant ikke aktiv søknadsgrunnlag for behandling 0."))
    }
}
