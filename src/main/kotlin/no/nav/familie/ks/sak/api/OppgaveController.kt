package no.nav.familie.ks.sak.api

import FerdigstillOppgaveKnyttJournalpostDto
import jakarta.validation.Valid
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.ks.sak.api.dto.DataForManuellJournalføringDto
import no.nav.familie.ks.sak.api.dto.FinnOppgaveDto
import no.nav.familie.ks.sak.api.dto.tilPersonInfoDto
import no.nav.familie.ks.sak.common.util.RessursUtils.illegalState
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.journalføring.InnkommendeJournalføringService
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
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
    private val personOpplysningerService: PersonopplysningerService,
    private val personidentService: PersonidentService,
    private val fagsakService: FagsakService,
    private val integrasjonService: IntegrasjonService,
    private val tilgangService: TilgangService,
    private val innkommendeJournalføringService: InnkommendeJournalføringService,
) {
    @PostMapping(
        path = ["/hent-oppgaver"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentOppgaver(
        @RequestBody finnOppgaveDto: FinnOppgaveDto,
    ): ResponseEntity<Ressurs<FinnOppgaveResponseDto>> =
        try {
            val oppgaver = oppgaveService.hentOppgaver(finnOppgaveDto.tilFinnOppgaveRequest())
            ResponseEntity.ok().body(Ressurs.success(oppgaver, "Finn oppgaver OK"))
        } catch (e: Throwable) {
            illegalState("Henting av oppgaver feilet", e)
        }

    @PostMapping(path = ["/{oppgaveId}/fordel"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fordelOppgave(
        @PathVariable oppgaveId: Long,
        @RequestParam saksbehandler: String,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Fordele oppgave",
        )

        val oppgaveIdFraRespons =
            oppgaveService.fordelOppgave(
                oppgaveId = oppgaveId,
                saksbehandler = saksbehandler,
                overstyrFordeling = false,
            )

        return ResponseEntity.ok().body(Ressurs.success(oppgaveIdFraRespons))
    }

    @PostMapping(path = ["/{oppgaveId}/tilbakestill"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun tilbakestillFordelingPåOppgave(
        @PathVariable oppgaveId: Long,
    ): ResponseEntity<Ressurs<Oppgave>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Tilbakestille fordeling på oppgave",
        )

        Result
            .runCatching {
                oppgaveService.tilbakestillFordelingPåOppgave(oppgaveId)
            }.fold(
                onSuccess = { return ResponseEntity.ok().body(Ressurs.Companion.success(it)) },
                onFailure = { return illegalState("Feil ved tilbakestilling av tildeling på oppgave", it) },
            )
    }

    @GetMapping("/{oppgaveId}/ferdigstill")
    fun ferdigstillOppgave(
        @PathVariable oppgaveId: Long,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Ferdigstill oppgave",
        )

        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        oppgaveService.ferdigstillOppgave(oppgave)

        return ResponseEntity.ok(Ressurs.success("Oppgave ferdigstilt"))
    }

    @GetMapping(path = ["/{oppgaveId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentDataForManuellJournalføring(
        @PathVariable oppgaveId: Long,
    ): ResponseEntity<Ressurs<DataForManuellJournalføringDto>> {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        val aktør = oppgave.aktoerId?.let { personidentService.hentAktør(it) }
        val journalpost = oppgave.journalpostId?.let { integrasjonService.hentJournalpost(it) }

        val dataForManuellJournalføringDto =
            DataForManuellJournalføringDto(
                oppgave = oppgave,
                journalpost = journalpost,
                person =
                    aktør?.let {
                        personOpplysningerService
                            .hentPersonInfoMedRelasjonerOgRegisterinformasjon(it)
                            .tilPersonInfoDto(it.aktivFødselsnummer())
                    },
                minimalFagsak = aktør?.let { fagsakService.finnMinimalFagsakForPerson(aktør.aktørId) },
            )

        return ResponseEntity.ok(Ressurs.success(dataForManuellJournalføringDto))
    }

    @PostMapping(path = ["/{oppgaveId}/ferdigstillOgKnyttjournalpost"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun ferdigstillOppgaveOgKnyttJournalpostTilBehandling(
        @PathVariable oppgaveId: Long,
        @RequestBody @Valid request: FerdigstillOppgaveKnyttJournalpostDto,
    ): ResponseEntity<Ressurs<String?>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "ferdigstill oppgave og knytt journalpost",
        )
        // Validerer at oppgave med gitt oppgaveId eksisterer
        oppgaveService.hentOppgave(oppgaveId)
        val fagsakId = innkommendeJournalføringService.knyttJournalpostTilFagsakOgFerdigstillOppgave(request, oppgaveId)
        return ResponseEntity.ok(Ressurs.success(fagsakId, "Oppgaven $oppgaveId er lukket"))
    }
}
