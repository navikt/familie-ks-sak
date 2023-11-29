package no.nav.familie.ks.sak.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonJsonConfig {
    @Bean
    fun objectMapper(): ObjectMapper {
        return objectMapper
    }
}
