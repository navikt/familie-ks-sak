package no.nav.familie.ks.sak.api

import no.nav.familie.eksterne.kontrakter.VedtakDVHV2
import no.nav.familie.ks.sak.integrasjon.statistikk.StatistikkClient
import no.nav.familie.ks.sak.statistikk.stønadsstatistikk.PubliserVedtakV2Task
import no.nav.familie.ks.sak.statistikk.stønadsstatistikk.StønadsstatistikkService
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/stonadsstatistikk")
@ProtectedWithClaims(issuer = "azuread")
class StønadsstatistikkController(
    private val stønadsstatistikkService: StønadsstatistikkService,
    private val taskService: TaskService,
    private val statistikkClient: StatistikkClient,
) {

    private val logger = LoggerFactory.getLogger(StønadsstatistikkController::class.java)

    @PostMapping(path = ["/vedtakV2"])
    fun hentVedtakDvhV2(@RequestBody(required = true) behandlinger: List<Long>): List<VedtakDVHV2> {
        try {
            return behandlinger.map { stønadsstatistikkService.hentVedtakV2(it) }
        } catch (e: Exception) {
            logger.warn("Feil ved henting av stønadsstatistikk V2 for $behandlinger", e)
            throw e
        }
    }

    @PostMapping(path = ["/send-til-dvh"])
    fun sendTilStønadsstatistikk(@RequestBody(required = true) behandlinger: List<Long>) {
        behandlinger.forEach {
            if (!statistikkClient.harSendtVedtaksmeldingForBehandling(it)) {
                val vedtakV2DVH = stønadsstatistikkService.hentVedtakV2(it)
                val vedtakV2Task = PubliserVedtakV2Task.opprettTask(vedtakV2DVH.personV2.personIdent, it)
                taskService.save(vedtakV2Task)
            }
        }
    }

    @PostMapping(path = ["/send-til-dvh-manuell"])
    fun sendTilStønadsstatistikkManuell(@RequestBody(required = true) behandlinger: List<Long>) {
        behandlinger.forEach {
            val vedtakV2DVH = stønadsstatistikkService.hentVedtakV2(it)
            val vedtakV2Task = PubliserVedtakV2Task.opprettTask(vedtakV2DVH.personV2.personIdent, it)
            taskService.save(vedtakV2Task)
        }
    }
}
