package no.nav.familie.ks.sak.api

import jakarta.validation.Valid
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.KompensasjonAndelDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleConfig.Companion.KOMPENSASJONSORDNING
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.TilbakestillBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.KompensasjonAndelService
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
@RequestMapping("/api/kompensasjonandel")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class KompensasjonAndelController(
    private val kompensasjonAndelService: KompensasjonAndelService,
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val unleashNextMedContextService: UnleashNextMedContextService,
) {
    @PutMapping(path = ["/{behandlingId}/{kompensasjonAndelId}"])
    fun oppdaterKompensasjonAndelOgOppdaterTilkjentYtelse(
        @PathVariable behandlingId: Long,
        @PathVariable kompensasjonAndelId: Long,
        @Valid @RequestBody kompensasjonAndelDto: KompensasjonAndelDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        validerAtKompensasjonsordningToggleErPå()

        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.UPDATE,
            handling = "Oppdater kompensasjonsandel",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)

        kompensasjonAndelService.oppdaterKompensasjonAndelOgOppdaterTilkjentYtelse(
            behandling,
            kompensasjonAndelId,
            kompensasjonAndelDto,
        )

        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId = behandlingId)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["/{behandlingId}/{kompensasjonAndelId}"])
    fun fjernKompensasjonAndelOgOppdaterTilkjentYtelse(
        @PathVariable behandlingId: Long,
        @PathVariable kompensasjonAndelId: Long,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        validerAtKompensasjonsordningToggleErPå()

        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.DELETE,
            handling = "Fjern kompensasjonsandel",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)

        kompensasjonAndelService.fjernKompensasjongAndelOgOppdaterTilkjentYtelse(
            behandling,
            kompensasjonAndelId,
        )

        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId = behandling.id)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["/{behandlingId}"])
    fun opprettTomKompensasjonAndel(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        validerAtKompensasjonsordningToggleErPå()

        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.CREATE,
            handling = "Opprett kompensasjonsandel",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)

        if (!behandling.erKompensasjonsordning()) {
            throw FunksjonellFeil("Behandlingen har ikke årsak '${BehandlingÅrsak.KOMPENSASJONSORDNING_2024.visningsnavn}'")
        }

        kompensasjonAndelService.opprettTomKompensasjonAndel(behandling)

        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId = behandling.id)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandling.id)))
    }

    private fun validerAtKompensasjonsordningToggleErPå() {
        if (!unleashNextMedContextService.isEnabled(KOMPENSASJONSORDNING)) {
            throw FunksjonellFeil("Behandling med årsak kompensasjonsordning er ikke tilgjengelig")
        }
    }
}
