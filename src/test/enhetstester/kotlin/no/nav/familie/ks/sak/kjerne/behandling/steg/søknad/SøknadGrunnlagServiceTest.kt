package no.nav.familie.ks.sak.kjerne.behandling.steg.søknad

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ks.sak.kjerne.behandling.steg.søknad.domene.SøknadGrunnlag
import no.nav.familie.ks.sak.kjerne.behandling.steg.søknad.domene.SøknadGrunnlagRepository
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SøknadGrunnlagServiceTest {

    @MockK
    private lateinit var søknadGrunnlagRepository: SøknadGrunnlagRepository

    @InjectMockKs
    private lateinit var søknadGrunnlagService: SøknadGrunnlagService

    @Test
    fun `lagreOgDeaktiverGammel - skal hente eksisterende aktiv søknad tilknyttet behandlingId og sette den til inaktiv og deretter lagre ny`() {
        val deaktivertSøknadSlot = slot<SøknadGrunnlag>()
        val nySøknadSlot = slot<SøknadGrunnlag>()

        val nySøknadGrunnlag = SøknadGrunnlag(
            behandlingId = 0,
            aktiv = true,
            søknad = ""
        )

        val gammelSøknadGrunnlag = SøknadGrunnlag(
            behandlingId = 0,
            aktiv = true,
            søknad = ""
        )

        every { søknadGrunnlagRepository.finnAktiv(any()) } returns gammelSøknadGrunnlag
        every { søknadGrunnlagRepository.saveAndFlush(capture(deaktivertSøknadSlot)) } returns mockk()
        every { søknadGrunnlagRepository.save(capture(nySøknadSlot)) } returns nySøknadGrunnlag

        søknadGrunnlagService.lagreOgDeaktiverGammel(nySøknadGrunnlag)

        assertFalse(deaktivertSøknadSlot.captured.aktiv)
        assertTrue(nySøknadSlot.captured.aktiv)
    }

    @Test
    fun `hentAktiv - skal hente aktiv søknad tilknyttet behandlingId når den finnes`() {
        val søknadGrunnlag = SøknadGrunnlag(
            behandlingId = 0,
            aktiv = true,
            søknad = ""
        )
        every { søknadGrunnlagRepository.finnAktiv(any()) } returns søknadGrunnlag

        val aktivSøknad = søknadGrunnlagService.finnAktiv(0L)

        assertNotNull(aktivSøknad)
    }

    @Test
    fun `hentAktiv - skal returnere null dersom søknad tilknyttet behandlingId ikke finnes`() {
        every { søknadGrunnlagRepository.finnAktiv(any()) } returns null

        val aktivSøknad = søknadGrunnlagService.finnAktiv(404L)

        assertNull(aktivSøknad)
    }
}
