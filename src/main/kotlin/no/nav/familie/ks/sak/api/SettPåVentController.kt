package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.SettPåVentDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.settpåvent.SettPåVentService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sett-på-vent/")
@ProtectedWithClaims(issuer = "azuread")
class SettPåVentController(
    private val tilgangService: TilgangService,
    private val settPåVentService: SettPåVentService,
    private val behandlingService: BehandlingService
) {
    @PostMapping(path = ["{behandlingId}"])
    fun settBehandlingPåVent(
        @PathVariable behandlingId: Long,
        @RequestBody settPåVentDto: SettPåVentDto
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.UPDATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "sett behandling på vent"
        )
        settPåVentService.settBehandlingPåVent(behandlingId, settPåVentDto.frist, settPåVentDto.årsak)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @PutMapping(path = ["{behandlingId}"])
    fun oppdaterSettBehandlingPåVent(
        @PathVariable behandlingId: Long,
        @RequestBody settPåVentDto: SettPåVentDto
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.UPDATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "oppdater SettPåVent for behandling"
        )
        settPåVentService.oppdaterSettBehandlingPåVent(behandlingId, settPåVentDto.frist, settPåVentDto.årsak)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @PutMapping(path = ["{behandlingId}/fortsettbehandling"])
    fun gjenopptaBehandling(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.UPDATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "gjenopptar behandling"
        )
        settPåVentService.gjenopptaBehandling(behandlingId)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }
}
