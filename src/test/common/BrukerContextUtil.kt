package no.nav.familie.ks.sak.data

import jakarta.servlet.http.HttpServletRequest
import no.nav.familie.ks.sak.sikkerhet.Rolle
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant

object BrukerContextUtil {
    fun clearBrukerContext() {
        RequestContextHolder.resetRequestAttributes()
        SecurityContextHolder.clearContext()
    }

    fun mockBrukerContext(
        preferredUsername: String = "A",
        groups: List<String> = emptyList(),
        servletRequest: HttpServletRequest = MockHttpServletRequest(),
    ) {
        val requestAttributes = ServletRequestAttributes(servletRequest)
        RequestContextHolder.setRequestAttributes(requestAttributes)

        val roller =
            groups.mapNotNull { group ->
                when (group) {
                    Rolle.BESLUTTER.name -> Rolle.BESLUTTER
                    Rolle.SAKSBEHANDLER.name -> Rolle.SAKSBEHANDLER
                    Rolle.VEILEDER.name -> Rolle.VEILEDER
                    Rolle.FORVALTER.name -> Rolle.FORVALTER
                    else -> null
                }
            }

        val jwt =
            Jwt
                .withTokenValue("mock-token-$preferredUsername")
                .header("alg", "none")
                .claim("preferred_username", preferredUsername)
                .claim("NAVident", preferredUsername)
                .claim("name", preferredUsername)
                .claim("groups", groups)
                .claim("oid", "mock-oid-$preferredUsername")
                .claim("sub", "mock-sub-different-$preferredUsername")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build()

        val authorities = roller.map { SimpleGrantedAuthority(it.authority()) }
        val authentication = JwtAuthenticationToken(jwt, authorities)
        SecurityContextHolder.setContext(SecurityContextImpl(authentication))
    }
}
