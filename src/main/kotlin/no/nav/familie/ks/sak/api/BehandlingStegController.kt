package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.BesluttVedtakDto
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
import org.springframework.web.bind.annotation.RequestParam
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

    @PostMapping(path = ["/vilkårsvurdering"])
    fun utførVilkårsvurdering(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "utfør vilkårsvurdering"
        )

        stegService.utførSteg(behandlingId, BehandlingSteg.VILKÅRSVURDERING)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @PostMapping(path = ["/behandlingsresultat"])
    fun utledBehandlingsresultat(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "vurdere behandlingsresultat"
        )

        stegService.utførSteg(behandlingId, BehandlingSteg.BEHANDLINGSRESULTAT)
        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @PostMapping(path = ["/foreslå-vedtak"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun foreslåVedtak(
        @PathVariable behandlingId: Long,
        @RequestParam behandlendeEnhet: String
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "foreslå vedtak"
        )

        stegService.utførSteg(behandlingId, BehandlingSteg.VEDTAK)
        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @PostMapping(path = ["/beslutt-vedtak"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun besluttVedtak(
        @PathVariable behandlingId: Long,
        @RequestBody besluttVedtakDto: BesluttVedtakDto
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.BESLUTTER,
            handling = "beslutt vedtak"
        )

        stegService.utførSteg(behandlingId, BehandlingSteg.BESLUTTE_VEDTAK, besluttVedtakDto)
        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }
}
