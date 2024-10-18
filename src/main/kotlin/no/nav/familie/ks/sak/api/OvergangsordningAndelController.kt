package no.nav.familie.ks.sak.api

import jakarta.validation.Valid
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.OvergangsordningAndelDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleConfig.Companion.OVERGANGSORDNING
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.TilbakestillBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/overgangsordningandel")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OvergangsordningAndelController(
    private val overgangsordningAndelService: OvergangsordningAndelService,
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val unleashNextMedContextService: UnleashNextMedContextService,
) {
    @PutMapping(path = ["/{behandlingId}/{overgangsordningAndelId}"])
    fun oppdaterOvergangsordningAndelOgOppdaterTilkjentYtelse(
        @PathVariable behandlingId: Long,
        @PathVariable overgangsordningAndelId: Long,
        @Valid @RequestBody overgangsordningAndelDto: OvergangsordningAndelDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        validerAtOvergangsordningToggleErPå()

        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.UPDATE,
            handling = "Oppdater overgangsordningandel",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)

        overgangsordningAndelService.oppdaterOvergangsordningAndelOgOppdaterTilkjentYtelse(
            behandling = behandling,
            overgangsordningAndelId = overgangsordningAndelId,
            overgangsordningAndelRequestDto = overgangsordningAndelDto,
        )

        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId = behandlingId)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["/{behandlingId}/{overgangsordningAndelId}"])
    fun fjernOvergangsordningAndelOgOppdaterTilkjentYtelse(
        @PathVariable behandlingId: Long,
        @PathVariable overgangsordningAndelId: Long,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        validerAtOvergangsordningToggleErPå()

        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.DELETE,
            handling = "Fjern overgangsordningandel",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)

        overgangsordningAndelService.fjernOvergangsordningAndelOgOppdaterTilkjentYtelse(
            behandling = behandling,
            overgangsordningAndelId = overgangsordningAndelId,
        )

        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId = behandling.id)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["/{behandlingId}"])
    fun opprettTomOvergangsordningAndel(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        validerAtOvergangsordningToggleErPå()

        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.CREATE,
            handling = "Opprett overgangsordningandel",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)

        if (!behandling.erOvergangsordning()) {
            throw FunksjonellFeil("Behandlingen har ikke årsak '${BehandlingÅrsak.OVERGANGSORDNING_2024.visningsnavn}'")
        }

        overgangsordningAndelService.opprettTomOvergangsordningAndel(behandling)

        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId = behandling.id)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandling.id)))
    }

    private fun validerAtOvergangsordningToggleErPå() {
        if (!unleashNextMedContextService.isEnabled(OVERGANGSORDNING)) {
            throw FunksjonellFeil("Behandling med årsak overgangsordning er ikke tilgjengelig")
        }
    }
}
