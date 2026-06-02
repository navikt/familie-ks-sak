package no.nav.familie.ks.sak.config

import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.log.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.log.interceptor.MdcValuesPropagatingClientInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class RestClientConfig(
    private val entraIDRestClientFactory: EntraIDRestClientFactory,
    private val consumerIdClientInterceptor: ConsumerIdClientInterceptor,
    private val mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor,
) {
    @Bean("integrasjonerRestClient")
    fun integrasjonerRestClient(
        @Value("\${FAMILIE_INTEGRASJONER_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory.lagHybridRestKlient(scope) {
            SikkerhetContext.hentJwt()?.tokenValue
        }

    @Bean("klageRestClient")
    fun klageRestClient(
        @Value("\${FAMILIE_KLAGE_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory.lagHybridRestKlient(scope) {
            SikkerhetContext.hentJwt()?.tokenValue
        }

    @Bean("tilbakekrevingRestClient")
    fun tilbakekrevingRestClient(
        @Value("\${FAMILIE_TILBAKE_API_URL_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory.lagHybridRestKlient(scope) {
            SikkerhetContext.hentJwt()?.tokenValue
        }

    @Bean("infotrygdRestClient")
    fun infotrygdRestClient(
        @Value("\${FAMILIE_KS_INFOTRYGD_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory.lagHybridRestKlient(scope) {
            SikkerhetContext.hentJwt()?.tokenValue
        }

    @Bean("oppdragRestClient")
    fun oppdragRestClient(
        @Value("\${FAMILIE_OPPDRAG_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory.lagHybridRestKlient(scope) {
            SikkerhetContext.hentJwt()?.tokenValue
        }

    @Bean("avstemmingRestClient")
    fun avstemmingRestClient(
        @Value("\${FAMILIE_OPPDRAG_SCOPE}") scope: String,
    ): RestClient {
        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofSeconds(150))
                setReadTimeout(Duration.ofSeconds(150))
            }
        return entraIDRestClientFactory
            .lagMaskinTilMaskinRestKlient(scope)
            .mutate()
            .requestFactory(requestFactory)
            .build()
    }

    @Bean("pdlRestClient")
    fun pdlRestClient(
        @Value("\${PDL_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope)

    @Bean("utenAuthRestClient")
    fun utenAuthRestClient(): RestClient =
        RestClient
            .builder()
            .requestInterceptor(consumerIdClientInterceptor)
            .requestInterceptor(mdcValuesPropagatingClientInterceptor)
            .build()
}
