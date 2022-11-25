package no.nav.familie.ks.sak.api

import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.familie.ks.sak.statistikk.stønadsstatistikk.PubliserVedtakTask
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
    private val tilgangService: TilgangService,
    private val taskService: TaskService
) {

    private val logger = LoggerFactory.getLogger(StønadsstatistikkController::class.java)

    @PostMapping(path = ["/vedtak"])
    fun hentVedtakDVH(@RequestBody(required = true) behandlinger: List<Long>): List<VedtakDVH> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Hente Vedtak DVH"
        )

        try {
            return behandlinger.map { stønadsstatistikkService.hentVedtakDVH(it) }
        } catch (e: Exception) {
            logger.warn("Feil ved henting av stønadsstatistikk V2 for $behandlinger", e)
            throw e
        }
    }

    @PostMapping(path = ["/send-til-dvh-manuell"])
    fun sendTilStønadsstatistikkManuell(@RequestBody(required = true) behandlinger: List<Long>) {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Sender vedtakDVH til stønadsstatistikk manuelt"
        )

        behandlinger.forEach {
            val vedtakDVH = stønadsstatistikkService.hentVedtakDVH(it)
            val vedtakTask = PubliserVedtakTask.opprettTask(vedtakDVH.person.personIdent, it)
            taskService.save(vedtakTask)
        }
    }
}
