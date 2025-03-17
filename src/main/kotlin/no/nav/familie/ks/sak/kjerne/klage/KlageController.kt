package no.nav.familie.ks.sak.kjerne.klage

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.klage.dto.OpprettKlageDto
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// Kalles av ks-sak-frontend
@RestController
@RequestMapping(path = ["/api/fagsaker"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class KlageController(
    private val tilgangService: TilgangService,
    private val klageService: KlageService,
    private val fagsakService: FagsakService,
) {
    @PostMapping("/{fagsakId}/opprett-klagebehandling")
    fun opprettKlage(
        @PathVariable fagsakId: Long,
        @RequestBody opprettKlageDto: OpprettKlageDto,
    ): Ressurs<Unit> {
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsakId = fagsakId,
            event = AuditLoggerEvent.CREATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Vis og lagre vedtaksbrev",
        )
        return Ressurs.success(klageService.opprettKlage(fagsak = fagsakService.hentFagsak(fagsakId), kravMottattDato = opprettKlageDto.kravMottattDato))
    }

    @GetMapping("/{fagsakId}/hent-klagebehandlinger")
    fun hentKlagebehandlinger(
        @PathVariable fagsakId: Long,
    ): Ressurs<List<KlagebehandlingDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsakId = fagsakId,
            event = AuditLoggerEvent.ACCESS,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Vis og lagre vedtaksbrev",
        )
        return Ressurs.success(klageService.hentKlagebehandlingerPåFagsak(fagsakId))
    }
}
