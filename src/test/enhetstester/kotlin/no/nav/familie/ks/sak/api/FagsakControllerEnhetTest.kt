package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.FagsakRequestDto
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FagsakControllerEnhetTest {
    private val fagsakService = mockk<FagsakService>()
    private val tilgangService = mockk<TilgangService>()
    private val personidentService = mockk<PersonidentService>()

    private val fagsakController =
        FagsakController(
            fagsakLåsingService = mockk(),
            fagsakService = fagsakService,
            tilgangService = tilgangService,
            tilbakekrevingService = mockk(),
            personidentService = personidentService,
        )

    private val fødselsnummer = randomFnr()
    private val aktør = randomAktør(fødselsnummer)

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandlingOgPersoner(any(), any(), any(), any()) }
        every { personidentService.hentAktør(aktør.aktørId) } returns aktør
        every { fagsakService.hentEllerOpprettFagsak(any()) } returns mockk()
    }

    @Test
    fun `skal validere tilgang til personen bak aktørId når fagsak hentes eller opprettes med aktørId`() {
        // Act
        fagsakController.hentEllerOpprettFagsak(FagsakRequestDto(personIdent = null, aktørId = aktør.aktørId))

        // Assert
        verify(exactly = 1) {
            tilgangService.validerTilgangTilHandlingOgPersoner(
                personIdenter = listOf(fødselsnummer),
                event = AuditLoggerEvent.CREATE,
                minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                handling = "Opprett fagsak",
            )
        }
    }

    @Test
    fun `skal validere tilgang til personIdent uten oppslag når personIdent er satt`() {
        // Act
        fagsakController.hentEllerOpprettFagsak(FagsakRequestDto(personIdent = fødselsnummer))

        // Assert
        verify(exactly = 1) {
            tilgangService.validerTilgangTilHandlingOgPersoner(
                personIdenter = listOf(fødselsnummer),
                event = AuditLoggerEvent.CREATE,
                minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                handling = "Opprett fagsak",
            )
        }
        verify(exactly = 0) { personidentService.hentAktør(any()) }
    }

    @Test
    fun `skal ikke hente eller opprette fagsak når saksbehandler mangler tilgang til personen bak aktørId`() {
        // Arrange
        every {
            tilgangService.validerTilgangTilHandlingOgPersoner(listOf(fødselsnummer), AuditLoggerEvent.CREATE, BehandlerRolle.SAKSBEHANDLER, "Opprett fagsak")
        } throws RolleTilgangskontrollFeil("Mangler tilgang til personen")

        // Act & Assert
        assertThrows<RolleTilgangskontrollFeil> {
            fagsakController.hentEllerOpprettFagsak(FagsakRequestDto(personIdent = null, aktørId = aktør.aktørId))
        }

        verify(exactly = 0) { fagsakService.hentEllerOpprettFagsak(any()) }
    }
}
