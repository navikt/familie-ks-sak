package no.nav.familie.ks.sak.sikkerhet

import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.RolleConfig
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.slf4j.LoggerFactory

object SikkerhetContext {
    const val SYSTEM_FORKORTELSE = "VL"
    const val SYSTEM_NAVN = "System"

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun erSystemKontekst() = hentSaksbehandler() == SYSTEM_FORKORTELSE

    fun erMaskinTilMaskinToken(): Boolean {
        val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
        return claims.get("oid") != null &&
            claims.get("oid") == claims.get("sub") &&
            claims.getAsList("roles").contains("access_as_application")
    }

    fun hentSaksbehandler(): String =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = { it.hentClaimsForIssuer("azuread")?.get("NAVident")?.toString() ?: SYSTEM_FORKORTELSE },
                onFailure = { SYSTEM_FORKORTELSE },
            )

    fun hentSaksbehandlerEpost(): String =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = {
                    it.hentClaimsForIssuer("azuread")?.get("preferred_username")?.toString() ?: SYSTEM_FORKORTELSE
                },
                onFailure = { SYSTEM_FORKORTELSE },
            )

    fun hentSaksbehandlerNavn(): String =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = { it.hentClaimsForIssuer("azuread")?.get("name")?.toString() ?: SYSTEM_NAVN },
                onFailure = { SYSTEM_NAVN },
            )

    fun hentRolletilgangFraSikkerhetscontext(
        rolleConfig: RolleConfig,
        lavesteSikkerhetsnivå: BehandlerRolle?,
    ): BehandlerRolle {
        if (hentSaksbehandler() == SYSTEM_FORKORTELSE) return BehandlerRolle.SYSTEM

        val grupper = hentGrupper()
        val høyesteSikkerhetsnivåForInnloggetBruker =
            when {
                grupper.contains(rolleConfig.BESLUTTER_ROLLE) -> BehandlerRolle.BESLUTTER
                grupper.contains(rolleConfig.SAKSBEHANDLER_ROLLE) -> BehandlerRolle.SAKSBEHANDLER
                grupper.contains(rolleConfig.FORVALTER_ROLLE) -> BehandlerRolle.FORVALTER
                grupper.contains(rolleConfig.VEILEDER_ROLLE) -> BehandlerRolle.VEILEDER
                else -> BehandlerRolle.UKJENT
            }

        return when {
            lavesteSikkerhetsnivå == null -> BehandlerRolle.UKJENT
            høyesteSikkerhetsnivåForInnloggetBruker.nivå >= lavesteSikkerhetsnivå.nivå -> lavesteSikkerhetsnivå
            else -> BehandlerRolle.UKJENT
        }
    }

    fun hentHøyesteRolletilgangForInnloggetBruker(rolleConfig: RolleConfig): BehandlerRolle {
        if (hentSaksbehandler() == SYSTEM_FORKORTELSE) return BehandlerRolle.SYSTEM

        val grupper = hentGrupper()

        return when {
            grupper.contains(rolleConfig.BESLUTTER_ROLLE) -> BehandlerRolle.BESLUTTER
            grupper.contains(rolleConfig.SAKSBEHANDLER_ROLLE) -> BehandlerRolle.SAKSBEHANDLER
            grupper.contains(rolleConfig.FORVALTER_ROLLE) -> BehandlerRolle.FORVALTER
            grupper.contains(rolleConfig.VEILEDER_ROLLE) -> BehandlerRolle.VEILEDER
            else -> BehandlerRolle.UKJENT
        }
    }

    fun harInnloggetBrukerForvalterRolle(rolleConfig: RolleConfig): Boolean = hentSaksbehandler() == SYSTEM_FORKORTELSE || hentGrupper().contains(rolleConfig.FORVALTER_ROLLE)

    fun hentGrupper(): List<String> =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = {
                    @Suppress("UNCHECKED_CAST")
                    if (it.issuers.contains("azuread")) it.getClaims("azuread").get("groups") as List<String> else emptyList()
                },
                onFailure = { emptyList() },
            )

    fun kallKommerFraKlage(): Boolean = kallKommerFra("teamfamilie:familie-klage")

    fun TokenValidationContext.hentClaimsForIssuer(issuer: String): JwtTokenClaims? = if (this.issuers.contains(issuer)) this.getClaims(issuer) else null

    private fun kallKommerFra(forventetApplikasjonsSuffix: String): Boolean {
        val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
        val applikasjonsnavn = claims.get("azp_name")?.toString() ?: "" // e.g. dev-gcp:some-team:application-name
        secureLogger.info("Applikasjonsnavn: $applikasjonsnavn")
        return applikasjonsnavn.endsWith(forventetApplikasjonsSuffix)
    }
}
