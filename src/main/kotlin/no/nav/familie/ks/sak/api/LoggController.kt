package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.common.util.RessursUtils.badRequest
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.logg.domene.Logg
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/logg")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class LoggController(
    private val loggService: LoggService
) {

    @GetMapping(path = ["/{behandlingId}"])
    fun hentLoggForBehandling(
        @PathVariable
        behandlingId: Long
    ): ResponseEntity<Ressurs<List<Logg>>> {
        // TODO: Legg til når tilgangservice er på plass.
        // tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        return Result.runCatching { loggService.hentLoggForBehandling(behandlingId) }
            .fold(
                onSuccess = { ResponseEntity.ok(Ressurs.success(it)) },
                onFailure = {
                    badRequest("Henting av logg feilet", it)
                }
            )
    }
}
