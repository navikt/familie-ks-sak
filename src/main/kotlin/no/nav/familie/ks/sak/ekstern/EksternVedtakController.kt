package no.nav.familie.ks.sak.ekstern

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/vedtak"],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@ProtectedWithClaims(issuer = "azuread")
class EksternVedtakController(
    private val tilgangService: TilgangService,
    private val eksternVedtakService: EksternVedtakService
) {

    @GetMapping("/{fagsakId}")
    @ProtectedWithClaims(issuer = "azuread")
    fun hentVedtak(@PathVariable fagsakId: Long): Ressurs<List<FagsystemVedtak>> {
        if (!SikkerhetContext.erMaskinTilMaskinToken()) {
            tilgangService.validerTilgangTilHandlingOgFagsak(
                fagsakId = fagsakId,
                handling = "Kan hente vedtak på fagsak=$fagsakId",
                event = AuditLoggerEvent.ACCESS,
                minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER
            )
        }

        return Ressurs.success(eksternVedtakService.hentVedtak(fagsakId))
    }
}
