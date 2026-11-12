package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.SpringProfile
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.TilbakestillBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.core.env.Environment

class ForvaltningControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val environment = mockk<Environment>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val tilbakestillBehandlingService = mockk<TilbakestillBehandlingService>()

    private val forvaltningController =
        ForvaltningController(
            fagsakLåsingService = mockk(),
            tilgangService = tilgangService,
            integrasjonKlient = mockk(),
            sakStatistikkService = mockk(),
            stønadsstatistikkService = mockk(),
            taskService = mockk(),
            konsistensavstemmingKjøreplanService = mockk(),
            personidentService = mockk(),
            vilkårsvurderingService = vilkårsvurderingService,
            environment = environment,
            ecbService = mockk(),
            behandlingRepository = behandlingRepository,
            testVerktøyService = mockk(),
            envService = mockk(),
            autovedtakService = mockk(),
            barnehagebarnService = mockk(),
            barnehagelisteVarslingService = mockk(),
            fagsakStatusScheduler = mockk(),
            tilbakestillBehandlingService = tilbakestillBehandlingService,
        )

    @Nested
    inner class FyllUtVilkårsvurdering {
        @BeforeEach
        fun setup() {
            every { environment.activeProfiles } returns arrayOf(SpringProfile.Preprod.navn)
            justRun { tilgangService.validerTilgangTilHandling(any(), any()) }
        }

        @Test
        fun `skal tilbakestille behandlingen til vilkårsvurdering etter at vilkårsvurderingen er fylt ut`() {
            // Arrange
            val behandling = lagBehandling(id = 1, status = BehandlingStatus.UTREDES)
            every { behandlingRepository.hentBehandling(behandling.id) } returns behandling
            justRun { vilkårsvurderingService.fyllUtVilkårsvurdering(behandling.id) }
            justRun { tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(behandling.id) }

            // Act
            forvaltningController.fyllUtVilkårsvurdering(behandling.id)

            // Assert
            verifyOrder {
                vilkårsvurderingService.fyllUtVilkårsvurdering(behandling.id)
                tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(behandling.id)
            }
        }

        @ParameterizedTest
        @EnumSource(value = BehandlingStatus::class, names = ["UTREDES"], mode = EnumSource.Mode.EXCLUDE)
        fun `skal ikke fylle ut eller tilbakestille når behandlingen ikke kan redigeres`(status: BehandlingStatus) {
            // Arrange
            val behandling = lagBehandling(id = 1, status = status)
            every { behandlingRepository.hentBehandling(behandling.id) } returns behandling

            // Act & Assert
            assertThrows<FunksjonellFeil> { forvaltningController.fyllUtVilkårsvurdering(behandling.id) }
            verify(exactly = 0) { vilkårsvurderingService.fyllUtVilkårsvurdering(any()) }
            verify(exactly = 0) { tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(any()) }
        }
    }
}
