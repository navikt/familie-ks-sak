package no.nav.familie.ks.sak.config

import no.nav.familie.ks.sak.common.http.interceptor.RolletilgangInterceptor
import no.nav.familie.sikkerhet.OIDCUtil
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@Import(OIDCUtil::class, RolleConfig::class)
class WebConfig(
    private val rolleConfig: RolleConfig,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(RolletilgangInterceptor(rolleConfig))
            .excludePathPatterns("/api/task/**", "/api/v2/task/**", "/internal", "/testverktoy", "/api/feature")
        super.addInterceptors(registry)
    }
}
