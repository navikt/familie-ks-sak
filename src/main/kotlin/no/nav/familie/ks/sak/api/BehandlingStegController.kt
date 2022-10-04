package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.RegistrerSøknadDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/behandlinger/{behandlingId}/steg")
@ProtectedWithClaims(issuer = "azuread")
class BehandlingStegController(
    private val tilgangService: TilgangService,
    private val stegService: StegService,
    private val behandlingService: BehandlingService
) {

    @PostMapping(path = ["/registrer-søknad"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun registrereSøknadOgOppdaterPersongrunnlag(
        @PathVariable behandlingId: Long,
        @RequestBody registerSøknadDto: RegistrerSøknadDto
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "registrere søknad"
        )

        stegService.utførSteg(behandlingId, BehandlingSteg.REGISTRERE_SØKNAD, registerSøknadDto)
        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }
}
