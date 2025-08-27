package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
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
    fun validerHentSammensattKontrollsakTilgang() {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)) {
            throw FunksjonellFeil(
                melding = "Mangler tilgang for å hente sammensatt kontrollsak.",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }

        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Hent SammensattKontrollsak",
        )
    }

    fun validerOpprettSammensattKontrollsakTilgang() {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)) {
            throw FunksjonellFeil(
                melding = "Mangler tilgang for å opprette sammensatt kontrollsak.",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }

        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Opprett SammensattKontrollsak",
        )
    }

    fun validerOppdaterSammensattKontrollsakTilgang() {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)) {
            throw FunksjonellFeil(
                melding = "Mangler tilgang for å oppdatere sammensatt kontrollsak.",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }

        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater SammensattKontrollsak",
        )
    }

    fun validerSlettSammensattKontrollsakTilgang() {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)) {
            throw FunksjonellFeil(
                melding = "Mangler tilgang for å slette sammensatt kontrollsak.",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }

        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Slett SammensattKontrollsak",
        )
    }

    fun validerRedigerbarBehandlingForSammensattKontrollsakId(
        sammensattKontrollsakId: Long,
    ) {
        val sammensattKontrollsak =
            sammensattKontrollsakService.finnSammensattKontrollsak(
                sammensattKontrollsakId = sammensattKontrollsakId,
            ) ?: throw FunksjonellFeil(
                melding = "Fant ingen sammensatt kontrollsak for id=$sammensattKontrollsakId.",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        validerRedigerbarBehandlingForBehandlingId(sammensattKontrollsak.behandlingId)
    }

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
