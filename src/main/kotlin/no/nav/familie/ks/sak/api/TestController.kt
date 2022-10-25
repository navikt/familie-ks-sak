package no.nav.familie.ks.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
@Unprotected
class TestController(private val tilgangService: TilgangService, private val integrasjonClient: IntegrasjonClient) {
    @PostMapping("/journalfør-søknad/{fnr}")
    fun opprettJournalføringOppgave(@PathVariable fnr: String): ResponseEntity<Ressurs<String>> {
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "journalføring av innkommende søknad"
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
