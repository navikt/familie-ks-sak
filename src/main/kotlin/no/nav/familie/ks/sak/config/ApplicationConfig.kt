package no.nav.familie.ks.sak.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.http.client.RetryOAuth2HttpClient
import no.nav.familie.http.interceptor.BearerTokenClientCredentialsClientInterceptor
import no.nav.familie.http.interceptor.BearerTokenClientInterceptor
import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.NavSystemtype
import no.nav.familie.log.filter.LogFilter
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.security.token.support.client.core.http.OAuth2HttpClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.temporal.ChronoUnit

@SpringBootConfiguration
@EntityScan("no.nav.familie.prosessering", ApplicationConfig.PAKKENAVN)
@ComponentScan("no.nav.familie.prosessering", "no.nav.familie.unleash", ApplicationConfig.PAKKENAVN)
@EnableRetry
@ConfigurationPropertiesScan
@EnableJwtTokenValidation(ignore = ["org.springdoc", "org.springframework"])
@EnableScheduling
@Import(
    ConsumerIdClientInterceptor::class,
    BearerTokenClientInterceptor::class,
    BearerTokenClientCredentialsClientInterceptor::class,
)
@EnableOAuth2Client(cacheEnabled = true)
class ApplicationConfig {
    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        log.info("Registering LogFilter filter")

        return FilterRegistrationBean<LogFilter>().apply {
            filter = LogFilter(systemtype = NavSystemtype.NAV_SAKSBEHANDLINGSSYSTEM)
            order = 1
        }
    }

    /**
     * Overskriver felles sin som bruker proxy, som ikke skal brukes på gcp.
     */
    @Bean
    @Primary
    fun restTemplateBuilder(objectMapper: ObjectMapper): RestTemplateBuilder {
        val jackson2HttpMessageConverter = MappingJackson2HttpMessageConverter(objectMapper)
        return RestTemplateBuilder()
            .connectTimeout(Duration.of(2, ChronoUnit.SECONDS))
            .readTimeout(Duration.of(30, ChronoUnit.SECONDS))
            .additionalMessageConverters(listOf(jackson2HttpMessageConverter) + RestTemplate().messageConverters)
    }

    /**
     * Overskriver OAuth2HttpClient som settes opp i token-support som ikke kan få med objectMapper fra felles
     * pga. .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
     * og [OAuth2AccessTokenResponse] som burde settes med setters, då feltnavn heter noe annet enn feltet i json
     */
    @Bean
    @Primary
    fun oAuth2HttpClient(): OAuth2HttpClient =
        RetryOAuth2HttpClient(
            RestClient.create(
                RestTemplateBuilder()
                    .connectTimeout(Duration.of(2, ChronoUnit.SECONDS))
                    .readTimeout(Duration.of(4, ChronoUnit.SECONDS))
                    .build(),
            ),
        )

    @Bean
    fun restOperations(): RestOperations =
        RestTemplate(
            listOf(
                StringHttpMessageConverter(StandardCharsets.UTF_8),
                ByteArrayHttpMessageConverter(),
                MappingJackson2HttpMessageConverter(objectMapper),
            ),
        )

    @Bean
    fun prosesseringInfoProvider(
        @Value("\${prosessering.rolle}") prosesseringRolle: String,
    ) = object : ProsesseringInfoProvider {
        override fun hentBrukernavn(): String =
            try {
                SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread").getStringClaim("preferred_username")
            } catch (e: Exception) {
                "VL"
            }

        override fun harTilgang(): Boolean = grupper().contains(prosesseringRolle)

        @Suppress("UNCHECKED_CAST")
        private fun grupper(): List<String> =
            try {
                SpringTokenValidationContextHolder()
                    .getTokenValidationContext()
                    .getClaims("azuread")
                    ?.get("groups") as List<String>? ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
    }

    @Bean("jwtBearer")
    fun restTemplateJwtBearer(
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        bearerTokenClientInterceptor: BearerTokenClientInterceptor,
    ): RestOperations =
        RestTemplateBuilder()
            .connectTimeout(Duration.of(2, ChronoUnit.SECONDS))
            .readTimeout(Duration.of(30, ChronoUnit.SECONDS))
            .additionalMessageConverters(listOf(MappingJackson2HttpMessageConverter(objectMapper)) + RestTemplate().messageConverters)
            .additionalInterceptors(
                consumerIdClientInterceptor,
                bearerTokenClientInterceptor,
                MdcValuesPropagatingClientInterceptor(),
            ).build()

    @Bean("jwtBearerClientCredential")
    fun restTemplateClientCredentialBearer(
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        bearerTokenClientCredentialsClientInterceptor: BearerTokenClientCredentialsClientInterceptor,
    ): RestOperations =
        RestTemplateBuilder()
            .connectTimeout(Duration.of(2, ChronoUnit.SECONDS))
            .readTimeout(Duration.of(30, ChronoUnit.SECONDS))
            .additionalMessageConverters(listOf(MappingJackson2HttpMessageConverter(objectMapper)) + RestTemplate().messageConverters)
            .additionalInterceptors(
                consumerIdClientInterceptor,
                bearerTokenClientCredentialsClientInterceptor,
                MdcValuesPropagatingClientInterceptor(),
            ).build()

    @Bean("jwtBearerLongTimeout")
    fun restTemplateJwtBearerLongTimeout(
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        bearerTokenClientInterceptor: BearerTokenClientInterceptor,
    ): RestOperations =
        RestTemplateBuilder()
            .connectTimeout(Duration.of(150, ChronoUnit.SECONDS))
            .readTimeout(Duration.of(150, ChronoUnit.SECONDS))
            .additionalMessageConverters(listOf(MappingJackson2HttpMessageConverter(objectMapper)) + RestTemplate().messageConverters)
            .interceptors(
                consumerIdClientInterceptor,
                bearerTokenClientInterceptor,
                MdcValuesPropagatingClientInterceptor(),
            ).build()

    companion object {
        private val log = LoggerFactory.getLogger(ApplicationConfig::class.java)
        const val PAKKENAVN = "no.nav.familie.ks.sak"
    }
}
