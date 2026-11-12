package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.TilgangRequestDto
import no.nav.familie.ks.sak.api.dto.TilgangResponsDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.sikkerhet.PersonTilgangService
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TilgangController(
    private val personOpplysningerService: PersonopplysningerService,
    private val personidentService: PersonidentService,
    private val personTilgangService: PersonTilgangService,
    private val tilgangService: TilgangService,
) {
    @PostMapping(path = ["/tilgang"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentTilgangOgDiskresjonskode(
        @RequestBody tilgangRequestDTO: TilgangRequestDto,
    ): ResponseEntity<Ressurs<TilgangResponsDto>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Sjekke tilgang til person",
        )

        val aktør = personidentService.hentAktør(tilgangRequestDTO.brukerIdent)
        val adressebeskyttelse = personOpplysningerService.hentAdressebeskyttelseSomSystembruker(aktør)
        val harTilgang = personTilgangService.sjekkTilgangTilPerson(tilgangRequestDTO.brukerIdent).harTilgang

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
