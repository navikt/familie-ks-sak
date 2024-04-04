package no.nav.familie.ks.sak.api

import jakarta.validation.Valid
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ident")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class IdentController(
    private val personidentService: PersonidentService,
) {
    @PostMapping
    fun håndterPdlHendelse(
        @Valid
        @RequestBody
        nyIdent: PersonIdent,
    ): ResponseEntity<Ressurs<String>> {
        personidentService.opprettTaskForIdentHendelse(nyIdent)
        return ResponseEntity.ok(Ressurs.success("Håndtert ny ident"))
    }
}
