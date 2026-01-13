package no.nav.familie.ks.sak.integrasjon.ecb

import no.nav.familie.leader.LeaderClient
import no.nav.familie.valutakurs.ECBValutakursRestKlient
import no.nav.familie.valutakurs.domene.ecb.Frequency
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Component
class ECBScheduler(
    private val ecbClient: ECBValutakursRestKlient,
) {
    private val logger: Logger = LoggerFactory.getLogger(ECBScheduler::class.java)

    @Scheduled(cron = "0 0 0 8 * MON-FRI")
    fun dagligSjekkOmValutakursklientFungerer() {
        if (LeaderClient.isLeader() == true) {
            val førsteMandagIForrigeMåned =
                LocalDate
                    .now()
                    .minusMonths(1)
                    .withDayOfMonth(1)
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))

            try {
                ecbClient.hentValutakurs(
                    frequency = Frequency.Daily,
                    currencies = listOf("NOK"),
                    exchangeRateDate = førsteMandagIForrigeMåned,
                )
            } catch (e: Exception) {
                logger.error(
                    """Den daglige selftesten mot ECB feilet med verdier NOK og $førsteMandagIForrigeMåned. 
                    |- Sjekk om ECB er nede. 
                    |- Sjekk om API har endret seg. 
                    |
                    |Se README i https://github.com/navikt/familie-felles/tree/main/valutakurs-klient for å kjøre integrasjonstest mot ECB og for linker til dokumentasjonen til ECB
                    |
                    """.trimMargin(),
                    e,
                )
            }
        }
    }
}
