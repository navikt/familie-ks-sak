package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerResponsDto
import no.nav.familie.ks.sak.api.dto.SøkParamDto
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakDeltagerService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/fagsaker/sok")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakDeltagerController(
    private val fagsakDeltagerService: FagsakDeltagerService,
) {
    private val logger: Logger = LoggerFactory.getLogger(FagsakDeltagerController::class.java)

    @PostMapping
    fun søkFagsak(
        @RequestBody søkParam: SøkParamDto,
    ): ResponseEntity<Ressurs<List<FagsakDeltagerResponsDto>>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} søker fagsak")
        val fagsakDeltagere = fagsakDeltagerService.hentFagsakDeltagere(søkParam.personIdent)
        return ResponseEntity.ok().body(Ressurs.success(fagsakDeltagere))
    }
}
