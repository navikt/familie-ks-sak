package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.brev.BrevService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BrevControllerEnhetTest {
    private val brevService = mockk<BrevService>()
    private val tilgangService = mockk<TilgangService>()
    private val fagsakService = mockk<FagsakService>()

    private val brevController =
        BrevController(
            brevService = brevService,
            genererBrevService = mockk(),
            tilgangService = tilgangService,
            behandlingService = mockk<BehandlingService>(),
            vedtakService = mockk(),
            fagsakService = fagsakService,
        )

    @Nested
    inner class SendBrevPåFagsak {
        private val fagsak = lagFagsak(id = 1L)
        private val mottakerIdent = randomFnr()
        private val manueltBrevDto = ManueltBrevDto(brevmal = Brevmal.INFORMASJONSBREV_DELT_BOSTED, mottakerIdent = mottakerIdent)

        @BeforeEach
        fun setUp() {
            justRun { tilgangService.validerTilgangTilHandlingOgFagsak(any(), any(), any(), any()) }
            justRun { tilgangService.validerTilgangTilHandlingOgPersoner(any(), any(), any(), any()) }
            every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
            every { fagsakService.hentMinimalFagsak(fagsakId = fagsak.id) } returns mockk()
            every { brevService.leggTilEnhet(fagsak.id, manueltBrevDto) } returns manueltBrevDto
            justRun { brevService.sendBrev(fagsak = fagsak, manueltBrevDto = manueltBrevDto) }
        }

        @Test
        fun `skal validere tilgang til fagsaken og mottakeren før brevet sendes`() {
            // Act
            brevController.sendBrevPåFagsak(fagsakId = fagsak.id, manueltBrevDto = manueltBrevDto)

            // Assert
            verifyOrder {
                tilgangService.validerTilgangTilHandlingOgFagsak(
                    fagsakId = fagsak.id,
                    event = AuditLoggerEvent.UPDATE,
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    handling = "sende brev",
                )
                tilgangService.validerTilgangTilHandlingOgPersoner(
                    personIdenter = listOf(mottakerIdent),
                    event = AuditLoggerEvent.UPDATE,
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    handling = "sende brev",
                )
                brevService.sendBrev(fagsak = fagsak, manueltBrevDto = manueltBrevDto)
            }
        }

        @Test
        fun `skal ikke sende brev når saksbehandler mangler tilgang til fagsaken`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilHandlingOgFagsak(fagsak.id, AuditLoggerEvent.UPDATE, BehandlerRolle.SAKSBEHANDLER, "sende brev")
            } throws RolleTilgangskontrollFeil("Mangler tilgang til fagsaken")

            // Act & Assert
            assertThrows<RolleTilgangskontrollFeil> {
                brevController.sendBrevPåFagsak(fagsakId = fagsak.id, manueltBrevDto = manueltBrevDto)
            }

            verify(exactly = 0) { brevService.sendBrev(any(), any(), any()) }
            verify(exactly = 0) { fagsakService.hentMinimalFagsak(any()) }
        }

        @Test
        fun `skal ikke sende brev når saksbehandler mangler tilgang til mottakeren`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilHandlingOgPersoner(listOf(mottakerIdent), AuditLoggerEvent.UPDATE, BehandlerRolle.SAKSBEHANDLER, "sende brev")
            } throws RolleTilgangskontrollFeil("Mangler tilgang til mottakeren")

            // Act & Assert
            assertThrows<RolleTilgangskontrollFeil> {
                brevController.sendBrevPåFagsak(fagsakId = fagsak.id, manueltBrevDto = manueltBrevDto)
            }

            verify(exactly = 0) { brevService.sendBrev(any(), any(), any()) }
        }
    }
}
