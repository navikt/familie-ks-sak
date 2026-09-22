package no.nav.familie.ks.sak.task

import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.SlettInaktivePersonopplysningsgrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.SlettetBatch
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SlettInaktivePersonopplysningsgrunnlagScheduler(
    private val slettInaktivePersonopplysningsgrunnlagService: SlettInaktivePersonopplysningsgrunnlagService,
    private val featureToggleService: FeatureToggleService,
) {
    @Scheduled(cron = "0 0 3 * * *")
    fun slettInaktivePersonopplysningsgrunnlag() {
        if (LeaderClient.isLeader() != true) return
        if (!featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_INAKTIVE_PERSONOPPLYSNINGSGRUNNLAG)) return

        var totaltSlettet = 0
        var sisteSlettedeId = 0L
        val slettedeBatcher = mutableListOf<SlettetBatch>()
        for (batch in 0 until MAKS_ANTALL_BATCHER_PER_KJØRING) {
            val slettetBatch =
                slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(
                    etterId = sisteSlettedeId,
                    batchStørrelse = BATCH_STØRRELSE,
                )
            val slettedeIder = slettetBatch.grunnlagIder
            if (slettedeIder.isEmpty()) break

            slettedeBatcher.add(slettetBatch)
            totaltSlettet += slettedeIder.size
            sisteSlettedeId = slettedeIder.last()
        }

        if (totaltSlettet > 0) {
            val antallSlettedeRaderPerTabell = summerAntallSlettedeRaderPerTabell(slettedeBatcher)
            logger.info(
                "Slettet $totaltSlettet inaktive personopplysningsgrunnlag med tilhørende personer og registeropplysninger. " +
                    "Totalt ${antallSlettedeRaderPerTabell.values.sum()} rader slettet: " +
                    antallSlettedeRaderPerTabell.entries.joinToString { (tabell, antall) -> "$tabell=$antall" },
            )
        } else {
            val antallInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling = slettInaktivePersonopplysningsgrunnlagService.tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling()
            if (antallInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling > 0) {
                logger.warn(
                    "Fant $antallInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling inaktive personopplysningsgrunnlag på behandlinger uten aktivt grunnlag. " +
                        "Disse slettes ikke. Må undersøkes manuelt.",
                )
            } else {
                logger.info(
                    "Fant ingen inaktive personopplysningsgrunnlag å slette. " +
                        "Jobben og togglen ${FeatureToggle.SKAL_SLETTE_INAKTIVE_PERSONOPPLYSNINGSGRUNNLAG} kan fjernes.",
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SlettInaktivePersonopplysningsgrunnlagScheduler::class.java)
        internal const val BATCH_STØRRELSE = 200
        internal const val MAKS_ANTALL_BATCHER_PER_KJØRING = 50

        internal fun summerAntallSlettedeRaderPerTabell(slettedeBatcher: List<SlettetBatch>): Map<String, Long> =
            buildMap {
                slettedeBatcher.forEach { batch ->
                    batch.antallSlettedeRaderPerTabell.forEach { (tabell, antall) -> merge(tabell, antall, Long::plus) }
                }
            }
    }
}
