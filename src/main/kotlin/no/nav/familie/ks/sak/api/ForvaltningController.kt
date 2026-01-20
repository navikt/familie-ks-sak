package no.nav.familie.ks.sak.api

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.api.dto.ManuellStartKonsistensavstemmingDto
import no.nav.familie.ks.sak.api.dto.OpprettAutovedtakBehandlingPåFagsakDto
import no.nav.familie.ks.sak.api.dto.OpprettOppgaveDto
import no.nav.familie.ks.sak.barnehagelister.BarnehageListeService
import no.nav.familie.ks.sak.barnehagelister.BarnehagebarnService
import no.nav.familie.ks.sak.barnehagelister.BarnehagelisteVarslingService
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnVisningDto
import no.nav.familie.ks.sak.common.EnvService
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.SpringProfile
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.integrasjon.ecb.ECBService
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.integrasjon.oppdrag.AvstemmingKlient
import no.nav.familie.ks.sak.internal.TestVerktøyService
import no.nav.familie.ks.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ks.sak.kjerne.avstemming.GrensesnittavstemmingTask
import no.nav.familie.ks.sak.kjerne.avstemming.KonsistensavstemmingKjøreplanService
import no.nav.familie.ks.sak.kjerne.avstemming.KonsistensavstemmingTask
import no.nav.familie.ks.sak.kjerne.avstemming.domene.KonsistensavstemmingTaskDto
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.personident.PatchMergetIdentDto
import no.nav.familie.ks.sak.kjerne.personident.PatchMergetIdentTask
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import no.nav.familie.ks.sak.statistikk.stønadsstatistikk.PubliserVedtakTask
import no.nav.familie.ks.sak.statistikk.stønadsstatistikk.StønadsstatistikkService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.data.domain.Page
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/forvaltning/")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ForvaltningController(
    private val tilgangService: TilgangService,
    private val integrasjonKlient: IntegrasjonKlient,
    private val sakStatistikkService: SakStatistikkService,
    private val stønadsstatistikkService: StønadsstatistikkService,
    private val taskService: TaskRepositoryWrapper,
    private val konsistensavstemmingKjøreplanService: KonsistensavstemmingKjøreplanService,
    private val personidentService: PersonidentService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val barnehageListeService: BarnehageListeService,
    private val environment: Environment,
    private val ecbService: ECBService,
    private val behandlingRepository: BehandlingRepository,
    private val testVerktøyService: TestVerktøyService,
    private val envService: EnvService,
    private val autovedtakService: AutovedtakService,
    private val barnehagebarnService: BarnehagebarnService,
    private val barnehagelisteVarslingService: BarnehagelisteVarslingService,
    private val avstemmingKlient: AvstemmingKlient,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(ForvaltningController::class.java)

    @PostMapping("/journalfør-søknad/{fnr}")
    fun opprettJournalføringOppgave(
        @PathVariable fnr: String,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "teste journalføring av innkommende søknad for opprettelse av journalføring oppgave",
        )
        val arkiverDokumentRequest =
            ArkiverDokumentRequest(
                fnr = fnr,
                forsøkFerdigstill = false,
                hoveddokumentvarianter =
                    listOf(
                        Dokument(
                            dokument = ByteArray(10),
                            dokumenttype = Dokumenttype.KONTANTSTØTTE_SØKNAD,
                            filtype = Filtype.PDFA,
                        ),
                    ),
            )
        val journalførDokumentResponse = integrasjonKlient.journalførDokument(arkiverDokumentRequest)
        return ResponseEntity.ok(Ressurs.success(journalførDokumentResponse.journalpostId, "Dokument er Journalført"))
    }

    @PostMapping("/opprett-oppgave")
    fun opprettOppgave(
        @RequestBody opprettOppgaveDto: OpprettOppgaveDto,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "teste opprettelse av oppgave",
        )
        val aktørId = personidentService.hentAktør(opprettOppgaveDto.fnr).aktørId
        val opprettOppgaveRequest =
            OpprettOppgaveRequest(
                ident = OppgaveIdentV2(ident = aktørId, gruppe = IdentGruppe.AKTOERID),
                saksId = null,
                journalpostId = opprettOppgaveDto.journalpostId,
                tema = Tema.KON,
                oppgavetype = opprettOppgaveDto.oppgavetype,
                fristFerdigstillelse = LocalDate.now().plusDays(1),
                beskrivelse = opprettOppgaveDto.beskrivelse,
                enhetsnummer = opprettOppgaveDto.enhet,
                behandlingstema = opprettOppgaveDto.behandlingstema,
                behandlingstype = opprettOppgaveDto.behandlingstype,
                tilordnetRessurs = opprettOppgaveDto.tilordnetRessurs,
            )
        integrasjonKlient.opprettOppgave(opprettOppgaveRequest)
        return ResponseEntity.ok(Ressurs.success("Oppgave opprettet"))
    }

    @GetMapping("/dvh/sakstatistikk/send-alle-behandlinger-til-dvh")
    fun sendAlleBehandlingerTilDVH(): ResponseEntity<Ressurs<String>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "teste sending av siste tilstand for alle behandlinger til DVH",
        )
        sakStatistikkService.sendAlleBehandlingerTilDVH()
        return ResponseEntity.ok(Ressurs.success(":)", "Alle behandlinger er sendt"))
    }

    @PostMapping(path = ["/dvh/stønadsstatistikk/vedtak"])
    fun hentVedtakDVH(
        @RequestBody(required = true) behandlinger: List<Long>,
    ): List<VedtakDVH> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Hente Vedtak DVH",
        )

        try {
            return behandlinger.map { stønadsstatistikkService.hentVedtakDVH(it) }
        } catch (e: Exception) {
            logger.warn("Feil ved henting av stønadsstatistikk V2 for $behandlinger", e)
            throw e
        }
    }

    @PostMapping(path = ["/dvh/stønadsstatistikk/send-til-dvh-manuell"])
    fun sendTilStønadsstatistikkManuell(
        @RequestBody(required = true) behandlinger: List<Long>,
    ) {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Sender vedtakDVH til stønadsstatistikk manuelt",
        )

        behandlinger.forEach {
            val vedtakDVH = stønadsstatistikkService.hentVedtakDVH(it)
            val vedtakTask = PubliserVedtakTask.opprettTask(vedtakDVH.person.personIdent, it)
            taskService.save(vedtakTask)
        }
    }

    @PostMapping(path = ["/avstemming/send-grensesnittavstemming-manuell"])
    fun sendGrensesnittavstemmingManuell(
        @RequestBody(required = true) periode: Periode,
    ) {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Kjører grensesnittavstemming manuelt",
        )
        taskService.save(
            GrensesnittavstemmingTask.opprettTask(periode.fom.atStartOfDay(), periode.tom.atStartOfDay()),
        )
    }

    @PostMapping(path = ["/avstemming/send-konsistensavstemming-manuell"])
    fun sendKonsistensavstemmingManuell(
        @RequestBody(required = true)
        manuellStartKonsistensavstemmingDto: ManuellStartKonsistensavstemmingDto,
    ) {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Kjører konsistensavstemming manuelt",
        )

        val manuellKjøreplan = konsistensavstemmingKjøreplanService.leggTilManuellKjøreplan()
        taskService.save(
            KonsistensavstemmingTask.opprettTask(
                KonsistensavstemmingTaskDto(
                    kjøreplanId = manuellKjøreplan.id,
                    initieltKjøreTidspunkt = manuellStartKonsistensavstemmingDto.triggerTid,
                ),
            ),
        )
    }

    @PutMapping(path = ["/{behandlingId}/fyll-ut-vilkarsvurdering"])
    fun fyllUtVilkårsvurdering(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<String>> {
        val activeProfiles = environment.activeProfiles.map { it.trim() }.toSet()
        val erProd = SpringProfile.Prod.navn in activeProfiles
        val erDevPostgresPreprod = SpringProfile.DevPostgresPreprod.navn in activeProfiles
        val erPreprod = SpringProfile.Preprod.navn in activeProfiles

        if (erProd) {
            throw Feil("Skal ikke være tilgjengelig i prod")
        } else if (!erDevPostgresPreprod && !erPreprod) {
            throw Feil("Skal bare være tilgjengelig i for preprod eller lokalt")
        }

        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Fyll ut vilkårsvurderingen automatisk",
        )

        vilkårsvurderingService.fyllUtVilkårsvurdering(behandlingId)

        return ResponseEntity.ok(Ressurs.success("Oppdaterte vilkårsvurdering"))
    }

    @GetMapping("/barnehageliste/lesOgArkiver/{uuid}")
    fun lesOgArkiverBarnehageliste(
        @PathVariable uuid: String,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "teste lesing og arkivering av barnehageliste",
        )
        barnehageListeService.lesOgArkiverBarnehageliste(UUID.fromString(uuid))
        return ResponseEntity.ok(Ressurs.success(":)", "Barnehagliste lest og arkivert"))
    }

    @GetMapping("/barnehageliste/hentUarkvierteBarnehagelisteUuider")
    fun hentUarkiverteBarnehagelisteUuider(): ResponseEntity<Ressurs<List<String>>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "hente ut liste av uarkiverte barnehageliste uuid",
        )
        val uuidList = barnehageListeService.hentUarkiverteBarnehagelisteUuider()
        return ResponseEntity.ok(Ressurs.success(uuidList, "OK"))
    }

    @PostMapping(
        path = ["/barnehageliste/hentAlleBarnehagebarnPage"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentAlleBarnehagebarnPage(
        @RequestBody(required = true) barnehagebarnRequestParams: BarnehagebarnRequestParams,
    ): ResponseEntity<Ressurs<Page<BarnehagebarnVisningDto>>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "hente ut alle barnehagebarn",
        )

        val alleBarnehagebarnPage = barnehagebarnService.hentBarnehagebarnForVisning(barnehagebarnRequestParams)

        return ResponseEntity.ok(Ressurs.success(alleBarnehagebarnPage, "OK"))
    }

    @GetMapping("/hentValutakurs/")
    fun hentValutakurs(
        @RequestParam valuta: String,
        @RequestParam dato: LocalDate,
    ): ResponseEntity<BigDecimal> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "hentValutakurs",
        )
        return ResponseEntity.ok(ecbService.hentValutakurs(valuta, dato))
    }

    @GetMapping(path = ["/behandling/{behandlingId}/begrunnelsetest"])
    fun hentBegrunnelsetestPåBehandling(
        @PathVariable behandlingId: Long,
    ): String {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.ACCESS,
            handling = "hente data til test",
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
        )

        return testVerktøyService
            .hentBrevTest(behandlingId)
            .replace("\n", System.lineSeparator())
    }

    @Operation(
        summary = "Endepunkt for å teste trege http kall",
        description =
            "Dette endepunktet kaller et endepunkt som vil sove x antall sekunder i familie-oppdrag",
    )
    @Deprecated("Kan slettes når spring er fikset med httpclient5")
    @PostMapping(path = ["/testTregtEndepunktOppdrag"])
    fun sov(
        @RequestParam sekunder: Long,
        @RequestParam antallGanger: Long,
    ): String {
        var result = "OK"
        repeat(antallGanger.toInt()) { i ->
            try {
                avstemmingKlient.sov(sekunder)
                logger.info("testTregtEndepunktOppdrag kjørte ok #${i + 1}")
            } catch (e: Exception) {
                logger.error("testTregtEndepunktOppdrag feilet #${i + 1}", e)
                result = "FAILED"
            }
        }
        return result
    }

    @PostMapping("/opprettAutovedtakBehandlingPaaFagsak")
    fun opprettAutovedtakBehandlingPåFagsak(
        @RequestBody opprettAutovedtakBehandlingPåFagsakDto: OpprettAutovedtakBehandlingPåFagsakDto,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Opprett autovedtak behandling på fagsak",
        )

        val fagsakId = opprettAutovedtakBehandlingPåFagsakDto.fagsakId

        autovedtakService.opprettAutovedtakBehandlingPåFagsak(
            fagsakId = fagsakId,
            behandlingÅrsak = opprettAutovedtakBehandlingPåFagsakDto.behandlingsÅrsak,
            behandlingType = opprettAutovedtakBehandlingPåFagsakDto.behandlingType,
        )

        return ResponseEntity.ok(
            Ressurs.success(
                "Automatisk revurdering på fagsak $fagsakId opprettet OK",
            ),
        )
    }

    @PatchMapping("/patch-fagsak-med-ny-ident")
    fun patchMergetIdent(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description =
                "skalSjekkeAtGammelIdentErHistoriskAvNyIdent - Sjekker at " +
                    "gammel ident er historisk av ny. Hvis man ønsker å patche med en ident hvor den gamle ikke er historisk av ny, så settes " +
                    "denne til false. OBS: Du må da være sikker på at identen man ønsker å patche til er samme person. Dette kan skje hvis " +
                    "identen ikke er merget av folketrygden.",
        )
        @RequestBody
        @Valid
        patchMergetIdentDto: PatchMergetIdentDto,
    ): ResponseEntity<String> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Patch merget ident",
        )

        val task = PatchMergetIdentTask.opprettTask(patchMergetIdentDto)

        taskService.save(task)

        return ResponseEntity.ok("ok")
    }

    @GetMapping("/redirect/behandling/{behandlingId}")
    @Unprotected
    fun redirectTilKontantstøtte(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Any> {
        val hostname =
            if (envService.erLokal()) {
                "http://localhost:8000"
            } else if (envService.erPreprod()) {
                "https://kontantstotte.ansatt.dev.nav.no"
            } else if (envService.erProd()) {
                "https://kontantstotte.intern.nav.no"
            } else {
                throw Feil("Klarer ikke å utlede miljø for redirect til fagsak")
            }
        val behandling = behandlingRepository.hentBehandlingNullable(behandlingId)
        return if (behandling == null) {
            ResponseEntity.status(200).body("Fant ikke behandling med id $behandlingId")
        } else {
            ResponseEntity
                .status(302)
                .location(URI.create("$hostname/fagsak/${behandling.fagsak.id}/$behandlingId/"))
                .build()
        }
    }

    @PostMapping("/barnehagelister/dry-run-e-post-varsel")
    fun kjørDryRunBarnehagelister(
        @RequestBody dryRunEpost: String,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Kjør dry-run e-postvarsel nye barnehagelister",
        )
        barnehagelisteVarslingService.sendVarslingOmNyBarnehagelisteTilEnhet(dryRun = true, dryRunEpost = dryRunEpost)

        return ResponseEntity.ok(Ressurs.success("OK"))
    }
}
