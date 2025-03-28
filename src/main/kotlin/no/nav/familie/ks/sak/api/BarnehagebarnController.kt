package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.BarnehagebarnService
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnDtoInterface
import no.nav.familie.ks.sak.barnehagelister.epost.EpostService
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.data.domain.Page
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/barnehagebarn")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BarnehagebarnController(
    private val barnehagebarnService: BarnehagebarnService,
    private val tilgangService: TilgangService,
    private val epostService: EpostService,
) {
    @PostMapping(
        path = ["/barnehagebarnliste"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentAlleBarnehagebarnPage(
        @RequestBody(required = true) barnehagebarnRequestParams: BarnehagebarnRequestParams,
    ): ResponseEntity<Ressurs<Page<BarnehagebarnDtoInterface>>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente ut alle barnehagebarn",
        )
        val alleBarnehagebarnPage = barnehagebarnService.hentBarnehageBarn(barnehagebarnRequestParams)
        return ResponseEntity.ok(Ressurs.success(alleBarnehagebarnPage, "OK"))
    }

    @GetMapping(
        path = ["/barnehagekommuner"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentAlleBarnehageKommuner(): ResponseEntity<Ressurs<Set<String>>> = ResponseEntity.ok(Ressurs.success(barnehagebarnService.hentAlleKommuner()))
}
