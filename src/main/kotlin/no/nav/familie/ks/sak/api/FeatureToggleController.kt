package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.common.util.RessursUtils
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/featuretoggles")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FeatureToggleController(
    private val unleashNextMedContextService: UnleashNextMedContextService,
) {
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentToggles(
        @RequestBody toggles: List<String>,
    ): ResponseEntity<Ressurs<Map<String, Boolean>>> =
        RessursUtils.ok(
            toggles.fold(mutableMapOf()) { acc, toggleId ->
                acc[toggleId] = unleashNextMedContextService.isEnabled(toggleId)
                acc
            },
        )
}
