package no.nav.familie.ks.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaKlient
import no.nav.familie.ks.sak.integrasjon.infotrygd.InnsynResponse
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class InfotrygdFeedKlientMock {
    @Bean
    @Profile("mock-infotrygd-replika")
    @Primary
    fun mockInfotrygdReplikaClient(): InfotrygdReplikaKlient {
        val infotrygdReplikaKlient = mockk<InfotrygdReplikaKlient>(relaxed = true)

        every { infotrygdReplikaKlient.hentAlleBarnasIdenterForLøpendeFagsaker() } returns emptySet()
        every { infotrygdReplikaKlient.hentKontantstøttePerioderFraInfotrygd(any()) } returns InnsynResponse(emptyList())

        return infotrygdReplikaKlient
    }
}
