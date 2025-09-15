package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.HttpStatus

class SammensattKontrollsakValidatorTest {
    private val featureToggleService: FeatureToggleService = mockk()
    private val tilgangService: TilgangService = mockk()
    private val sammensattKontrollsakService: SammensattKontrollsakService = mockk()
    private val behandlingService: BehandlingService = mockk()

    private val sammensattKontrollsakValidator: SammensattKontrollsakValidator =
        SammensattKontrollsakValidator(
            featureToggleService = featureToggleService,
            tilgangService = tilgangService,
            sammensattKontrollsakService = sammensattKontrollsakService,
            behandlingService = behandlingService,
        )

    @Nested
    inner class ValiderHentSammensattKontrollsakTilgangTest {
        @Test
        fun `skal kaste exception om toggel ikke er skrudd på`() {
            // Arrange
            every {
                featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)
            } returns false

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    sammensattKontrollsakValidator.validerHentSammensattKontrollsakTilgang()
                }
            assertThat(exception.melding).isEqualTo("Mangler tilgang for å hente sammensatt kontrollsak.")
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `skal kaste exception om man mangler egnet rolle`() {
            // Arrange
            every {
                featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)
            } returns true

            every {
                tilgangService.validerTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    any(),
                )
            } throws RolleTilgangskontrollFeil("Mangler tilgang", httpStatus = HttpStatus.FORBIDDEN)

            // Act & assert
            val exception =
                assertThrows<RolleTilgangskontrollFeil> {
                    sammensattKontrollsakValidator.validerHentSammensattKontrollsakTilgang()
                }
            assertThat(exception.melding).isEqualTo("Mangler tilgang")
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `skal ikke kaste feil om valideringen er godkjent`() {
            // Arrange
            every {
                featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)
            } returns true

            every {
                tilgangService.validerTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    any(),
                )
            } just runs

            // Act & assert
            assertDoesNotThrow {
                sammensattKontrollsakValidator.validerHentSammensattKontrollsakTilgang()
            }
        }
    }

    @Nested
    inner class ValiderOpprettSammensattKontrollsakTilgangTest {
        @Test
        fun `skal kaste exception om toggel ikke er skrudd på`() {
            // Arrange
            every {
                featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)
            } returns false

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    sammensattKontrollsakValidator.validerOpprettSammensattKontrollsakTilgang()
                }
            assertThat(exception.melding).isEqualTo("Mangler tilgang for å opprette sammensatt kontrollsak.")
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `skal kaste exception om man mangler egnet rolle`() {
            // Arrange
            every {
                featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)
            } returns true

            every {
                tilgangService.validerTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    any(),
                )
            } throws RolleTilgangskontrollFeil("Mangler tilgang", httpStatus = HttpStatus.FORBIDDEN)

            // Act & assert
            val exception =
                assertThrows<RolleTilgangskontrollFeil> {
                    sammensattKontrollsakValidator.validerOpprettSammensattKontrollsakTilgang()
                }
            assertThat(exception.melding).isEqualTo("Mangler tilgang")
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `skal ikke kaste feil om valideringen er godkjent`() {
            // Arrange
            every {
                featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)
            } returns true

            every {
                tilgangService.validerTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    any(),
                )
            } just runs

            // Act & assert
            assertDoesNotThrow {
                sammensattKontrollsakValidator.validerOpprettSammensattKontrollsakTilgang()
            }
        }
    }

    @Nested
    inner class ValiderOppdaterSammensattKontrollsakTilgangTest {
        @Test
        fun `skal kaste exception om toggel ikke er skrudd på`() {
            // Arrange
            every {
                featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)
            } returns false

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    sammensattKontrollsakValidator.validerOppdaterSammensattKontrollsakTilgang()
                }
            assertThat(exception.melding).isEqualTo("Mangler tilgang for å oppdatere sammensatt kontrollsak.")
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `skal kaste exception om man mangler egnet rolle`() {
            // Arrange
            every {
                featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)
            } returns true

            every {
                tilgangService.validerTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    any(),
                )
            } throws RolleTilgangskontrollFeil("Mangler tilgang", httpStatus = HttpStatus.FORBIDDEN)

            // Act & assert
            val exception =
                assertThrows<RolleTilgangskontrollFeil> {
                    sammensattKontrollsakValidator.validerOppdaterSammensattKontrollsakTilgang()
                }
            assertThat(exception.melding).isEqualTo("Mangler tilgang")
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `skal ikke kaste feil om valideringen er godkjent`() {
            // Arrange
            every {
                featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)
            } returns true

            every {
                tilgangService.validerTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    any(),
                )
            } just runs

            // Act & assert
            assertDoesNotThrow {
                sammensattKontrollsakValidator.validerOppdaterSammensattKontrollsakTilgang()
            }
        }
    }

    @Nested
    inner class ValiderSlettSammensattKontrollsakTilgangTest {
        @Test
        fun `skal kaste exception om toggel ikke er skrudd på`() {
            // Arrange
            every {
                featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)
            } returns false

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    sammensattKontrollsakValidator.validerSlettSammensattKontrollsakTilgang()
                }
            assertThat(exception.melding).isEqualTo("Mangler tilgang for å slette sammensatt kontrollsak.")
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `skal kaste exception om man mangler egnet rolle`() {
            // Arrange
            every {
                featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)
            } returns true

            every {
                tilgangService.validerTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    any(),
                )
            } throws RolleTilgangskontrollFeil("Mangler tilgang", httpStatus = HttpStatus.FORBIDDEN)

            // Act & assert
            val exception =
                assertThrows<RolleTilgangskontrollFeil> {
                    sammensattKontrollsakValidator.validerSlettSammensattKontrollsakTilgang()
                }
            assertThat(exception.melding).isEqualTo("Mangler tilgang")
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `skal ikke kaste feil om valideringen er godkjent`() {
            // Arrange
            every {
                featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)
            } returns true

            every {
                tilgangService.validerTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    any(),
                )
            } just runs

            // Act & assert
            assertDoesNotThrow {
                sammensattKontrollsakValidator.validerSlettSammensattKontrollsakTilgang()
            }
        }
    }

    @Nested
    inner class ValiderRedigerbarBehandlingForSammensattKontrollsakIdTest {
        @Test
        fun `skal kaste exception om ingen sammensatt kontrollsak blir funnet for id`() {
            // Arrange
            val sammensattKontrollsakId = 0L

            every {
                sammensattKontrollsakService.finnSammensattKontrollsak(
                    sammensattKontrollsakId = sammensattKontrollsakId,
                )
            } returns null

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    sammensattKontrollsakValidator.validerRedigerbarBehandlingForSammensattKontrollsakId(
                        sammensattKontrollsakId = sammensattKontrollsakId,
                    )
                }
            assertThat(exception.melding).isEqualTo("Fant ingen sammensatt kontrollsak for id=$sammensattKontrollsakId.")
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingStatus::class,
            names = ["OPPRETTET", "UTREDES", "SATT_PÅ_MASKINELL_VENT"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal kaste exception om behandlingen ikke er redigerbar`(
            behandlingStatus: BehandlingStatus,
        ) {
            // Arrange
            val behandling =
                lagBehandling(
                    status = behandlingStatus,
                )

            val sammensattKontrollsak =
                SammensattKontrollsak(
                    id = 0L,
                    behandlingId = behandling.id,
                    fritekst = "blabla",
                )

            every {
                sammensattKontrollsakService.finnSammensattKontrollsak(
                    sammensattKontrollsakId = sammensattKontrollsak.id,
                )
            } returns sammensattKontrollsak

            every {
                behandlingService.hentBehandling(
                    behandlingId = sammensattKontrollsak.behandlingId,
                )
            } returns behandling

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    sammensattKontrollsakValidator.validerRedigerbarBehandlingForSammensattKontrollsakId(
                        sammensattKontrollsakId = sammensattKontrollsak.id,
                    )
                }
            assertThat(exception.melding).isEqualTo("Behandlingen er låst for videre redigering ($behandlingStatus)")
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingStatus::class,
            names = ["FATTER_VEDTAK", "IVERKSETTER_VEDTAK", "AVSLUTTET"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal ikke kaste exception om behandlingen er redigerbar`(
            behandlingStatus: BehandlingStatus,
        ) {
            // Arrange
            val behandling =
                lagBehandling(
                    status = behandlingStatus,
                )

            val sammensattKontrollsak =
                SammensattKontrollsak(
                    id = 0L,
                    behandlingId = behandling.id,
                    fritekst = "",
                )

            every {
                sammensattKontrollsakService.finnSammensattKontrollsak(
                    sammensattKontrollsakId = sammensattKontrollsak.id,
                )
            } returns sammensattKontrollsak

            every {
                behandlingService.hentBehandling(behandlingId = sammensattKontrollsak.behandlingId)
            } returns behandling

            // Act & assert
            assertDoesNotThrow {
                sammensattKontrollsakValidator.validerRedigerbarBehandlingForSammensattKontrollsakId(
                    sammensattKontrollsakId = sammensattKontrollsak.id,
                )
            }
        }
    }

    @Nested
    inner class ValiderRedigerbarBehandlingForBehandlingIdTest {
        @ParameterizedTest
        @EnumSource(
            value = BehandlingStatus::class,
            names = ["OPPRETTET", "UTREDES", "SATT_PÅ_MASKINELL_VENT"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal kaste exception om behandlingen ikke er redigerbar`(
            behandlingStatus: BehandlingStatus,
        ) {
            // Arrange
            val behandling =
                lagBehandling(
                    status = behandlingStatus,
                )

            every {
                behandlingService.hentBehandling(123L)
            } returns behandling

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    sammensattKontrollsakValidator.validerRedigerbarBehandlingForBehandlingId(123L)
                }
            assertThat(exception.melding).isEqualTo("Behandlingen er låst for videre redigering ($behandlingStatus)")
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingStatus::class,
            names = ["FATTER_VEDTAK", "IVERKSETTER_VEDTAK", "AVSLUTTET"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal ikke kaste exception om behandlingen er redigerbar`(
            behandlingStatus: BehandlingStatus,
        ) {
            // Arrange
            val behandling =
                lagBehandling(
                    status = behandlingStatus,
                )

            every {
                behandlingService.hentBehandling(123L)
            } returns behandling

            // Act & assert
            assertDoesNotThrow {
                sammensattKontrollsakValidator.validerRedigerbarBehandlingForBehandlingId(123L)
            }
        }
    }
}
