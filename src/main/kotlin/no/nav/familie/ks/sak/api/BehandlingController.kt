package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingPåVentDto
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.EndreBehandlendeEnhetDto
import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.OpprettBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.SettBehandlingPåVentService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(
    private val opprettBehandlingService: OpprettBehandlingService,
    private val behandlingService: BehandlingService,
    private val tilgangService: TilgangService,
    private val settBehandlingPåVentService: SettBehandlingPåVentService
) {

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettBehandling(@RequestBody opprettBehandlingDto: OpprettBehandlingDto): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgPersoner(
            personIdenter = listOf(opprettBehandlingDto.søkersIdent),
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.CREATE,
            handling = "Opprett behandling"
        )
        val behandling = opprettBehandlingService.opprettBehandling(opprettBehandlingDto)
        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandling.id)))
    }

    @GetMapping(path = ["/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentBehandling(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            event = AuditLoggerEvent.ACCESS,
            handling = "Hent behandling"
        )

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @PutMapping(path = ["/{behandlingId}/enhet"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun endreBehandlendeEnhet(
        @PathVariable behandlingId: Long,
        @RequestBody endreBehandlendeEnhet: EndreBehandlendeEnhetDto
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.UPDATE,
            handling = "Endre behandlende enhet"
        )

        if (endreBehandlendeEnhet.begrunnelse.isBlank()) {
            throw FunksjonellFeil(
                melding = "Begrunnelse kan ikke være tom",
                frontendFeilmelding = "Du må skrive en begrunnelse for endring av enhet"
            )
        }

        behandlingService.oppdaterBehandlendeEnhet(behandlingId, endreBehandlendeEnhet)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @PostMapping("/{behandlingId}/sett-på-vent")
    fun settBehandlingPåVent(
        @PathVariable behandlingId: Long,
        settBehandlingPåVentDto: BehandlingPåVentDto
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "sette behandling på vent"
        )
        settBehandlingPåVentService.settBehandlingPåVent(
            behandlingId,
            settBehandlingPåVentDto.frist,
            settBehandlingPåVentDto.årsak
        )
        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @PutMapping("/{behandlingId}/sett-på-vent/oppdater")
    fun oppdaterVentendeBehandling(
        @PathVariable behandlingId: Long,
        settBehandlingPåVentDto: BehandlingPåVentDto
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "endre behandling på vent"
        )
        settBehandlingPåVentService.oppdaterBehandlingPåVent(
            behandlingId,
            settBehandlingPåVentDto.frist,
            settBehandlingPåVentDto.årsak
        )
        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @PutMapping("/{behandlingId}/sett-på-vent/gjenoppta")
    fun gjenopptaBehandling(
        @PathVariable behandlingId: Long
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "gjenoppta behandling"
        )
        settBehandlingPåVentService.gjenopptaBehandlingPåVent(behandlingId)
        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }
}
