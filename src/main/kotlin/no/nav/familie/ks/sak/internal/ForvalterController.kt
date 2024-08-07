package no.nav.familie.ks.sak.internal

import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.internal.kontantstøtteInfobrevJuli2024.DistribuerInformasjonsbrevKontantstøtteJuli2024
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvalter")
@ProtectedWithClaims(issuer = "azuread")
class ForvalterController(
    private val testVerktøyService: TestVerktøyService,
    private val tilgangService: TilgangService,
    private val distribuerInformasjonsbrevKontantstøtteJuli2024: DistribuerInformasjonsbrevKontantstøtteJuli2024,
) {
    private val logger: Logger = LoggerFactory.getLogger(ForvalterController::class.java)

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

    @PostMapping(path = ["/fagsaker/kjor-send-informasjonsbrev-juli-2024"])
    fun sendInformasjonsBrevJuli2024TilFagsakSomErTruffet() {
        tilgangService.validerTilgangTilHandling(
            handling = "Send informasjonsbrev til alle fagsak som skal motta informasjonsbrev juli 2024",
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
        )

        logger.info("Kaller kjor-send-informasjonsbrev-juli-2024")
        distribuerInformasjonsbrevKontantstøtteJuli2024.opprettTaskerForÅJournalføreOgSendeUtInformasjonsbrevKontantstøttJuli2024()
    }

    @PostMapping(path = ["/fagsaker/hent-fagsak-id-send-informasjonsbrev-juli-2024"])
    fun hentAlleFagsakIdSomDetSkalSendesBrevTil(): Set<Long> {
        tilgangService.validerTilgangTilHandling(
            handling = "Henter alle fagsak som skal motta informasjonsbrev juli 2024",
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
        )

        logger.info("Kaller fagsaker/hent-personer-informasjonsbrev-endring-kontantstotte-infotrygd")

        return distribuerInformasjonsbrevKontantstøtteJuli2024.hentAlleFagsakIdSomDetSkalSendesBrevTil().toSet()
    }
}
