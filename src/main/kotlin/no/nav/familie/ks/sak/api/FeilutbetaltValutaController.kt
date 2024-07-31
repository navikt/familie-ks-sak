package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.FeilutbetaltValutaDto
import no.nav.familie.ks.sak.api.dto.tilFeilutbetaltValutaDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta.FeilutbetaltValuta
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta.FeilutbetaltValutaService
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
@RequestMapping("/api/feilutbetalt-valuta")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FeilutbetaltValutaController(
    private val tilgangService: TilgangService,
    private val feilutbetaltValutaService: FeilutbetaltValutaService,
    private val behandlingService: BehandlingService,
) {
    @PostMapping(
        path = ["/behandlinger/{behandlingId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun leggTilFeilutbetaltValuta(
        @PathVariable behandlingId: Long,
        @RequestBody feilutbetaltValutaDto: FeilutbetaltValutaDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "legg til feilutbetalt valuta med periode til behandling",
        )

        feilutbetaltValutaService.leggTilFeilutbetaltValuta(
            FeilutbetaltValuta(
                behandlingId = behandlingId,
                fom = feilutbetaltValutaDto.fom,
                tom = feilutbetaltValutaDto.tom,
                feilutbetaltBeløp = feilutbetaltValutaDto.feilutbetaltBeløp,
            ),
        )

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @PutMapping(
        path = ["/behandlinger/{behandlingId}/{feilutbetaltValutaId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun oppdaterFeilutbetaltValuta(
        @PathVariable behandlingId: Long,
        @PathVariable feilutbetaltValutaId: Long,
        @RequestBody feilutbetaltValuta: FeilutbetaltValutaDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "oppdater feilutbetalt valuta i behandling",
        )

        feilutbetaltValutaService.oppdaterFeilutbetaltValuta(
            oppdatertFeilutbetaltValuta = feilutbetaltValuta,
            id = feilutbetaltValutaId,
        )

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["/behandlinger/{behandlingId}/{feilutbetaltValutaId}"])
    fun fjernFeilutbetaltValuta(
        @PathVariable behandlingId: Long,
        @PathVariable feilutbetaltValutaId: Long,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Fjerner feilutbetalt valuta i behandling",
        )
        feilutbetaltValutaService.fjernFeilutbetaltValuta(id = feilutbetaltValutaId)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @GetMapping(path = ["/behandlinger/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAlleFeilutbetaltValutaForBehandling(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<List<FeilutbetaltValutaDto>?>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "henter alle feilutbetalt valuta for behandling",
        )
        return ResponseEntity.ok(
            Ressurs.success(
                feilutbetaltValutaService
                    .hentAlleFeilutbetaltValutaForBehandling(
                        behandlingId = behandlingId,
                    ).map { it.tilFeilutbetaltValutaDto() },
            ),
        )
    }
}
