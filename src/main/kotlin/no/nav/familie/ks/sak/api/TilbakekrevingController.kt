package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.ForhåndsvisTilbakekrevingVarselbrevDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tilbakekreving")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class TilbakekrevingController(
    private val tilgangService: TilgangService,
    private val tilbakekrevingService: TilbakekrevingService
) {

    @PostMapping("/{behandlingId}/forhåndsvis-tilbakekreving-varselbrev")
    fun hentForhåndsvisningVarselbrev(
        @PathVariable behandlingId: Long,
        @RequestBody forhåndsvisTilbakekrevingVarselbrevDto: ForhåndsvisTilbakekrevingVarselbrevDto
    ): ResponseEntity<Ressurs<ByteArray>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente forhåndsvisning av varselbrev for tilbakekreving"
        )
        return ResponseEntity.ok(
            Ressurs.success(
                tilbakekrevingService.hentForhåndsvisningTilbakekrevingVarselBrev(
                    behandlingId,
                    forhåndsvisTilbakekrevingVarselbrevDto
                )
            )
        )
    }
}
