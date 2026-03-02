package no.nav.familie.ks.sak.config

import no.nav.familie.kontrakter.felles.jsonMapperBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper

@Configuration
class JacksonJsonConfig {
    @Bean
    fun jsonMapper(): JsonMapper =
        jsonMapperBuilder
            .build()
}
