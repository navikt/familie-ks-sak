package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.KompetanseDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class KompetanseController(
    private val kompetanseService: KompetanseService,
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
) {
    // Denne API-en brukes for å oppdatere kompetanse
    // Kompetanse oppretter automatisk etter vilkårsvurdering steg når vilkår er vurdert etter EØS forordningen
    @PutMapping(path = ["{behandlingId}/kompetanse"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterKompetanse(
        @PathVariable behandlingId: Long,
        @RequestBody kompentanseDto: KompetanseDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.UPDATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "oppdater kompetanse",
        )
        kompetanseService.oppdaterKompetanse(BehandlingId(behandlingId), kompentanseDto)
        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId)))
    }

    @DeleteMapping(
        path = ["behandlinger/{behandlingId}/kompetanse/{kompetanseId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun slettKompetanse(
        @PathVariable behandlingId: Long,
        @PathVariable kompetanseId: Long,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.DELETE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "slette kompetanse",
        )

        kompetanseService.slettKompetanse(kompetanseId)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId)))
    }
}
