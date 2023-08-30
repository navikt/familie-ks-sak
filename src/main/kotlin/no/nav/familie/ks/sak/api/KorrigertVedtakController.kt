package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.KorrigertVedtakDto
import no.nav.familie.ks.sak.api.dto.tilKorrigertVedtak
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtakService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/korrigertvedtak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class KorrigertVedtakController(
    private val behandlingService: BehandlingService,
    private val korrigertVedtakService: KorrigertVedtakService,
    private val tilgangService: TilgangService,
) {

    @PostMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettKorrigertVedtakPåBehandling(
        @PathVariable behandlingId: Long,
        @RequestBody korrigertVedtakDto: KorrigertVedtakDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.CREATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Opprett korrigering på vedtak",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)
        val korrigertVedtak = korrigertVedtakDto.tilKorrigertVedtak(behandling)

        korrigertVedtakService.lagreKorrigertVedtakOgDeaktiverGamle(korrigertVedtak)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId)))
    }

    @PatchMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun settKorrigertVedtakTilInaktivPåBehandling(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.UPDATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Sett korrigering på vedtak til inaktiv",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)
        korrigertVedtakService.settKorrigertVedtakPåBehandlingTilInaktiv(behandling)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }
}
