package no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.ks.sak.barnehagelister.epost.EpostService
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class BarnehagelisteVarslingService(
    val barnehageBarnService: BarnehagebarnService,
    val epostService: EpostService,
) {
    @Scheduled(cron = "0 0 6 * * ?")
    fun sendVarslingOmBarnehagelisteHverMorgenKl6() {
        if (LeaderClient.isLeader() != true) {
            return
        }
        sendVarslingOmNyBarnehagelisteTilEnhet()
    }

    fun sendVarslingOmNyBarnehagelisteTilEnhet(
        dryRun: Boolean = false,
        dryRunEpost: String = "",
    ) {
        logger.info("Sjekker om det er kommet inn barnehagelister ila siste døgn.")

        val kommunerSendtForFørsteGangSisteDøgn = barnehageBarnService.finnKommunerSendtInnSisteDøgn()

        if (kommunerSendtForFørsteGangSisteDøgn.isNotEmpty()) {
            logger.info("Sender epost for nye kommuner i barnehagelister: $kommunerSendtForFørsteGangSisteDøgn")

            if (dryRun) {
                epostService.sendEpostVarslingBarnehagelister(dryRunEpost, kommunerSendtForFørsteGangSisteDøgn.map { it.navn }.toSet())
            } else {
                epostService.sendEpostVarslingBarnehagelister(`KONTAKT_E-POST_BERGEN`, kommunerSendtForFørsteGangSisteDøgn.map { it.navn }.toSet())
            }
        }
    }

    companion object {
        val `KONTAKT_E-POST_BERGEN` = "kontantstotte@nav.no"

        private val logger = LoggerFactory.getLogger(BarnehagelisteVarslingService::class.java)
    }
}

data class KommuneEllerBydel(
    val nummer: String,
    val navn: String,
)
