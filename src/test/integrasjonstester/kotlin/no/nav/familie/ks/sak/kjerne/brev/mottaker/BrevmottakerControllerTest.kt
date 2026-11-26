package no.nav.familie.ks.sak.kjerne.brev.mottaker

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.BrevmottakerDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagBrevmottakerDto
import no.nav.familie.ks.sak.datagenerator.lagBrevmottakerDb
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BrevmottakerControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var brevmottakerService: BrevmottakerService

    private val tilgangService = mockk<TilgangService>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private lateinit var controller: BrevmottakerController

    @BeforeEach
    fun setUp() {
        clearMocks(tilgangService, behandlingService)
        opprettSøkerFagsakOgBehandling()
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { behandlingService.lagBehandlingRespons(behandling.id) } returns mockk()
        controller = BrevmottakerController(tilgangService, brevmottakerService, behandlingService)
    }

    @Nested
    inner class LeggTilBrevmottaker {
        @Test
        fun `skal lagre flere brevmottakere på behandlingen`() {
            // Arrange
            val dto = lagBrevmottakerDto(type = MottakerType.FULLMEKTIG)

            // Act
            controller.leggTilBrevmottaker(behandling.id, dto)
            controller.leggTilBrevmottaker(behandling.id, dto.copy(type = MottakerType.VERGE))

            // Assert
            assertThat(brevmottakerService.hentBrevmottakere(behandling.id).map { it.type })
                .containsExactlyInAnyOrder(MottakerType.FULLMEKTIG, MottakerType.VERGE)
        }

        @Test
        fun `skal validere tilgang og lagre brevmottaker med ny id og trimmet postadresse`() {
            // Arrange
            val dto = lagBrevmottakerDto(id = 123, postnummer = " 0661 ", poststed = " Oslo ")

            // Act
            controller.leggTilBrevmottaker(behandling.id, dto)

            // Assert
            val lagret = brevmottakerService.hentBrevmottakere(behandling.id).single()
            assertThat(lagret.id).isPositive()
            assertThat(BrevmottakerDto.fra(lagret)).isEqualTo(dto.copy(id = lagret.id, postnummer = "0661", poststed = "Oslo"))
            verify(exactly = 1) {
                tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
                    behandlingId = behandling.id,
                    event = AuditLoggerEvent.UPDATE,
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    handling = "legge til brevmottaker",
                )
            }
            verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        }

        @Test
        fun `skal ikke lagre når tilgangskontrollen feiler`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(any(), any(), any(), any())
            } throws RolleTilgangskontrollFeil("Ingen tilgang")

            // Act & Assert
            assertThatThrownBy { controller.leggTilBrevmottaker(behandling.id, lagBrevmottakerDto()) }
                .isInstanceOf(RolleTilgangskontrollFeil::class.java)
            assertThat(brevmottakerService.hentBrevmottakere(behandling.id)).isEmpty()
        }

        @Test
        fun `skal ikke lagre på en låst behandling`() {
            // Arrange
            every { behandlingService.hentBehandling(behandling.id) } returns
                lagBehandling(id = behandling.id, status = BehandlingStatus.AVSLUTTET)

            // Act & Assert
            assertThatThrownBy { controller.leggTilBrevmottaker(behandling.id, lagBrevmottakerDto()) }
                .isInstanceOf(FunksjonellFeil::class.java)
            assertThat(brevmottakerService.hentBrevmottakere(behandling.id)).isEmpty()
        }
    }

    @Nested
    inner class SlettBrevmottaker {
        @Test
        fun `skal validere tilgang og slette brevmottaker fra behandlingen`() {
            // Arrange
            val brevmottaker = brevmottakerService.opprettBrevmottaker(lagBrevmottakerDb(behandlingId = behandling.id))
            val gjenværendeBrevmottaker =
                brevmottakerService.opprettBrevmottaker(lagBrevmottakerDb(behandlingId = behandling.id, type = MottakerType.VERGE))

            // Act
            controller.slettBrevmottaker(behandlingId = behandling.id, mottakerId = brevmottaker.id)

            // Assert
            assertThat(brevmottakerService.hentBrevmottakere(behandling.id).map { it.id }).containsExactly(gjenværendeBrevmottaker.id)
            verify(exactly = 1) {
                tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
                    behandlingId = behandling.id,
                    event = AuditLoggerEvent.DELETE,
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    handling = "fjerne brevmottaker",
                )
            }
            verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        }

        @Test
        fun `skal ikke slette brevmottaker som tilhører en annen behandling`() {
            // Arrange
            val opprinneligBehandlingId = behandling.id
            val brevmottaker = brevmottakerService.opprettBrevmottaker(lagBrevmottakerDb(behandlingId = opprinneligBehandlingId))
            opprettSøkerFagsakOgBehandling()
            every { behandlingService.hentBehandling(behandling.id) } returns behandling

            // Act & Assert
            assertThatThrownBy { controller.slettBrevmottaker(behandlingId = behandling.id, mottakerId = brevmottaker.id) }
                .isInstanceOf(Feil::class.java)
                .hasMessage("Finner ikke brevmottaker med id=${brevmottaker.id} på behandling med id=${behandling.id}")
            assertThat(brevmottakerService.hentBrevmottakere(opprinneligBehandlingId).map { it.id }).containsExactly(brevmottaker.id)
        }

        @Test
        fun `skal ikke slette når tilgangskontrollen feiler`() {
            // Arrange
            val brevmottaker = brevmottakerService.opprettBrevmottaker(lagBrevmottakerDb(behandlingId = behandling.id))
            every {
                tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(any(), any(), any(), any())
            } throws RolleTilgangskontrollFeil("Ingen tilgang")

            // Act & Assert
            assertThatThrownBy { controller.slettBrevmottaker(behandlingId = behandling.id, mottakerId = brevmottaker.id) }
                .isInstanceOf(RolleTilgangskontrollFeil::class.java)
            assertThat(brevmottakerService.hentBrevmottakere(behandling.id).map { it.id }).containsExactly(brevmottaker.id)
        }

        @Test
        fun `skal ikke slette på en låst behandling`() {
            // Arrange
            val brevmottaker = brevmottakerService.opprettBrevmottaker(lagBrevmottakerDb(behandlingId = behandling.id))
            every { behandlingService.hentBehandling(behandling.id) } returns
                lagBehandling(id = behandling.id, status = BehandlingStatus.AVSLUTTET)

            // Act & Assert
            assertThatThrownBy { controller.slettBrevmottaker(behandlingId = behandling.id, mottakerId = brevmottaker.id) }
                .isInstanceOf(FunksjonellFeil::class.java)
            assertThat(brevmottakerService.hentBrevmottakere(behandling.id).map { it.id }).containsExactly(brevmottaker.id)
        }
    }

    @Nested
    inner class HentBrevmottakere {
        @Test
        fun `skal validere tilgang og bare hente mottakere på angitt behandling`() {
            // Arrange
            val opprinneligBehandlingId = behandling.id
            val brevmottaker = brevmottakerService.opprettBrevmottaker(lagBrevmottakerDb(behandlingId = opprinneligBehandlingId))
            opprettSøkerFagsakOgBehandling()
            brevmottakerService.opprettBrevmottaker(lagBrevmottakerDb(behandlingId = behandling.id))

            // Act
            val respons = controller.hentBrevmottakere(opprinneligBehandlingId)

            // Assert
            assertThat(respons.body?.data).containsExactly(BrevmottakerDto.fra(brevmottaker))
            verify(exactly = 1) {
                tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
                    behandlingId = opprinneligBehandlingId,
                    event = AuditLoggerEvent.ACCESS,
                    minimumBehandlerRolle = BehandlerRolle.VEILEDER,
                    handling = "hente brevmottakere",
                )
            }
        }

        @Test
        fun `skal returnere tom liste når behandlingen ikke har brevmottakere`() {
            // Arrange
            val behandlingId = behandling.id

            // Act
            val respons = controller.hentBrevmottakere(behandlingId)

            // Assert
            assertThat(respons.body?.data).isEmpty()
        }
    }
}
