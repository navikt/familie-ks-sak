package no.nav.familie.ks.sak.internal

import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.infotrygd.SøkerOgBarn
import no.nav.familie.ks.sak.internal.EndringKontantstøtte2024.DistribuerInformasjonsbrevKontantstøtteEndresInfotrygdService
import no.nav.familie.ks.sak.internal.EndringKontantstøtte2024.DistribuerInformasjonsbrevKontantstøtteEndresKSService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvalter")
@ProtectedWithClaims(issuer = "azuread")
class ForvalterController(
    private val testVerktøyService: TestVerktøyService,
    private val tilgangService: TilgangService,
    private val distribuerInformasjonsbrevKontantsøtteEndresService: DistribuerInformasjonsbrevKontantstøtteEndresKSService,
    private val distribuerInformasjonsbrevKontantstøtteEndresInfotrygdService: DistribuerInformasjonsbrevKontantstøtteEndresInfotrygdService,
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

        return testVerktøyService.hentBrevTest(behandlingId)
            .replace("\n", System.lineSeparator())
    }

    @PostMapping(path = ["/fagsaker/kjor-send-informasjonsbrev-endring-kontantstotte-ks"])
    fun sendInfobrevTilAlleMedBarnFødtEtterJanuar2023KS(
        @RequestBody erDryRun: Boolean = true,
    ): List<Long> {
        tilgangService.validerTilgangTilHandling(
            handling = "Sende informasjonsbrev om forkortet kontantstøtte til alle med barn født i 2023 eller senere",
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
        )

        return distribuerInformasjonsbrevKontantsøtteEndresService.opprettTaskerForÅJournalføreOgSendeUtInformasjonsbrevKontantstøtteendringKS(erDryRun)
    }

    @PostMapping(path = ["/fagsaker/kjor-send-informasjonsbrev-endring-kontantstotte-infotrygd"])
    fun sendInfobrevTilAlleMedBarnFødtEtterJanuar2023Infotrygd(
        @RequestBody erDryRun: Boolean = true,
    ): List<SøkerOgBarn> {
        tilgangService.validerTilgangTilHandling(
            handling = "Sende informasjonsbrev om forkortet kontantstøtte til alle med barn født i 2023 eller senere",
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
        )

        return distribuerInformasjonsbrevKontantstøtteEndresInfotrygdService.opprettTaskerForÅJournalføreOgSendeUtInformasjonsbrevKontantstøtteendringInfotrygd(
            erDryRun,
        )
    }
}
