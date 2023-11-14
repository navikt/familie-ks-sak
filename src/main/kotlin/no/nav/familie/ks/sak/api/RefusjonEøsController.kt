package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.RefusjonEøsDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøsService
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/refusjon-eøs")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class RefusjonEøsController(
    private val tilgangService: TilgangService,
    private val refusjonEøsService: RefusjonEøsService,
    private val behandlingService: BehandlingService,
) {
    @PostMapping(
        path = ["behandlinger/{behandlingId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun leggTilRefusjonEøsPeriode(
        @PathVariable behandlingId: Long,
        @RequestBody refusjonEøs: RefusjonEøsDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "legg til periode med refusjon EØS",
        )

        refusjonEøsService.leggTilRefusjonEøsPeriode(
            refusjonEøs = refusjonEøs,
            behandlingId = behandlingId,
        )

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @PutMapping(
        path = ["behandlinger/{behandlingId}/perioder/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun oppdaterRefusjonEøsPeriode(
        @PathVariable behandlingId: Long,
        @PathVariable id: Long,
        @RequestBody refusjonEøs: RefusjonEøsDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "oppdater periode med refusjon EØS",
        )

        refusjonEøsService.oppdaterRefusjonEøsPeriode(refusjonEøs = refusjonEøs, id = id)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["behandlinger/{behandlingId}/perioder/{id}"])
    fun fjernRefusjonEøsPeriode(
        @PathVariable behandlingId: Long,
        @PathVariable id: Long,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "fjerner periode med refusjon EØS",
        )

        refusjonEøsService.fjernRefusjonEøsPeriode(id = id, behandlingId = behandlingId)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @GetMapping(path = ["behandlinger/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentRefusjonEøsPerioder(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<List<RefusjonEøsDto>>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente refusjon EØS for behandling",
        )
        return ResponseEntity.ok(Ressurs.success(refusjonEøsService.hentRefusjonEøsPerioder(behandlingId = behandlingId)))
    }
}
