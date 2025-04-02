package no.nav.familie.ks.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ks.sak.integrasjon.infotrygd.InnsynResponse
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class InfotrygdFeedClientMock {
    @Bean
    @Profile("mock-infotrygd-replika")
    @Primary
    fun mockInfotrygdReplikaClient(): InfotrygdReplikaClient {
        val infotrygdReplikaClient = mockk<InfotrygdReplikaClient>(relaxed = true)

        every { infotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker() } returns emptySet()
        every { infotrygdReplikaClient.hentKontantstøttePerioderFraInfotrygd(any()) } returns InnsynResponse(emptyList())

        return infotrygdReplikaClient
    }
}
