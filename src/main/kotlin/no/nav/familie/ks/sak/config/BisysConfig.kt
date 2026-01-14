package no.nav.familie.ks.sak.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.sikkerhet.OIDCUtil
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
@Profile("!integrasjonstest")
class BisysConfig(
    private val oidcUtil: OIDCUtil,
    private val rolleConfig: RolleConfig,
) {
    @Bean
    fun bisysFilter() =
        object : OncePerRequestFilter() {
            override fun doFilterInternal(
                request: HttpServletRequest,
                response: HttpServletResponse,
                filterChain: FilterChain,
            ) {
                val clientNavn: String? =
                    try {
                        oidcUtil.getClaim("azp_name")
                    } catch (throwable: Throwable) {
                        null
                    }
                val erKallerBisys = clientNavn?.contains("bidrag") ?: false
                val harForvalterRolle = SikkerhetContext.harInnloggetBrukerForvalterRolle(rolleConfig)
                val erBisysRequest = request.requestURI.startsWith("/api/bisys")

                when {
                    erKallerBisys && !erBisysRequest -> {
                        response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Bisys applikasjon kan ikke kalle andre tjenester",
                        )
                    }

                    erBisysRequest && (!harForvalterRolle && !erKallerBisys) -> {
                        response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Bisys tjeneste kan kun kalles av bisys eller innlogget bruker med FORVALTER rolle",
                        )
                    }

                    erBisysRequest && (harForvalterRolle || erKallerBisys) -> {
                        filterChain.doFilter(request, response)
                    }

                    !erBisysRequest && !erKallerBisys -> {
                        filterChain.doFilter(request, response)
                    }
                }
            }

            override fun shouldNotFilter(request: HttpServletRequest) =
                request.requestURI.contains("/internal") ||
                    request.requestURI.startsWith("/swagger") ||
                    request.requestURI.startsWith("/v3") // i bruk av swagger
        }
}
