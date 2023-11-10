package no.nav.familie.ks.sak.config.featureToggle

import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.unleash.UnleashContextFields
import no.nav.familie.unleash.UnleashService
import org.springframework.stereotype.Service

@Service
class UnleashNextMedContextService(
    private val unleashService: UnleashService,
    private val behandlingService: BehandlingService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
) {
    fun isEnabled(
        toggleId: String,
        behandlingId: Long,
    ): Boolean {
        val behandling = behandlingService.hentBehandling(behandlingId)

        return unleashService.isEnabled(
            toggleId,
            properties =
                mapOf(
                    UnleashContextFields.FAGSAK_ID to behandling.fagsak.id.toString(),
                    UnleashContextFields.BEHANDLING_ID to behandling.id.toString(),
                    UnleashContextFields.ENHET_ID to arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetId,
                    UnleashContextFields.NAV_IDENT to SikkerhetContext.hentSaksbehandler(),
                    UnleashContextFields.EPOST to SikkerhetContext.hentSaksbehandlerEpost(),
                ),
        )
    }

    fun isEnabled(toggleId: String): Boolean {
        return unleashService.isEnabled(
            toggleId,
            properties =
                mapOf(
                    UnleashContextFields.NAV_IDENT to SikkerhetContext.hentSaksbehandler(),
                    UnleashContextFields.EPOST to SikkerhetContext.hentSaksbehandlerEpost(),
                ),
        )
    }
}
