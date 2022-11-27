package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.EndretUtbetalingAndelDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.TilbakestillBehandlingService
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/endretutbetalingandel")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class EndretUtbetalingAndelController(
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService
) {

    @PutMapping(path = ["/{behandlingId}/{endretUtbetalingAndelId}"])
    fun oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        @PathVariable behandlingId: Long,
        @PathVariable endretUtbetalingAndelId: Long,
        @RequestBody endretUtbetalingAndelDto: EndretUtbetalingAndelDto
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.UPDATE,
            handling = "Oppdater endretutbetalingandel",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)

        endretUtbetalingAndelService.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
            behandling,
            endretUtbetalingAndelId,
            endretUtbetalingAndelDto
        )

        tilbakestillBehandlingService
            .tilbakestillBehandlingTilBehandlingsresultat(behandlingId = behandling.id)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandling.id)))
    }

    @DeleteMapping(path = ["/{behandlingId}/{endretUtbetalingAndelId}"])
    fun fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        @PathVariable behandlingId: Long,
        @PathVariable endretUtbetalingAndelId: Long
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.UPDATE,
            handling = "Fjern endretutbetalingandel",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)

        endretUtbetalingAndelService.fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
            behandling,
            endretUtbetalingAndelId
        )

        tilbakestillBehandlingService
            .tilbakestillBehandlingTilBehandlingsresultat(behandlingId = behandling.id)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["/{behandlingId}"])
    fun lagreEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        @PathVariable behandlingId: Long
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.UPDATE,
            handling = "Oppdater endretutbetalingandel",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)

        endretUtbetalingAndelService.opprettTomEndretUtbetalingAndel(behandling)

        tilbakestillBehandlingService
            .tilbakestillBehandlingTilBehandlingsresultat(behandlingId = behandling.id)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandling.id)))
    }
}
