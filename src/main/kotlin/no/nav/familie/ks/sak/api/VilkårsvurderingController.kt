package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.EndreVilkårResultatDto
import no.nav.familie.ks.sak.api.dto.NyttVilkårDto
import no.nav.familie.ks.sak.api.dto.VedtakBegrunnelseTilknyttetVilkårResponseDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakBegrunnelseType
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
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
@RequestMapping("/api/vilkårsvurdering")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VilkårsvurderingController(
    private val behandlingService: BehandlingService,
    private val personidentService: PersonidentService,
    private val tilgangService: TilgangService,
    private val vilkårsvurderingService: VilkårsvurderingService
) {

    @PostMapping(path = ["/{behandlingId}"])
    fun opprettNyttVilkår(@PathVariable behandlingId: Long, @RequestBody nyttVilkårDto: NyttVilkårDto):
        ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.CREATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "nytt vilkår"
        )

        vilkårsvurderingService.opprettNyttVilkårPåBehandling(behandlingId, nyttVilkårDto)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @PutMapping(path = ["/{behandlingId}"])
    fun endreVilkår(
        @PathVariable behandlingId: Long,
        @RequestBody endreVilkårResultatDto: EndreVilkårResultatDto
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.UPDATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "endre vilkår"
        )

        vilkårsvurderingService.endreVilkårPåBehandling(
            behandlingId = behandlingId,
            endreVilkårResultatDto = endreVilkårResultatDto
        )

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["/{behandlingId}/{vilkaarId}"])
    fun slettVilkår(
        @PathVariable behandlingId: Long,
        @PathVariable vilkaarId: Long,
        @RequestBody personIdent: String
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.DELETE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "slette eller nullstill vilkår"
        )

        val aktør = personidentService.hentAktør(personIdent)

        vilkårsvurderingService.slettVilkårPåBehandling(
            behandlingId,
            vilkårId = vilkaarId,
            aktør = aktør
        )

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @GetMapping(path = ["/vilkaarsbegrunnelser"])
    fun hentVilkårsbegrunnelser(): ResponseEntity<Ressurs<Map<VedtakBegrunnelseType, List<VedtakBegrunnelseTilknyttetVilkårResponseDto>>>> {
        return ResponseEntity.ok(Ressurs.success(vilkårsvurderingService.hentVilkårsbegrunnelser()))
    }
}
