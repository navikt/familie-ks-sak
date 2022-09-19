package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.PersonInfoDto
import no.nav.familie.ks.sak.api.dto.tilPersonInfoDto
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/person")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PersonController(
    val personidentService: PersonidentService,
    val personOpplysningerService: PersonOpplysningerService,
    val integrasjonClient: IntegrasjonClient
) {

    @GetMapping
    fun hentPerson(
        @RequestHeader personIdent: String,
        @RequestBody personIdentBody: PersonIdent?
    ): ResponseEntity<Ressurs<PersonInfoDto>> {
        val aktør = personidentService.hentAktør(personIdent)
        val personinfo = integrasjonClient.hentMaskertPersonInfoVedManglendeTilgang(aktør)
            ?: personOpplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(aktør)
                .tilPersonInfoDto(personIdent)
        return ResponseEntity.ok(Ressurs.success(personinfo))
    }
}
