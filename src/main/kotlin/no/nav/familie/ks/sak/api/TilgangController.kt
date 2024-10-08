package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.TilgangRequestDto
import no.nav.familie.ks.sak.api.dto.TilgangResponsDto
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
class TilgangController(
    private val personOpplysningerService: PersonopplysningerService,
    private val personidentService: PersonidentService,
    private val integrasjonService: IntegrasjonService,
) {
    @PostMapping(path = ["/tilgang"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentTilgangOgDiskresjonskode(
        @RequestBody tilgangRequestDTO: TilgangRequestDto,
    ): ResponseEntity<Ressurs<TilgangResponsDto>> {
        val aktør = personidentService.hentAktør(tilgangRequestDTO.brukerIdent)
        val adressebeskyttelse = personOpplysningerService.hentAdressebeskyttelseSomSystembruker(aktør)
        val harTilgang = integrasjonService.sjekkTilgangTilPerson(tilgangRequestDTO.brukerIdent).harTilgang

        return ResponseEntity.ok(
            Ressurs.success(
                data =
                    TilgangResponsDto(
                        saksbehandlerHarTilgang = harTilgang,
                        adressebeskyttelsegradering = adressebeskyttelse,
                    ),
            ),
        )
    }
}
