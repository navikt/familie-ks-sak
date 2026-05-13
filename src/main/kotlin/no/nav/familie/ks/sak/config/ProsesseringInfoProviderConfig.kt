package no.nav.familie.ks.sak.config

import no.nav.familie.ks.sak.sikkerhet.Rolle.PROSESSERING
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext.harRolle
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext.hentSaksbehandlerEpost
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProsesseringInfoProviderConfig {
    @Bean
    fun prosesseringInfoProvider() =
        object : ProsesseringInfoProvider {
            override fun hentBrukernavn(): String = hentSaksbehandlerEpost()

            override fun harTilgang(): Boolean = harRolle(PROSESSERING)
        }
}
