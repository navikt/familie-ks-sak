package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/a-inntekt")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class AInntektController(
    private val integrasjonService: IntegrasjonService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("hent-url")
    fun hentAInntektUrl(
        @RequestBody personIdent: PersonIdent,
    ): Ressurs<String> {
        tilgangService.validerTilgangTilHandlingOgPersoner(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "hente a-inntekt url",
            personIdenter = listOf(personIdent.ident),
            event = AuditLoggerEvent.ACCESS,
        )

        return Ressurs.success(integrasjonService.hentAInntektUrl(personIdent))
    }
}
