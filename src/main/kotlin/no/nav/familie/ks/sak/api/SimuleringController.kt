package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.SimuleringDto
import no.nav.familie.ks.sak.api.mapper.SimuleringMapper.tilSimuleringDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
class SimuleringController(
    private val simuleringService: SimuleringService,
    private val tilgangService: TilgangService
) {

    @GetMapping(path = ["/{behandlingId}/simulering"])
    fun hentSimulering(
        @PathVariable behandlingId: Long
    ): ResponseEntity<Ressurs<SimuleringDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.ACCESS,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente og/eller oppdatere simulering på behandling"
        )
        return ResponseEntity.ok(
            Ressurs.success(
                simuleringService.oppdaterSimuleringPåBehandlingVedBehov(behandlingId).tilSimuleringDto()
            )
        )
    }
}
