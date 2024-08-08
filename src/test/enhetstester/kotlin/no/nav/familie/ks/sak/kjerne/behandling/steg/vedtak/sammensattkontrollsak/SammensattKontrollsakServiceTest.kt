package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.OppdaterSammensattKontrollsakDto
import no.nav.familie.ks.sak.api.dto.OpprettSammensattKontrollsakDto
import no.nav.familie.ks.sak.api.dto.SlettSammensattKontrollsakDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SammensattKontrollsakServiceTest {
    private val mockedSammensattKontrollsakRepository: SammensattKontrollsakRepository = mockk()
    private val mockedLoggService: LoggService = mockk()

    private val sammensattKontrollsakService: SammensattKontrollsakService =
        SammensattKontrollsakService(
            sammensattKontrollsakRepository = mockedSammensattKontrollsakRepository,
            loggService = mockedLoggService,
        )

    @Nested
    inner class FinnSammensattKontrollsakTest {
        @Test
        fun `skal finne sammensatt kontrollsak når det finnes innslag i databasen`() {
            // Arrange
            val lagretSammensattKontrollsak =
                SammensattKontrollsak(
                    id = 0L,
                    behandlingId = 123L,
                    fritekst = "blabla",
                )

            every {
                mockedSammensattKontrollsakRepository.finnSammensattKontrollsak(
                    sammensattKontrollsakId = lagretSammensattKontrollsak.id,
                )
            } returns lagretSammensattKontrollsak

            // Act
            val funnetSammensattKontrollsak =
                sammensattKontrollsakService.finnSammensattKontrollsak(
                    sammensattKontrollsakId = lagretSammensattKontrollsak.id,
                )

            // Assert
            assertThat(funnetSammensattKontrollsak).isNotNull()
            assertThat(funnetSammensattKontrollsak?.id).isEqualTo(lagretSammensattKontrollsak.id)
            assertThat(funnetSammensattKontrollsak?.behandlingId).isEqualTo(lagretSammensattKontrollsak.behandlingId)
            assertThat(funnetSammensattKontrollsak?.fritekst).isEqualTo(lagretSammensattKontrollsak.fritekst)
        }

        @Test
        fun `skal ikke finne sammensatt kontrollsak når det ikke finnes et innslag i databasen`() {
            // Arrange
            every {
                mockedSammensattKontrollsakRepository.finnSammensattKontrollsak(
                    sammensattKontrollsakId = 0L,
                )
            } returns null

            // Act
            val funnetSammensattKontrollsak =
                sammensattKontrollsakService.finnSammensattKontrollsak(
                    sammensattKontrollsakId = 0L,
                )

            // Assert
            assertThat(funnetSammensattKontrollsak).isNull()
        }
    }

    @Nested
    inner class FinnSammensattKontrollsakForBehandlingTest {
        @Test
        fun `skal finne sammensatt kontrollsak når det finnes et innslag i databasen`() {
            // Arrange
            val lagretSammensattKontrollsak =
                SammensattKontrollsak(
                    id = 0L,
                    behandlingId = 123L,
                    fritekst = "blabla",
                )

            every {
                mockedSammensattKontrollsakRepository.finnSammensattKontrollsakForBehandling(
                    behandlingId = 0L,
                )
            } returns lagretSammensattKontrollsak

            // Act
            val funnetSammensattKontrollsak =
                sammensattKontrollsakService.finnSammensattKontrollsakForBehandling(
                    behandlingId = 0L,
                )

            // Assert
            assertThat(funnetSammensattKontrollsak).isNotNull()
            assertThat(funnetSammensattKontrollsak?.id).isEqualTo(lagretSammensattKontrollsak.id)
            assertThat(funnetSammensattKontrollsak?.behandlingId).isEqualTo(lagretSammensattKontrollsak.behandlingId)
            assertThat(funnetSammensattKontrollsak?.fritekst).isEqualTo(lagretSammensattKontrollsak.fritekst)
        }

        @Test
        fun `skal ikke finne sammensatt kontrollsak når det ikke finnes et innslag i databasen`() {
            // Arrange
            every {
                mockedSammensattKontrollsakRepository.finnSammensattKontrollsakForBehandling(
                    behandlingId = 0L,
                )
            } returns null

            // Act
            val funnetSammensattKontrollsak =
                sammensattKontrollsakService.finnSammensattKontrollsakForBehandling(
                    behandlingId = 0L,
                )

            // Assert
            assertThat(funnetSammensattKontrollsak).isNull()
        }
    }

    @Nested
    inner class OpprettSammensattKontrollsakTest {
        @Test
        fun `skal opprette sammensatt kontrollsak`() {
            // Arrange
            val opprettSammensattKontrollsakDto =
                OpprettSammensattKontrollsakDto(
                    behandlingId = 123L,
                    fritekst = "blabla",
                )

            val mockedSammensattKontrollsak =
                SammensattKontrollsak(
                    id = 1L,
                    behandlingId = 123L,
                    fritekst = "blabla",
                )

            every {
                mockedSammensattKontrollsakRepository.save(
                    any(),
                )
            } returns mockedSammensattKontrollsak

            every {
                mockedLoggService.opprettSammensattKontrollsakOpprettetLogg(
                    behandlingId = any(),
                )
            } just runs

            // Act
            val opprettetSammensattKontrollsak =
                sammensattKontrollsakService.opprettSammensattKontrollsak(
                    opprettSammensattKontrollsakDto = opprettSammensattKontrollsakDto,
                )

            // Assert
            assertThat(opprettetSammensattKontrollsak.id).isEqualTo(mockedSammensattKontrollsak.id)
            assertThat(opprettetSammensattKontrollsak.behandlingId).isEqualTo(mockedSammensattKontrollsak.behandlingId)
            assertThat(opprettetSammensattKontrollsak.fritekst).isEqualTo(mockedSammensattKontrollsak.fritekst)
            verify(exactly = 1) {
                mockedSammensattKontrollsakRepository.save(
                    any(SammensattKontrollsak::class),
                )
            }
            verify(exactly = 1) {
                mockedLoggService.opprettSammensattKontrollsakOpprettetLogg(
                    behandlingId = opprettetSammensattKontrollsak.behandlingId,
                )
            }
        }
    }

    @Nested
    inner class OppdaterSammensattKontrollsakTest {
        @Test
        fun `skal kaste exception om man prøver å oppdatere en ikke eksisterende sammensatt kontrollsak`() {
            // Arrange
            val oppdaterSammensattKontrollsakDto =
                OppdaterSammensattKontrollsakDto(
                    id = 0L,
                    fritekst = "blabla",
                )

            every {
                mockedSammensattKontrollsakRepository.finnSammensattKontrollsak(
                    sammensattKontrollsakId = any(),
                )
            } returns null

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    sammensattKontrollsakService.oppdaterSammensattKontrollsak(
                        oppdaterSammensattKontrollsakDto = oppdaterSammensattKontrollsakDto,
                    )
                }
            assertThat(exception.message).isEqualTo("Fant ingen eksisterende sammensatt kontrollsak for id=0")
        }

        @Test
        fun `skal oppdatere eksisterende sammensatt kontrollsak`() {
            // Arrange
            val sammensattKontrollsakSlot = slot<SammensattKontrollsak>()

            val oppdaterSammensattKontrollsakDto =
                OppdaterSammensattKontrollsakDto(
                    id = 0L,
                    fritekst = "blabla",
                )

            val sammensattKontrollsak =
                SammensattKontrollsak(
                    id = 0L,
                    behandlingId = 123L,
                    fritekst = "bla",
                )

            every {
                mockedSammensattKontrollsakRepository.finnSammensattKontrollsak(any())
            } returns sammensattKontrollsak

            every {
                mockedSammensattKontrollsakRepository.save(
                    capture(
                        sammensattKontrollsakSlot,
                    ),
                )
            } returns sammensattKontrollsak

            every {
                mockedLoggService.opprettSammensattKontrollsakOppdatertLogg(
                    any(),
                )
            } just runs

            // Act
            val oppdatertSammensattKontrollsak =
                sammensattKontrollsakService.oppdaterSammensattKontrollsak(
                    oppdaterSammensattKontrollsakDto = oppdaterSammensattKontrollsakDto,
                )

            // Assert
            assertThat(oppdatertSammensattKontrollsak.id).isEqualTo(sammensattKontrollsak.id)
            assertThat(oppdatertSammensattKontrollsak.behandlingId).isEqualTo(sammensattKontrollsak.behandlingId)
            assertThat(oppdatertSammensattKontrollsak.fritekst).isEqualTo("blabla")
            assertThat(sammensattKontrollsakSlot.captured.id).isEqualTo(sammensattKontrollsak.id)
            assertThat(sammensattKontrollsakSlot.captured.behandlingId).isEqualTo(sammensattKontrollsak.behandlingId)
            assertThat(sammensattKontrollsakSlot.captured.fritekst).isEqualTo("blabla")
            verify(exactly = 1) {
                mockedSammensattKontrollsakRepository.save(
                    oppdatertSammensattKontrollsak,
                )
            }
            verify(exactly = 1) {
                mockedLoggService.opprettSammensattKontrollsakOppdatertLogg(
                    behandlingId = oppdatertSammensattKontrollsak.behandlingId,
                )
            }
        }
    }

    @Nested
    inner class SlettSammensattKontrollsakTest {
        @Test
        fun `skal kaste exception om man prøver å slette en sammensatt kontrollsak som ikke finnes`() {
            // Arrange
            val slettSammensattKontrollsakDto =
                SlettSammensattKontrollsakDto(
                    id = 0L,
                )

            every {
                mockedSammensattKontrollsakRepository.finnSammensattKontrollsak(
                    sammensattKontrollsakId = slettSammensattKontrollsakDto.id,
                )
            } returns null

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    sammensattKontrollsakService.slettSammensattKontrollsak(
                        slettSammensattKontrollsakDto = slettSammensattKontrollsakDto,
                    )
                }
            assertThat(exception.message).isEqualTo("Fant ingen eksisterende sammensatt kontrollsak for id=${slettSammensattKontrollsakDto.id}")
        }

        @Test
        fun `skal slette sammensatt kontrollsak`() {
            // Arrange
            val slettSammensattKontrollsakDto =
                SlettSammensattKontrollsakDto(
                    id = 0L,
                )

            val sammensattKontrollsak =
                SammensattKontrollsak(
                    id = 0L,
                    behandlingId = 123L,
                    fritekst = "blabla",
                )

            every {
                mockedSammensattKontrollsakRepository.finnSammensattKontrollsak(
                    sammensattKontrollsakId = slettSammensattKontrollsakDto.id,
                )
            } returns sammensattKontrollsak

            every {
                mockedSammensattKontrollsakRepository.deleteById(
                    any(),
                )
            } just runs

            every {
                mockedLoggService.opprettSammensattKontrollsakSlettetLogg(
                    any(),
                )
            } just runs

            // Act
            sammensattKontrollsakService.slettSammensattKontrollsak(
                slettSammensattKontrollsakDto = slettSammensattKontrollsakDto,
            )

            // Assert
            verify(exactly = 1) {
                mockedSammensattKontrollsakRepository.deleteById(
                    sammensattKontrollsak.id,
                )
            }
            verify(exactly = 1) {
                mockedLoggService.opprettSammensattKontrollsakSlettetLogg(
                    behandlingId = sammensattKontrollsak.behandlingId,
                )
            }
        }
    }
}
