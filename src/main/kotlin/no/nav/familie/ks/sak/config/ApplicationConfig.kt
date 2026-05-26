package no.nav.familie.ks.sak.config

import no.nav.familie.log.NavSystemtype
import no.nav.familie.log.filter.LogFilter
import no.nav.familie.log.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.sikkerhet.context.FamilieFellesSpringSecurityKonfigurasjon
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootConfiguration
@EntityScan("no.nav.familie.prosessering", ApplicationConfig.PAKKENAVN)
@ComponentScan(
    "no.nav.familie.prosessering",
    "no.nav.familie.unleash",
    "no.nav.familie.felles.tokenklient",
    ApplicationConfig.PAKKENAVN,
)
@EnableRetry
@ConfigurationPropertiesScan
@EnableScheduling
@Import(
    ConsumerIdClientInterceptor::class,
    FamilieFellesSpringSecurityKonfigurasjon::class,
)
class ApplicationConfig {
    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        log.info("Registering LogFilter filter")

        return FilterRegistrationBean<LogFilter>().apply {
            setFilter(LogFilter(systemtype = NavSystemtype.NAV_SAKSBEHANDLINGSSYSTEM))
            order = 1
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ApplicationConfig::class.java)
        const val PAKKENAVN = "no.nav.familie.ks.sak"
    }
}
