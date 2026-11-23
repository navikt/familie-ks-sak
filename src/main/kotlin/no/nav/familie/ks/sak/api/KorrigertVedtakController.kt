package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.KorrigertVedtakRequestDto
import no.nav.familie.ks.sak.api.dto.KorrigertVedtakResponsDto
import no.nav.familie.ks.sak.api.dto.tilKorrigertVedtak
import no.nav.familie.ks.sak.api.dto.tilKorrigertVedtakResponsDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtakService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/korrigertvedtak")
@Validated
class KorrigertVedtakController(
    private val behandlingService: BehandlingService,
    private val korrigertVedtakService: KorrigertVedtakService,
    private val tilgangService: TilgangService,
) {
    @PostMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettKorrigertVedtakPåBehandling(
        @PathVariable behandlingId: Long,
        @RequestBody korrigertVedtakRequestDto: KorrigertVedtakRequestDto,
    ): ResponseEntity<Ressurs<KorrigertVedtakResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.CREATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Opprett korrigering på vedtak",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)
        val korrigertVedtak = korrigertVedtakRequestDto.tilKorrigertVedtak(behandling)

        val lagretKorrigertVedtak = korrigertVedtakService.lagreKorrigertVedtakOgDeaktiverGamle(korrigertVedtak)

        return ResponseEntity.ok(Ressurs.success(lagretKorrigertVedtak.tilKorrigertVedtakResponsDto()))
    }

    @GetMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKorrigertVedtakPåBehandling(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<KorrigertVedtakResponsDto?>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.ACCESS,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Hent korrigering på vedtak",
        )

        val korrigertVedtak = korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(behandlingId)

        return ResponseEntity.ok(Ressurs.success(korrigertVedtak?.tilKorrigertVedtakResponsDto()))
    }

    @PatchMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun settKorrigertVedtakTilInaktivPåBehandling(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<KorrigertVedtakResponsDto?>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.UPDATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Sett korrigering på vedtak til inaktiv",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)
        val korrigertVedtak = korrigertVedtakService.settKorrigertVedtakPåBehandlingTilInaktiv(behandling)

        return ResponseEntity.ok(Ressurs.success(korrigertVedtak?.tilKorrigertVedtakResponsDto()))
    }
}
