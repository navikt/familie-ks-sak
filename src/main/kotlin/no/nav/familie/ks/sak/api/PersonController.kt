package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.PersonInfoDto
import no.nav.familie.ks.sak.api.dto.tilPersonInfoDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
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
    val personOpplysningerService: PersonopplysningerService,
    val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    val integrasjonService: IntegrasjonService,
    val tilgangService: TilgangService,
    val behandlingService: BehandlingService,
) {
    @GetMapping
    @Deprecated("Slettes når vi går over til POST endepunktene i frontend")
    fun hentPerson(
        @RequestHeader personIdent: String,
        @RequestBody personIdentBody: PersonIdent?,
    ): ResponseEntity<Ressurs<PersonInfoDto>> {
        val aktør = personidentService.hentAktør(personIdent)
        val personinfo =
            integrasjonService.hentMaskertPersonInfoVedManglendeTilgang(aktør)
                ?: personOpplysningerService
                    .hentPersonInfoMedRelasjonerOgRegisterinformasjon(aktør)
                    .tilPersonInfoDto(personIdent)
        return ResponseEntity.ok(Ressurs.success(personinfo))
    }

    @PostMapping
    fun hentPerson(
        @RequestBody personIdentDto: PersonIdent,
    ): ResponseEntity<Ressurs<PersonInfoDto>> {
        val personIdent = personIdentDto.ident

        val aktør = personidentService.hentAktør(personIdent)
        val personinfo =
            integrasjonService.hentMaskertPersonInfoVedManglendeTilgang(aktør)
                ?: personOpplysningerService
                    .hentPersonInfoMedRelasjonerOgRegisterinformasjon(aktør)
                    .tilPersonInfoDto(personIdent)
        return ResponseEntity.ok(Ressurs.success(personinfo))
    }

    @GetMapping(path = ["/enkel"])
    @Deprecated("Slettes når vi går over til POST endepunktene i frontend")
    fun hentPersonEnkel(
        @RequestHeader personIdent: String,
        @RequestBody personIdentBody: PersonIdent?,
    ): ResponseEntity<Ressurs<PersonInfoDto>> {
        val aktør = personidentService.hentAktør(personIdent)
        val personinfo =
            integrasjonService.hentMaskertPersonInfoVedManglendeTilgang(aktør)
                ?: personOpplysningerService
                    .hentPersoninfoEnkel(aktør)
                    .tilPersonInfoDto(personIdent)
        return ResponseEntity.ok(Ressurs.success(personinfo))
    }

    @PostMapping(path = ["/enkel"])
    fun hentPersonEnkel(
        @RequestBody personIdentDto: PersonIdent,
    ): ResponseEntity<Ressurs<PersonInfoDto>> {
        val personIdent = personIdentDto.ident

        val aktør = personidentService.hentAktør(personIdent)
        val personinfo =
            integrasjonService.hentMaskertPersonInfoVedManglendeTilgang(aktør)
                ?: personOpplysningerService
                    .hentPersoninfoEnkel(aktør)
                    .tilPersonInfoDto(personIdent)
        return ResponseEntity.ok(Ressurs.success(personinfo))
    }

    @GetMapping(path = ["/oppdater-registeropplysninger/{behandlingId}"])
    fun hentOgOppdaterRegisteropplysningerPåBehandling(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<BehandlingResponsDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            event = AuditLoggerEvent.UPDATE,
            handling = "Oppdater registeropplysninger på behandling",
        )

        val behandling = behandlingService.hentBehandling(behandlingId)

        personopplysningGrunnlagService.oppdaterRegisteropplysningerPåBehandling(behandling)

        return ResponseEntity.ok(
            Ressurs.success(behandlingService.lagBehandlingRespons(behandlingId = behandling.id)),
        )
    }
}
