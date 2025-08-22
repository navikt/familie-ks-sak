package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.FagsakRequestDto
import no.nav.familie.ks.sak.api.dto.MinimalFagsakResponsDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
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
@RequestMapping("/api/fagsaker")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(
    private val fagsakService: FagsakService,
    private val tilgangService: TilgangService,
    private val tilbakekrevingService: TilbakekrevingService,
    private val personidentService: PersonidentService,
) {
    private val logger: Logger = LoggerFactory.getLogger(FagsakController::class.java)

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentEllerOpprettFagsak(
        @RequestBody fagsakRequest: FagsakRequestDto,
    ): ResponseEntity<Ressurs<MinimalFagsakResponsDto>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter eller oppretter ny fagsak")

        tilgangService.validerTilgangTilHandlingOgPersoner(
            personIdenter = listOfNotNull(fagsakRequest.personIdent),
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.CREATE,
            handling = "Opprett fagsak",
        )

        return ResponseEntity.ok().body(Ressurs.success(fagsakService.hentEllerOpprettFagsak(fagsakRequest)))
    }

    @GetMapping(path = ["/minimal/{fagsakId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMinimalFagsak(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<Ressurs<MinimalFagsakResponsDto>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter minimal fagsak med id $fagsakId")

        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsakId = fagsakId,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            event = AuditLoggerEvent.ACCESS,
            handling = "Hent fagsak",
        )

        return ResponseEntity.ok().body(Ressurs.success(fagsakService.hentMinimalFagsak(fagsakId)))
    }

    @PostMapping(path = ["/hent-fagsak-paa-person"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMinimalFagsakForPerson(
        @RequestBody request: PersonIdent,
    ): ResponseEntity<Ressurs<MinimalFagsakResponsDto>> {
        val personIdent = request.ident

        tilgangService.validerTilgangTilHandlingOgFagsakForPerson(
            personIdent = personIdent,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            event = AuditLoggerEvent.ACCESS,
            handling = "Hent fagsak for person",
        )
        val minimalFagsakForPerson = fagsakService.finnMinimalFagsakForPerson(personIdent)

        return minimalFagsakForPerson?.let { ResponseEntity.ok().body(Ressurs.success(it)) }
            ?: ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Ressurs.failure(errorMessage = "Fant ikke fagsak på person"))
    }

    @GetMapping(
        path = ["/{fagsakId}/har-åpen-tilbakekrevingsbehandling"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun harÅpenTilbakekrevingsbehandling(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<Ressurs<Boolean>> {
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsakId = fagsakId,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            event = AuditLoggerEvent.ACCESS,
            handling = "sjekke om saken har en åpen tilbakekrevingsbehandling",
        )

        return ResponseEntity.ok(Ressurs.success(tilbakekrevingService.harÅpenTilbakekrevingsbehandling(fagsakId)))
    }

    @PostMapping(path = ["/sok/fagsaker-hvor-person-er-deltaker"])
    fun søkFagsakerHvorPersonErDeltaker(
        @RequestBody request: RestSøkFagsakRequest,
    ): ResponseEntity<Ressurs<List<RestFagsakIdOgTilknyttetAktørId>>> {
        tilgangService.validerTilgangTilHandlingOgPersoner(
            personIdenter = listOf(request.personIdent),
            event = AuditLoggerEvent.ACCESS,
            minimumBehandlerRolle = BehandlerRolle.SYSTEM,
            handling = "Søk fagsaker hvor person er deltaker",
        )

        val aktør = personidentService.hentAktør(request.personIdent)

        val fagsakerHvorAktørErSøkerEllerMottarLøpendeKontantstøtte =
            fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeKontantstøtte(aktør)

        val fagsakIdOgTilknyttetAktørId =
            fagsakerHvorAktørErSøkerEllerMottarLøpendeKontantstøtte.map {
                RestFagsakIdOgTilknyttetAktørId(aktørId = it.aktør.aktørId, fagsakId = it.id)
            }

        return ResponseEntity.ok().body(Ressurs.success(fagsakIdOgTilknyttetAktørId))
    }
}

data class RestSøkFagsakRequest(
    val personIdent: String,
)

data class RestFagsakIdOgTilknyttetAktørId(
    val aktørId: String,
    val fagsakId: Long,
)
