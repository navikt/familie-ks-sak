package no.nav.familie.ks.sak.barnehagelister

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class BarnehagebarnStatistikkService(
    private val barnehagebarnRepository: BarnehagebarnRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val nyeKommunerGauge =
        MultiGauge.builder("barnehagebarn.nye.kommuner").register(Metrics.globalRegistry)

    private val kommunerPerMaanedGauge =
        MultiGauge.builder("barnehagebarn.kommuner.per.maaned").register(Metrics.globalRegistry)

    private val barnPerMaanedGauge =
        MultiGauge.builder("barnehagebarn.barn.per.maaned").register(Metrics.globalRegistry)

    @Scheduled(cron = "@daily")
    fun oppdaterBarnehagebarnStatistikk() {
        logger.info("Oppdaterer barnehagebarn-statistikk")

        oppdaterNyeKommunerPerMaaned()
        oppdaterKommunerPerMaaned()
        oppdaterBarnPerMaaned()
    }

    private fun oppdaterNyeKommunerPerMaaned() {
        val nyeKommuner = barnehagebarnRepository.finnNyeKommunerPerMaaned()

        val rows =
            nyeKommuner.map { rad ->
                MultiGauge.Row.of(
                    Tags.of("month", rad.getMonth(), "kommune_navn", rad.getKommuneNavn()),
                    1,
                )
            }

        nyeKommunerGauge.register(rows, true)
    }

    private fun oppdaterKommunerPerMaaned() {
        val kommunerPerMaaned = barnehagebarnRepository.finnAntallKommunerPerMaaned()

        val rows =
            kommunerPerMaaned.map { rad ->
                MultiGauge.Row.of(
                    Tags.of("month", rad.getMonth()),
                    rad.getAntall(),
                )
            }

        kommunerPerMaanedGauge.register(rows, true)
    }

    private fun oppdaterBarnPerMaaned() {
        val barnPerMaaned = barnehagebarnRepository.finnAntallBarnPerMaaned()

        val rows =
            barnPerMaaned.map { rad ->
                MultiGauge.Row.of(
                    Tags.of("month", rad.getMonth()),
                    rad.getAntall(),
                )
            }

        barnPerMaanedGauge.register(rows, true)
    }
}
