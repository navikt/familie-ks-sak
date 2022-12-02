package no.nav.familie.ks.sak.api

import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.avstemming.GrensesnittavstemmingTask
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import no.nav.familie.ks.sak.statistikk.stønadsstatistikk.PubliserVedtakTask
import no.nav.familie.ks.sak.statistikk.stønadsstatistikk.StønadsstatistikkService
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/forvaltning/")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ForvaltningController(
    private val tilgangService: TilgangService,
    private val integrasjonClient: IntegrasjonClient,
    private val sakStatistikkService: SakStatistikkService,
    private val stønadsstatistikkService: StønadsstatistikkService,
    private val taskService: TaskService
) {

    private val logger = LoggerFactory.getLogger(ForvaltningController::class.java)

    @PostMapping("/journalfør-søknad/{fnr}")
    fun opprettJournalføringOppgave(@PathVariable fnr: String): ResponseEntity<Ressurs<String>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "teste journalføring av innkommende søknad for opprettelse av journalføring oppgave"
        )
        val arkiverDokumentRequest = ArkiverDokumentRequest(
            fnr = fnr,
            forsøkFerdigstill = false,
            hoveddokumentvarianter = listOf(
                Dokument(
                    dokument = ByteArray(10),
                    dokumenttype = Dokumenttype.KONTANTSTØTTE_SØKNAD,
                    filtype = Filtype.PDFA
                )
            )
        )
        val journalførDokumentResponse = integrasjonClient.journalførDokument(arkiverDokumentRequest)
        return ResponseEntity.ok(Ressurs.success(journalførDokumentResponse.journalpostId, "Dokument er Journalført"))
    }

    @GetMapping("/dvh/sakstatistikk/send-alle-behandlinger-til-dvh")
    fun sendAlleBehandlingerTilDVH(): ResponseEntity<Ressurs<String>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "teste sending av siste tilstand for alle behandlinger til DVH"
        )
        sakStatistikkService.sendAlleBehandlingerTilDVH()
        return ResponseEntity.ok(Ressurs.success(":)", "Alle behandlinger er sendt"))
    }

    @PostMapping(path = ["/dvh/stønadsstatistikk/vedtak"])
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

    @PostMapping(path = ["/dvh/stønadsstatistikk/send-til-dvh-manuell"])
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

    @PostMapping(path = ["/avstemming/send-grensesnittavstemming-manuell"])
    fun sendGrensesnittavstemmingManuell(
        @RequestBody(required = true) fom: LocalDateTime,
        @RequestBody(required = true) tom: LocalDateTime
    ) {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Sender vedtakDVH til stønadsstatistikk manuelt"
        )
        taskService.save(GrensesnittavstemmingTask.opprettTask(fom, tom))
    }
}
