package no.nav.familie.ks.sak.api

import com.azure.identity.ClientSecretCredentialBuilder
import com.microsoft.graph.serviceclient.GraphServiceClient
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

    @PostMapping(
        path = ["/barnehageliste/send-epost-custom-credentials"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun sendEpostMedInnlogging(
        @RequestBody loginParams: LoginParams,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "send e-postvarsel",
        )

        val credential =
            ClientSecretCredentialBuilder()
                .tenantId(loginParams.tenantId)
                .clientId(loginParams.clientId)
                .clientSecret(loginParams.clientSecret)
                .build()
        val customGraphServiceClient = GraphServiceClient(credential, "https://graph.microsoft.com/.default")
        val customEpostService = EpostService(customGraphServiceClient)

        customEpostService.sendEpostVarslingBarnehagelister("fredrik.markus.pfeil@nav.no", listOf("hei", "på", "deg"))

        return ResponseEntity.ok(Ressurs.success("OK"))
    }

    data class LoginParams(
        val clientId: String,
        val clientSecret: String,
        val tenantId: String,
    )

    @PostMapping(
        path = ["/barnehageliste/send-epost"],
    )
    fun sendEpost(): ResponseEntity<Ressurs<String>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "send e-postvarsel",
        )

        epostService.sendEpostVarslingBarnehagelister("fredrik.markus.pfeil@nav.no", listOf("halla"))

        return ResponseEntity.ok(Ressurs.success("OK"))
    }

    @GetMapping(
        path = ["/barnehagekommuner"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentAlleBarnehageKommuner(): ResponseEntity<Ressurs<Set<String>>> = ResponseEntity.ok(Ressurs.success(barnehagebarnService.hentAlleKommuner()))
}
