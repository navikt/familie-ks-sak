package no.nav.familie.ks.sak.api

import jakarta.validation.Valid
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.TilgangsstyrtJournalpost
import no.nav.familie.ks.sak.api.dto.JournalføringRequestDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.journalføring.InnkommendeJournalføringService
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/journalpost")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class JournalføringController(
    private val innkommendeJournalføringService: InnkommendeJournalføringService,
    private val tilgangService: TilgangService,
) {
    @PostMapping(path = ["/bruker"])
    fun hentJournalposterForBruker(
        @RequestBody personIdentBody: PersonIdent,
    ): ResponseEntity<Ressurs<List<TilgangsstyrtJournalpost>>> =
        ResponseEntity.ok(
            Ressurs.success(
                innkommendeJournalføringService.hentJournalposterForBruker(
                    personIdentBody.ident,
                ),
            ),
        )

    @GetMapping("/{journalpostId}/dokument/{dokumentId}")
    fun hentDokumentIJournalpost(
        @PathVariable journalpostId: String,
        @PathVariable dokumentId: String,
    ): ResponseEntity<Ressurs<ByteArray>> =
        ResponseEntity.ok(
            Ressurs.success(
                innkommendeJournalføringService.hentDokumentIJournalpost(
                    journalpostId,
                    dokumentId,
                ),
            ),
        )

    @GetMapping(
        path = ["/{journalpostId}/dokument/{dokumentId}/pdf"],
        produces = [MediaType.APPLICATION_PDF_VALUE],
    )
    fun hentDokumentIJournalpostSomPdf(
        @PathVariable journalpostId: String,
        @PathVariable dokumentId: String,
    ): ResponseEntity<ByteArray> = ResponseEntity.ok(innkommendeJournalføringService.hentDokumentIJournalpost(journalpostId, dokumentId))

    @PostMapping(path = ["/{journalpostId}/journalfør/{oppgaveId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun journalførOppgave(
        @PathVariable journalpostId: String,
        @PathVariable oppgaveId: String,
        @RequestBody @Valid request: JournalføringRequestDto,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "journalføring",
        )
        if (request.dokumenter.any { it.dokumentTittel.isNullOrBlank() }) {
            throw FunksjonellFeil("Minst ett av dokumentene mangler dokumenttittel.")
        }
        val fagsakId = innkommendeJournalføringService.journalfør(request, journalpostId, oppgaveId)
        return ResponseEntity.ok(Ressurs.success(fagsakId, "Journalpost $journalpostId Journalført"))
    }
}
