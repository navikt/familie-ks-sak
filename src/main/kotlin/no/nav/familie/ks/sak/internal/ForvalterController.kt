package no.nav.familie.ks.sak.internal

import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvalter")
@ProtectedWithClaims(issuer = "azuread")
class ForvalterController(
    private val testVerktøyService: TestVerktøyService,
    private val tilgangService: TilgangService,
) {
    private val logger: Logger = LoggerFactory.getLogger(ForvalterController::class.java)

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
}
