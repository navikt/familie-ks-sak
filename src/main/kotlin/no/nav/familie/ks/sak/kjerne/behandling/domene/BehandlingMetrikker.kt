package no.nav.familie.ks.sak.kjerne.behandling.domene

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.leader.LeaderClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Component
class BehandlingMetrikker(
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val behandlingRepository: BehandlingRepository,
) {
    private val enheter = listOf("2103", "4806", "4820", "4833", "4842", "4817", "4812")

    private val antallBehandlingerTotalt = MultiGauge.builder("behandlinger.totalt").register(Metrics.globalRegistry)

    private val antallBehandlingerOpprettet = Metrics.counter("behandlinger.opprettet")

    private val antallBehandlingerPerType = initBehandlingTypeMetrikker()

    private val antallBehandlingerPerÅrsak = initBehandlingÅrsakMetrikker()

    private val antallBehandlingerBehandletPerEnhet = initBehandlingerBehandletPerEnhetMetrikker()

    private val antallBehandlingsresultat =
        Behandlingsresultat.values().associateWith {
            Metrics.counter(
                "behandlinger.resultat",
                "type",
                it.name,
                "beskrivelse",
                it.displayName
            )
        }

    private val behandlingstid: DistributionSummary = Metrics.summary("behandlinger.tid")

    fun tellNøkkelTallVedOpprettelseAvBehandling(behandling: Behandling) {
        antallBehandlingerOpprettet.increment()
        antallBehandlingerPerType[behandling.type]?.increment()
        antallBehandlingerPerÅrsak[behandling.opprettetÅrsak]?.increment()
    }

    fun oppdaterBehandlingMetrikker(behandling: Behandling) {
        tellBehandlingstidMetrikk(behandling)
        tellBehandlingerBehandletPerEnhetMetrikk(behandling)
        tellBehandlingsresultatTypeMetrikk(behandling)
    }

    private fun tellBehandlingerBehandletPerEnhetMetrikk(behandling: Behandling) {
        val behandlendeEnhet = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id)

        antallBehandlingerBehandletPerEnhet[behandlendeEnhet.behandlendeEnhetId]?.increment()
    }

    private fun tellBehandlingstidMetrikk(behandling: Behandling) {
        val dagerSidenOpprettet = ChronoUnit.DAYS.between(behandling.opprettetTidspunkt, LocalDateTime.now())
        behandlingstid.record(dagerSidenOpprettet.toDouble())
    }

    private fun tellBehandlingsresultatTypeMetrikk(behandling: Behandling) =
        antallBehandlingsresultat[behandling.resultat]?.increment()

    private fun initBehandlingTypeMetrikker(): Map<BehandlingType, Counter> =
        BehandlingType.values().associateWith {
            Metrics.counter(
                "behandlinger.opprettet",
                "type",
                it.name,
                "beskrivelse",
                it.visningsnavn,
            )
        }

    private fun initBehandlingÅrsakMetrikker(): Map<BehandlingÅrsak, Counter> =
        BehandlingÅrsak.values().associateWith {
            Metrics.counter(
                "behandlinger.aarsak",
                "aarsak",
                it.name,
                "beskrivelse",
                it.visningsnavn
            )
        }

    private fun initBehandlingerBehandletPerEnhetMetrikker(): Map<String, Counter> =
        enheter.associateWith {
            Metrics.counter(
                "behandlinger.ferdigstilt",
                "enhet",
                it
            )
        }

    @Scheduled(cron = "@daily")
    fun tellAlleBehandlinger() {
        if (!erLeader()) return

        val behandlinger = behandlingRepository.count()
        val dagensDato = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val multiGaugeRows = mutableListOf(
            MultiGauge.Row.of(
                Tags.of(
                    ÅR_MÅNED_DAG,
                    dagensDato
                ),
                behandlinger
            )
        )

        enheter.forEach { enhet ->
            val behandlingerPåEnhet = arbeidsfordelingService.hentAlleBehandlingerPåEnhet(enhet)

            multiGaugeRows.add(
                MultiGauge.Row.of(
                    Tags.of(
                        "$enhet-$ÅR_MÅNED_DAG",
                        dagensDato
                    ),
                    behandlingerPåEnhet.size
                )
            )
        }

        antallBehandlingerTotalt.register(multiGaugeRows)
    }

    private fun erLeader(): Boolean {
        return LeaderClient.isLeader() == true
    }

    companion object {
        const val ÅR_MÅNED_DAG = "aar-maaned-dag"
    }
}
