package no.nav.familie.ks.sak.kjerne.eøs.valutakurs

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.ValutakursDto
import no.nav.familie.ks.sak.api.dto.tilValutakurs
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.ecb.ECBService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
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
import java.time.LocalDate

@RestController
@RequestMapping("/api/differanseberegning/valutakurs")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ValutakursController(
    private val tilgangService: TilgangService,
    private val valutakursService: ValutakursService,
    private val personidentService: PersonidentService,
    private val behandlingService: BehandlingService,
    private val ecbService: ECBService,
) {
    @PutMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterValutakurs(
        @PathVariable behandlingId: Long,
        @RequestBody valutakursDto: ValutakursDto,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.UPDATE,
            handling = "Oppdaterer valutakurs",
        )
        val barnAktører = valutakursDto.barnIdenter.map { personidentService.hentAktør(it) }

        val valutaKurs =
            if (skalManueltSetteValutakurs(valutakursDto)) {
                valutakursDto.tilValutakurs(barnAktører)
            } else {
                oppdaterValutakursMedKursFraECB(valutakursDto, valutakursDto.tilValutakurs(barnAktører = barnAktører))
            }

        valutakursService.oppdaterValutakurs(BehandlingId(behandlingId), valutaKurs)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["{behandlingId}/{valutakursId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun slettValutakurs(
        @PathVariable behandlingId: Long,
        @PathVariable valutakursId: Long,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.DELETE,
            handling = "Sletter valutakurs",
        )
        valutakursService.slettValutakurs(valutakursId)

        return ResponseEntity.ok(Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandlingId)))
    }

    private fun oppdaterValutakursMedKursFraECB(
        restValutakurs: ValutakursDto,
        valutakurs: Valutakurs,
    ) = if (valutakursErEndret(restValutakurs, valutakursService.hentValutakurs(restValutakurs.id))) {
        valutakurs.copy(
            kurs =
                ecbService.hentValutakurs(
                    restValutakurs.valutakode!!,
                    restValutakurs.valutakursdato!!,
                ),
        )
    } else {
        valutakurs
    }

    /**
     * Sjekker om valuta er Islandske Kroner og kursdato er før 01.02.2018
     */
    private fun skalManueltSetteValutakurs(restValutakurs: ValutakursDto): Boolean =
        restValutakurs.valutakursdato != null &&
            restValutakurs.valutakode == "ISK" &&
            restValutakurs.valutakursdato.isBefore(
                LocalDate.of(2018, 2, 1),
            )

    /**
     * Sjekker om *restValutakurs* inneholder nødvendige verdier og sammenligner disse med *eksisterendeValutakurs*
     */
    private fun valutakursErEndret(
        restValutakurs: ValutakursDto,
        eksisterendeValutakurs: Valutakurs,
    ): Boolean = restValutakurs.valutakode != null && restValutakurs.valutakursdato != null && (eksisterendeValutakurs.valutakursdato != restValutakurs.valutakursdato || eksisterendeValutakurs.valutakode != restValutakurs.valutakode)
}
