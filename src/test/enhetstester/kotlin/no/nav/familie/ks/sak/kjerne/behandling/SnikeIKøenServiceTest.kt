package no.nav.familie.ks.sak.kjerne.behandling

import io.mockk.*
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagBehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SnikeIKøenServiceTest {
    private val behandlingService: BehandlingService = mockk()
    private val loggService: LoggService = mockk()
    private val tilbakestillBehandlingService: TilbakestillBehandlingService = mockk()

    private val snikeIKøenService: SnikeIKøenService =
        SnikeIKøenService(
            behandlingService = behandlingService,
            loggService = loggService,
            tilbakestillBehandlingService = tilbakestillBehandlingService,
        )

    @Nested
    inner class SettAktivBehandlingPåMaskinellVent {
        @Test
        fun `skal kaste exception hvis behandling ikke er aktiv`() {
            // Arrange
            val behandling = lagBehandling(aktiv = false)

            every { behandlingService.hentBehandling(1L) }.returns(behandling)

            // Act & assert
            val exception =
                assertThrows<IllegalStateException> {
                    snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                        1L,
                        SettPåMaskinellVentÅrsak.SATSENDRING,
                    )
                }
            assertThat(exception.message).isEqualTo("Behandling=1 er ikke aktiv")
        }

        @Test
        fun `skal kaste exception hvis behandlingsstatusen ikke er UTREDES og behandlingen ikke er på vent`() {
            // Arrange
            val behandling = lagBehandling(status = BehandlingStatus.OPPRETTET)
            lagBehandlingStegTilstand(
                behandling,
                BehandlingSteg.REGISTRERE_PERSONGRUNNLAG,
                BehandlingStegStatus.KLAR,
            )

            every { behandlingService.hentBehandling(1L) }.returns(behandling)

            // Act & assert
            val exception =
                assertThrows<IllegalStateException> {
                    snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                        1L,
                        SettPåMaskinellVentÅrsak.SATSENDRING,
                    )
                }
            assertThat(exception.message).isEqualTo("Behandling=1 kan ikke settes på maskinell vent då status=OPPRETTET")
        }

        @Test
        fun `skal sette behandling på maskinell vent når BehandlingStatus er UTREDES og behandling ikke er på vent`() {
            // Arrange
            val behandling = lagBehandling(status = BehandlingStatus.UTREDES)
            lagBehandlingStegTilstand(
                behandling,
                BehandlingSteg.REGISTRERE_SØKNAD,
                BehandlingStegStatus.KLAR,
            )

            val årsak = SettPåMaskinellVentÅrsak.SATSENDRING

            every { behandlingService.hentBehandling(1L) }.returns(behandling)
            every { behandlingService.oppdaterBehandling(behandling) }.returns(behandling)
            every { loggService.opprettSettPåMaskinellVent(behandling, årsak.beskrivelse) } just runs

            // Act
            snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                1L,
                årsak,
            )

            // Assert
            assertThat(behandling.status).isEqualTo(BehandlingStatus.SATT_PÅ_MASKINELL_VENT)
            assertThat(behandling.aktiv).isFalse()
            verify(exactly = 1) { behandlingService.oppdaterBehandling(behandling) }
            verify(exactly = 1) { loggService.opprettSettPåMaskinellVent(behandling, årsak.beskrivelse) }
        }

        @Test
        fun `skal sette behandling på maskinell vent når behandling er på vent men BehandlingStatus er UTREDES`() {
            // Arrange
            val behandling = lagBehandling(status = BehandlingStatus.UTREDES)
            lagBehandlingStegTilstand(
                behandling,
                BehandlingSteg.REGISTRERE_SØKNAD,
                BehandlingStegStatus.VENTER,
            )

            val årsak = SettPåMaskinellVentÅrsak.SATSENDRING

            every { behandlingService.hentBehandling(1L) }.returns(behandling)
            every { behandlingService.oppdaterBehandling(behandling) }.returns(behandling)
            every { loggService.opprettSettPåMaskinellVent(behandling, årsak.beskrivelse) } just runs

            // Act
            snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                1L,
                årsak,
            )

            // Assert
            assertThat(behandling.status).isEqualTo(BehandlingStatus.SATT_PÅ_MASKINELL_VENT)
            assertThat(behandling.aktiv).isFalse()
            verify(exactly = 1) { behandlingService.oppdaterBehandling(behandling) }
            verify(exactly = 1) { loggService.opprettSettPåMaskinellVent(behandling, årsak.beskrivelse) }
        }

        @Test
        fun `skal sette behandling på maskinell vent så lenge BehandlingStatus ikke er UTREDES men behandlingen er på vent`() {
            // Arrange
            val behandling = lagBehandling(status = BehandlingStatus.FATTER_VEDTAK)
            behandling.behandlingStegTilstand.clear()
            lagBehandlingStegTilstand(
                behandling,
                BehandlingSteg.AVSLUTT_BEHANDLING,
                BehandlingStegStatus.VENTER,
            )

            val årsak = SettPåMaskinellVentÅrsak.SATSENDRING

            every { behandlingService.hentBehandling(1L) }.returns(behandling)
            every { behandlingService.oppdaterBehandling(behandling) }.returns(behandling)
            every { loggService.opprettSettPåMaskinellVent(behandling, årsak.beskrivelse) } just runs

            // Act
            snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                1L,
                årsak,
            )

            // Assert
            assertThat(behandling.status).isEqualTo(BehandlingStatus.SATT_PÅ_MASKINELL_VENT)
            assertThat(behandling.aktiv).isFalse()
            verify(exactly = 1) { behandlingService.oppdaterBehandling(behandling) }
            verify(exactly = 1) { loggService.opprettSettPåMaskinellVent(behandling, årsak.beskrivelse) }
        }

    }

    @Nested
    inner class ReaktiverBehandlingPåMaskinellVent {

        @Test
        fun `asdf`() {

            // Arrange
            val behandling = lagBehandling()

            // Act
            val erReaktivert = snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling)

            // Assert
            assertThat(erReaktivert).isTrue()

        }

    }
}
