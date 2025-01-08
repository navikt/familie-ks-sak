package no.nav.familie.ks.sak.config

import no.nav.familie.ks.sak.common.ClockProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockProviderConfig {
    @Bean
    fun clockProvider(): ClockProvider =
        ClockProvider {
            Clock.systemDefaultZone()
        }
}
