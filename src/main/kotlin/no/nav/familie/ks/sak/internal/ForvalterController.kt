package no.nav.familie.ks.sak.internal

import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/forvalter")
@ProtectedWithClaims(issuer = "azuread")
class ForvalterController(
    private val testVerktøyService: TestVerktøyService,
    private val tilgangService: TilgangService,
    private val behandlingRepository: BehandlingRepository
) {
    @GetMapping(path = ["/behandling/{behandlingId}/begrunnelsetest"])
    fun hentBegrunnelsetestPåBehandling(
        @PathVariable behandlingId: Long,
    ): String {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.ACCESS,
            handling = "hente data til test",
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
        )

        return testVerktøyService.hentBrevTest(behandlingId)
            .replace("\n", System.lineSeparator())
    }

    @PostMapping(path = ["/revurder-fremtidig-opphor-behandlinger"])
    fun revurderFremtidigOpphørBehandlinger(
    ): ResponseEntity<String> {
        // Gjør et kall mot DB
        // Hent alle behandlinger for fremtidig opphør etter Aug 2024
        // Send brev for hver behandling

        val behandlingerFremtidigOpphørAugust2024 = behandlingRepository.finnBehandlingerFremtidigOpphørEtterAugust2024()

        return ResponseEntity.ok().body("ok")
    }
}
