package no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp

import jakarta.validation.Valid
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.UtenlandskPeriodebeløpDto
import no.nav.familie.ks.sak.api.dto.tilUtenlandskPeriodebeløp
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/differanseberegning/utenlandskperidebeløp")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class UtenlandskPeriodebeløpController(
    private val tilgangService: TilgangService,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService,
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
    private val personidentService: PersonidentService,
    private val behandlingService: BehandlingService,
) {
    @PutMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterUtenlandskPeriodebeløp(
        @PathVariable behandlingId: Long,
        @Valid @RequestBody
        restUtenlandskPeriodebeløp: UtenlandskPeriodebeløpDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.UPDATE,
            handling = "Oppdaterer utenlandsk periodebeløp",
        )

        val barnAktører = restUtenlandskPeriodebeløp.barnIdenter.map { personidentService.hentAktør(it) }

        val eksisterendeUtenlandskPeriodeBeløp =
            utenlandskPeriodebeløpRepository.getReferenceById(restUtenlandskPeriodebeløp.id)

        val utenlandskPeriodebeløp =
            restUtenlandskPeriodebeløp.tilUtenlandskPeriodebeløp(barnAktører, eksisterendeUtenlandskPeriodeBeløp)

        utenlandskPeriodebeløpService
            .oppdaterUtenlandskPeriodebeløp(behandlingId, utenlandskPeriodebeløp)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId)))
    }

    @DeleteMapping(path = ["{behandlingId}/{utenlandskPeriodebeløpId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun slettUtenlandskPeriodebeløp(
        @PathVariable behandlingId: Long,
        @PathVariable utenlandskPeriodebeløpId: Long,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.UPDATE,
            handling = "Sletter utenlandsk periodebeløp",
        )

        utenlandskPeriodebeløpService.slettUtenlandskPeriodebeløp(behandlingId, utenlandskPeriodebeløpId)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }
}
