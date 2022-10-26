package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
@ProtectedWithClaims(issuer = "azuread")
@Validated
@Profile("dev", "postgres", "preprod", "dev-postgres-preprod")
class TestController(private val tilgangService: TilgangService, private val integrasjonClient: IntegrasjonClient) {

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
}
