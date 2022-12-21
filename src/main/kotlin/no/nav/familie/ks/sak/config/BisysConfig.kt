package no.nav.familie.ks.sak.config

import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.sikkerhet.OIDCUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
class BisysConfig(
    private val oidcUtil: OIDCUtil,
    private val rolleConfig: RolleConfig,
    @Value("\${BIDRAG_GRUNNLAG_CLIENT_ID:dummy}")
    private val bidragGrunnlagClientId: String,
    @Value("\${BIDRAG_GRUNNLAG_FEATURE_CLIENT_ID:dummy}")
    private val bidragGrunnlagFeatureClientId: String
) {

    @Bean
    fun bisysFilter() = object : OncePerRequestFilter() {
        override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
            val clientId = oidcUtil.getClaim("azp")
            val erKallerBisys = clientId == bidragGrunnlagClientId || clientId == bidragGrunnlagFeatureClientId
            val harForvalterRolle = SikkerhetContext.harInnloggetBrukerForvalterRolle(rolleConfig)
            val erBisysRequest = request.requestURI.startsWith("/api/bisys")

            when {
                erKallerBisys && !erBisysRequest -> {
                    response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Bisys applikasjon kan ikke kalle andre tjenester"
                    )
                }
                erBisysRequest && (!harForvalterRolle && !erKallerBisys) -> {
                    response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Bisys tjeneste kan kun kalles av bisys eller innlogget bruker med FORVALTER rolle"
                    )
                }
                erBisysRequest && (harForvalterRolle || erKallerBisys) -> filterChain.doFilter(request, response)
                !erBisysRequest && !erKallerBisys -> filterChain.doFilter(request, response)
            }
        }

        override fun shouldNotFilter(request: HttpServletRequest) =
            request.requestURI.contains("/internal") ||
                request.requestURI.startsWith("/swagger") ||
                request.requestURI.startsWith("/v3") // i bruk av swagger
    }
}
