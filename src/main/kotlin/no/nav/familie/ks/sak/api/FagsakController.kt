package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerResponsDto
import no.nav.familie.ks.sak.api.dto.FagsakRequestDto
import no.nav.familie.ks.sak.api.dto.MinimalFagsakResponsDto
import no.nav.familie.ks.sak.api.dto.SøkParamDto
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ks.sak.sikkerhet.SjekkTilgang
import no.nav.familie.ks.sak.sikkerhet.Tilgang
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/fagsaker")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(private val fagsakService: FagsakService) {

    private val logger: Logger = LoggerFactory.getLogger(FagsakController::class.java)

    @PostMapping(path = ["/sok"])
    fun søkFagsak(@RequestBody søkParam: SøkParamDto): ResponseEntity<Ressurs<List<FagsakDeltagerResponsDto>>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} søker fagsak")
        val fagsakDeltagere = fagsakService.hentFagsakDeltagere(søkParam.personIdent)
        return ResponseEntity.ok().body(Ressurs.success(fagsakDeltagere))
    }

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @SjekkTilgang(Tilgang.PERSON, auditLoggerEvent = AuditLoggerEvent.CREATE, handling = "Opprett fagsak")
    fun hentEllerOpprettFagsak(@RequestBody fagsakRequest: FagsakRequestDto): ResponseEntity<Ressurs<MinimalFagsakResponsDto>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter eller oppretter ny fagsak")
        return ResponseEntity.ok().body(Ressurs.success(fagsakService.hentEllerOpprettFagsak(fagsakRequest)))
    }

    @GetMapping(path = ["/minimal/{fagsakId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @SjekkTilgang(Tilgang.FAGSAK, handling = "Henter fagsak")
    fun hentMinimalFagsak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<MinimalFagsakResponsDto>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter minimal fagsak med id $fagsakId")
        return ResponseEntity.ok().body(Ressurs.success(fagsakService.hentMinimalFagsak(fagsakId)))
    }

    @PostMapping(path = ["/hent-fagsak-paa-person"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @SjekkTilgang(Tilgang.PERSON, handling = "Henter fagsak for person")
    fun hentMinimalFagsakForPerson(@RequestBody request: PersonIdent): ResponseEntity<Ressurs<MinimalFagsakResponsDto>> {
        return ResponseEntity.ok().body(Ressurs.success(fagsakService.hentMinimalFagsakForPerson(request.ident)))
    }
}
