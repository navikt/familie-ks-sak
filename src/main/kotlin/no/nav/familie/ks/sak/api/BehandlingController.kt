package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.EndreBehandlendeEnhetDto
import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
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
class BehandlingController(private val behandlingService: BehandlingService) {

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettBehandling(@RequestBody opprettBehandlingDto: OpprettBehandlingDto): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        val behandling = behandlingService.opprettBehandling(opprettBehandlingDto)
        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandling.id)))
    }

    @GetMapping(path = ["/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentBehandling(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @PutMapping(path = ["/{behandlingId}/enhet"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun endreBehandlendeEnhet(
        @PathVariable behandlingId: Long,
        @RequestBody endreBehandlendeEnhet: EndreBehandlendeEnhetDto
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {

        if (endreBehandlendeEnhet.begrunnelse.isBlank()) {
            throw FunksjonellFeil(
                melding = "Begrunnelse kan ikke være tom",
                frontendFeilmelding = "Du må skrive en begrunnelse for endring av enhet"
            )
        }

        behandlingService.oppdaterBehandlendeEnhet(behandlingId, endreBehandlendeEnhet)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }
}
