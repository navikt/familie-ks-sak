package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class SammensattKontrollsakValidator(
    private val featureToggleService: FeatureToggleService,
    private val tilgangService: TilgangService,
    private val sammensattKontrollsakService: SammensattKontrollsakService,
    private val behandlingService: BehandlingService,
) {
    fun validerHentSammensattKontrollsakTilgang(behandlingId: Long) {
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.ACCESS,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Hent SammensattKontrollsak",
        )
    }

    fun validerOpprettSammensattKontrollsakTilgang(behandlingId: Long) {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)) {
            throw FunksjonellFeil(
                melding = "Mangler tilgang for å opprette sammensatt kontrollsak.",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }

        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.CREATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Opprett SammensattKontrollsak",
        )
    }

    fun validerOppdaterSammensattKontrollsakTilgang(sammensattKontrollsakId: Long) {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)) {
            throw FunksjonellFeil(
                melding = "Mangler tilgang for å oppdatere sammensatt kontrollsak.",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }

        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = hentSammensattKontrollsak(sammensattKontrollsakId).behandlingId,
            event = AuditLoggerEvent.UPDATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater SammensattKontrollsak",
        )
    }

    fun validerSlettSammensattKontrollsakTilgang(sammensattKontrollsakId: Long) {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)) {
            throw FunksjonellFeil(
                melding = "Mangler tilgang for å slette sammensatt kontrollsak.",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }

        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandlingId = hentSammensattKontrollsak(sammensattKontrollsakId).behandlingId,
            event = AuditLoggerEvent.DELETE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Slett SammensattKontrollsak",
        )
    }

    fun validerRedigerbarBehandlingForSammensattKontrollsakId(
        sammensattKontrollsakId: Long,
    ) {
        validerRedigerbarBehandlingForBehandlingId(hentSammensattKontrollsak(sammensattKontrollsakId).behandlingId)
    }

    private fun hentSammensattKontrollsak(sammensattKontrollsakId: Long): SammensattKontrollsak =
        sammensattKontrollsakService.finnSammensattKontrollsak(
            sammensattKontrollsakId = sammensattKontrollsakId,
        ) ?: throw FunksjonellFeil(
            melding = "Fant ingen sammensatt kontrollsak for id=$sammensattKontrollsakId.",
            httpStatus = HttpStatus.BAD_REQUEST,
        )

    fun validerRedigerbarBehandlingForBehandlingId(
        behandlingId: Long,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (!behandling.erRedigerbar()) {
            throw FunksjonellFeil(
                melding = "Behandlingen er låst for videre redigering (${behandling.status})",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }
}
