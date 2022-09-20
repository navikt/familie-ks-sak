package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.ks.sak.integrasjon.journalføring.InnkommendeJournalføringService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/journalpost")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class JournalføringController(
    private val innkommendeJournalføringService: InnkommendeJournalføringService,
) {

    @GetMapping(path = ["/for-bruker/{brukerId}"])
    fun hentJournalposterForBruker(@PathVariable brukerId: String): ResponseEntity<Ressurs<List<Journalpost>>> =
        ResponseEntity.ok(Ressurs.success(innkommendeJournalføringService.hentJournalposterForBruker(brukerId)))
}
