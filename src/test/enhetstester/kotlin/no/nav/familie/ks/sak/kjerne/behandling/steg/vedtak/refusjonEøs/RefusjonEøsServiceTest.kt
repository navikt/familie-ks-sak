package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagRefusjonEøs
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RefusjonEøsServiceTest {
    private val refusjonEøsRepository = mockk<RefusjonEøsRepository>()
    private val loggService = mockk<LoggService>()

    private val refusjonEøsService: RefusjonEøsService =
        RefusjonEøsService(
            refusjonEøsRepository = refusjonEøsRepository,
            loggService = loggService,
        )

    @Nested
    inner class HarRefusjonEøsPåBehandlingTest {
        @Test
        fun `skal returnere false om refusjon eøs på behandling er tom`() {
            // Arrange
            val behandling = lagBehandling()

            every {
                refusjonEøsRepository.finnRefusjonEøsForBehandling(behandlingId = behandling.id)
            } returns emptyList()

            // Act
            val harRefusjonEøsPåBehandling = refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id)

            // Assert
            assertThat(harRefusjonEøsPåBehandling).isFalse()
        }

        @Test
        fun `skal returnere true om refusjon eøs på behandling ikke er tom`() {
            // Arrange
            val behandling = lagBehandling()

            every {
                refusjonEøsRepository.finnRefusjonEøsForBehandling(behandlingId = behandling.id)
            } returns listOf(lagRefusjonEøs())

            // Act
            val harRefusjonEøsPåBehandling = refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id)

            // Assert
            assertThat(harRefusjonEøsPåBehandling).isTrue()
        }
    }
}
