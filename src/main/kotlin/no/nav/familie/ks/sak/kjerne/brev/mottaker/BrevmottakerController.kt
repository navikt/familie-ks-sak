package no.nav.familie.ks.sak.kjerne.brev.mottaker

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.BrevmottakerDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/brevmottaker")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BrevmottakerController(
    private val tilgangService: TilgangService,
    private val brevmottakerService: BrevmottakerService,
    private val behandlingService: BehandlingService,
) {
    @PostMapping(path = ["{behandlingId}"], produces = [APPLICATION_JSON_VALUE], consumes = [APPLICATION_JSON_VALUE])
    fun leggTilBrevmottaker(
        @PathVariable behandlingId: Long,
        @RequestBody brevmottaker: BrevmottakerDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.UPDATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "legge til brevmottaker",
        )
        validerKanRedigereBehandling(behandlingId)
        brevmottakerService.leggTilBrevmottaker(brevmottaker, behandlingId)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["{behandlingId}/{mottakerId}"])
    fun fjernBrevmottaker(
        @PathVariable behandlingId: Long,
        @PathVariable mottakerId: Long,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.DELETE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "fjerne brevmottaker",
        )
        validerKanRedigereBehandling(behandlingId)
        brevmottakerService.fjernBrevmottaker(id = mottakerId)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @GetMapping(path = ["{behandlingId}"], produces = [APPLICATION_JSON_VALUE])
    fun hentBrevmottakere(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<List<BrevmottakerDto>>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.ACCESS,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente brevmottakere",
        )
        return ResponseEntity.ok(Ressurs.success(brevmottakerService.hentBrevmottakere(behandlingId = behandlingId)))
    }

    private fun validerKanRedigereBehandling(behandlingId: Long) {
        behandlingService.hentBehandling(behandlingId).run {
            if (status.erLåstMenIkkeAvsluttet() || erAvsluttet()) {
                throw FunksjonellFeil("Behandlingen er låst for videre redigering ($status)")
            }
        }
    }
}
