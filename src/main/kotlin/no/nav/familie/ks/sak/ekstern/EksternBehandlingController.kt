package no.nav.familie.ks.sak.ekstern

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/behandling"],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@ProtectedWithClaims(issuer = "azuread")
class EksternBehandlingController(
    private val tilgangService: TilgangService,
    private val klageServiceEkstern: KlageServiceEkstern
) {

    @GetMapping("kan-opprette-revurdering-klage/{fagsakId}")
    fun kanOppretteRevurdering(@PathVariable fagsakId: Long): Ressurs<KanOppretteRevurderingResponse> {
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsakId = fagsakId,
            handling = "Kan opprette revurdering fra klage på fagsak=$fagsakId",
            event = AuditLoggerEvent.CREATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER
        )

        if (!SikkerhetContext.kallKommerFraKlage()) {
            throw Feil("Kallet utføres ikke av en autorisert klient")
        }

        return Ressurs.success(klageServiceEkstern.kanOppretteRevurdering(fagsakId))
    }

    @PostMapping("opprett-revurdering-klage/{fagsakId}")
    fun opprettRevurderingKlage(@PathVariable fagsakId: Long): Ressurs<OpprettRevurderingResponse> {
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsakId = fagsakId,
            handling = "Opprett revurdering fra klage på fagsak=$fagsakId",
            event = AuditLoggerEvent.CREATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER
        )

        if (!SikkerhetContext.kallKommerFraKlage()) {
            throw Feil("Kallet utføres ikke av en autorisert klient")
        }
        return Ressurs.success(klageServiceEkstern.opprettRevurderingKlage(fagsakId))
    }
}
