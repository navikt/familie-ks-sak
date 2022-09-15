package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.ks.sak.api.dto.FinnOppgaveDto
import no.nav.familie.ks.sak.common.util.RessursUtils.illegalState
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/oppgave")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppgaveController(
    private val oppgaveService: OppgaveService,
    private val tilgangService: TilgangService
) {

    @PostMapping(
        path = ["/hent-oppgaver"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun hentOppgaver(@RequestBody finnOppgaveDto: FinnOppgaveDto): ResponseEntity<Ressurs<FinnOppgaveResponseDto>> =
        try {
            val oppgaver = oppgaveService.hentOppgaver(finnOppgaveDto.tilFinnOppgaveRequest())
            ResponseEntity.ok().body(Ressurs.success(oppgaver, "Finn oppgaver OK"))
        } catch (e: Throwable) {
            illegalState("Henting av oppgaver feilet", e)
        }

    @PostMapping(path = ["/{oppgaveId}/fordel"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fordelOppgave(@PathVariable oppgaveId: Long, @RequestParam saksbehandler: String): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Fordele oppgave"
        )

        val oppgaveIdFraRespons =
            oppgaveService.fordelOppgave(
                oppgaveId = oppgaveId,
                saksbehandler = saksbehandler,
                overstyrFordeling = false
            )

        return ResponseEntity.ok().body(Ressurs.success(oppgaveIdFraRespons))
    }

    @PostMapping(path = ["/{oppgaveId}/tilbakestill"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun tilbakestillFordelingPåOppgave(@PathVariable oppgaveId: Long): ResponseEntity<Ressurs<Oppgave>> {

        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "tilbakestille fordeling på oppgave"
        )

        Result.runCatching {
            oppgaveService.tilbakestillFordelingPåOppgave(oppgaveId)
        }.fold(
            onSuccess = { return ResponseEntity.ok().body(Ressurs.Companion.success(it)) },
            onFailure = { return illegalState("Feil ved tilbakestilling av tildeling på oppgave", it) }
        )
    }

    @GetMapping("/{oppgaveId}/ferdigstill")
    fun ferdigstillOppgave(@PathVariable oppgaveId: Long): ResponseEntity<Ressurs<String>> {

        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Ferdigstill oppgave"
        )

        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        oppgaveService.ferdigstillOppgave(oppgave)

        return ResponseEntity.ok(Ressurs.success("Oppgave ferdigstilt"))
    }
}
