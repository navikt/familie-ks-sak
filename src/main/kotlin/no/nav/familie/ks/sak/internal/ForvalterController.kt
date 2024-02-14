package no.nav.familie.ks.sak.internal

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.internal.endringKontantstøtteInfobrev2024.DistribuerInformasjonsbrevKontantstøtteEndresInfotrygdService
import no.nav.familie.ks.sak.internal.endringKontantstøtteInfobrev2024.DistribuerInformasjonsbrevKontantstøtteEndresKSService
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
    fun sendInfobrevTilAlleMedBarnFødtEtterAugust2022KS(
        @RequestBody kjøretype: Kjøretype = Kjøretype.DRY_RUN,
    ): List<Long> {
        tilgangService.validerTilgangTilHandling(
            handling = "Send informasjonsbrev om forkortet kontantstøtte til alle med barn født i september 2022 eller senere",
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
        )

        return distribuerInformasjonsbrevKontantsøtteEndresService
            .opprettTaskerForÅJournalføreOgSendeUtInformasjonsbrevKontantstøtteendringKS(erDryRun = kjøretype == Kjøretype.DRY_RUN)
    }

    @PostMapping(path = ["/fagsaker/hent-personer-informasjonsbrev-endring-kontantstotte-infotrygd"])
    fun hentSøkereMedBarnFødtEtterAugust2022Infotrygd(): Set<String> {
        tilgangService.validerTilgangTilHandling(
            handling = "Henter alle med barn født i september 2022 eller senere fra Infotrygd",
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
        )

        return distribuerInformasjonsbrevKontantstøtteEndresInfotrygdService.hentPersonerFraInfotrygdMedBarnFødEtterAugust22()
            .toSet()
    }

    @PostMapping(path = ["/fagsaker/kjor-send-informasjonsbrev-endring-kontantstotte-infotrygd"])
    fun sendInfobrevTilAlleMedBarnFødtEtterAugust2022Infotrygd(
        @RequestBody søkerIdenterFraInfotrygd: Set<String>,
    ) {
        tilgangService.validerTilgangTilHandling(
            handling = "Send informasjonsbrev om forkortet kontantstøtte",
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
        )

        return distribuerInformasjonsbrevKontantstøtteEndresInfotrygdService.opprettTaskerForÅJournalføreOgSendeUtInformasjonsbrevKontantstøtteendringInfotrygd(
            søkerIdenterFraInfotrygd,
        )
    }

    @PostMapping(path = ["/fagsaker/obs-hent-personer-og-send-informasjonsbrev-endring-kontantstotte-infotrygd"])
    fun hentOgSendInfobrevTilAlleMedBarnFødtEtterAugust2022Infotrygd(
        @RequestBody kjøretype: Kjøretype = Kjøretype.DRY_RUN,
    ) {
        if (kjøretype != Kjøretype.SEND_BREV) {
            throw Feil("Kjøretypen var ikke \"SEND_BREV\"")
        }

        tilgangService.validerTilgangTilHandling(
            handling = "Send informasjonsbrev om forkortet kontantstøtte til alle med barn født i september 2022 eller senere",
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
        )

        val søkerIdenterFraInfotrygd =
            distribuerInformasjonsbrevKontantstøtteEndresInfotrygdService
                .hentPersonerFraInfotrygdMedBarnFødEtterAugust22().toSet()

        return distribuerInformasjonsbrevKontantstøtteEndresInfotrygdService
            .opprettTaskerForÅJournalføreOgSendeUtInformasjonsbrevKontantstøtteendringInfotrygd(søkerIdenterFraInfotrygd)
    }
}

enum class Kjøretype {
    DRY_RUN,
    SEND_BREV,
}
