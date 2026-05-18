package no.nav.familie.ks.sak.sikkerhet

import no.nav.familie.ks.sak.config.RolleConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtAudienceValidator
import org.springframework.security.oauth2.jwt.JwtIssuerValidator
import org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component("azureAdAuthenticationManager")
class AzureAdAuthenticationManager(
    @Value("\${AZURE_OPENID_CONFIG_JWKS_URI}") jwksUri: String,
    @Value("\${AZURE_OPENID_CONFIG_ISSUER}") issuer: String,
    @Value("\${AZURE_APP_CLIENT_ID}") audience: String,
    @param:Value("\${prosessering.rolle}") private val prosesseringRolle: String,
    private val rolleConfig: RolleConfig,
) : AuthenticationManager {
    private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    private val teamfamilieRegex = Regex(".*:teamfamilie:.*")
    private val klageRegex = Regex(".*:teamfamilie:familie-klage")
    private val bisysRegex = Regex(".*:bidrag:bidrag-grunnlag(-feature)?")

    private val authenticationManager: AuthenticationManager =
        run {
            val decoder =
                NimbusJwtDecoder.withJwkSetUri(jwksUri).build().also {
                    it.setJwtValidator(
                        createDefaultWithValidators(
                            JwtIssuerValidator(issuer),
                            JwtAudienceValidator(audience),
                        ),
                    )
                }
            val provider = JwtAuthenticationProvider(decoder)
            provider.setJwtAuthenticationConverter(::convert)
            ProviderManager(provider)
        }

    override fun authenticate(authentication: Authentication): Authentication = authenticationManager.authenticate(authentication)

    private fun convert(jwt: Jwt): JwtAuthenticationToken {
        val grupper = jwt.getClaimAsStringList("groups") ?: emptyList()
        val appNavn = jwt.getClaimAsString("azp_name") ?: ""
        val roles = jwt.getClaimAsStringList("roles") ?: emptyList()

        val roller =
            buildSet {
                if (grupper.contains(rolleConfig.VEILEDER_ROLLE)) add(Rolle.VEILEDER)
                if (grupper.contains(rolleConfig.SAKSBEHANDLER_ROLLE)) add(Rolle.SAKSBEHANDLER)
                if (grupper.contains(rolleConfig.BESLUTTER_ROLLE)) add(Rolle.BESLUTTER)
                if (grupper.contains(rolleConfig.FORVALTER_ROLLE)) add(Rolle.FORVALTER)
                if (grupper.contains(prosesseringRolle)) add(Rolle.PROSESSERING)

                if (roles.contains("access_as_application")) {
                    if (appNavn.matches(teamfamilieRegex)) add(Rolle.TEAMFAMILIE_APPLIKASJON)
                    if (appNavn.matches(klageRegex)) add(Rolle.KLAGE_APPLIKASJON)
                    if (appNavn.matches(bisysRegex)) add(Rolle.BISYS_APPLIKASJON)
                }
            }

        if (roller.isEmpty()) {
            secureLogger.warn("Bruker har ingen gyldige roller. Grupper i token: ${grupper.joinToString(", ")}")
        }

        return JwtAuthenticationToken(jwt, roller.map { SimpleGrantedAuthority(it.authority()) })
    }
}
