package no.nav.familie.ks.sak.kjerne.korrigertetterbetaling

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.KorrigertEtterbetalingRequestDto
import no.nav.familie.ks.sak.api.dto.KorrigertEtterbetalingResponsDto
import no.nav.familie.ks.sak.api.dto.tilKorrigertEtterbetalingResponsDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
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
@RequestMapping("/api/korrigertetterbetaling")
@Validated
class KorrigertEtterbetalingController(
    private val tilgangService: TilgangService,
    private val korrigertEtterbetalingService: KorrigertEtterbetalingService,
    private val behandlingService: BehandlingService,
) {
    @PostMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettKorrigertEtterbetalingPåBehandling(
        @PathVariable behandlingId: Long,
        @RequestBody korrigertEtterbetalingRequestDto: KorrigertEtterbetalingRequestDto,
    ): ResponseEntity<Ressurs<KorrigertEtterbetalingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.CREATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Opprett korrigert etterbetaling",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)

        val korrigertEtterbetaling = korrigertEtterbetalingRequestDto.tilKorrigertEtterbetaling(behandling)
        val lagretKorrigertEtterbetaling = korrigertEtterbetalingService.lagreKorrigertEtterbetaling(korrigertEtterbetaling)

        return ResponseEntity.ok(Ressurs.success(lagretKorrigertEtterbetaling.tilKorrigertEtterbetalingResponsDto()))
    }

    @GetMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKorrigertEtterbetalingPåBehandling(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<KorrigertEtterbetalingResponsDto?>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.ACCESS,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Hent korrigert etterbetaling",
        )

        val korrigertEtterbetaling = korrigertEtterbetalingService.finnAktivtKorrigeringPåBehandling(behandlingId)

        return ResponseEntity.ok(Ressurs.success(korrigertEtterbetaling?.tilKorrigertEtterbetalingResponsDto()))
    }

    @PatchMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun settKorrigertEtterbetalingTilInaktivPåBehandling(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<KorrigertEtterbetalingResponsDto?>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.UPDATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater korrigert etterbetaling",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)

        val korrigertEtterbetaling = korrigertEtterbetalingService.settKorrigeringPåBehandlingTilInaktiv(behandling)

        return ResponseEntity.ok(Ressurs.success(korrigertEtterbetaling?.tilKorrigertEtterbetalingResponsDto()))
    }
}
