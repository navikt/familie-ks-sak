package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.VedtakBegrunnelseTilknyttetVilkårResponseDto
import no.nav.familie.ks.sak.kjerne.vedtak.VedtakBegrunnelseType
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vilkaarsvurdering")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VilkårsvurderingController(
    private val vilkårsvurderingService: VilkårsvurderingService
) {
    @GetMapping(path = ["/vilkaarsbegrunnelser"])
    fun hentVilkårsbegrunnelser(): ResponseEntity<Ressurs<Map<VedtakBegrunnelseType, List<VedtakBegrunnelseTilknyttetVilkårResponseDto>>>> {
        return ResponseEntity.ok(Ressurs.success(vilkårsvurderingService.hentVilkårsbegrunnelser()))
    }
}
