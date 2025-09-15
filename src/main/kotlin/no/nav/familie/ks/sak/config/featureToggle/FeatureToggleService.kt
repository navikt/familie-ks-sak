package no.nav.familie.ks.sak.config.featureToggle

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.unleash.UnleashContextFields
import no.nav.familie.unleash.UnleashService
import org.springframework.stereotype.Service

@Service
class FeatureToggleService(
    private val unleashService: UnleashService,
    private val behandlingRepository: BehandlingRepository,
    private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
) {
    fun isEnabled(
        toggle: FeatureToggle,
        behandlingId: Long,
    ): Boolean {
        val behandling = behandlingRepository.hentBehandling(behandlingId)

        return unleashService.isEnabled(
            toggle.navn,
            properties =
                mapOf(
                    UnleashContextFields.FAGSAK_ID to behandling.fagsak.id.toString(),
                    UnleashContextFields.BEHANDLING_ID to behandling.id.toString(),
                    UnleashContextFields.ENHET_ID to
                        (
                            arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandlingId)
                                ?: throw Feil("Finner ikke tilknyttet arbeidsfordeling på behandling med id $behandlingId")
                        ).behandlendeEnhetId,
                    UnleashContextFields.NAV_IDENT to SikkerhetContext.hentSaksbehandler(),
                    UnleashContextFields.EPOST to SikkerhetContext.hentSaksbehandlerEpost(),
                ),
        )
    }

    fun isEnabledForFagsak(
        fagsakId: Long,
        toggle: FeatureToggle,
    ): Boolean =
        unleashService.isEnabled(
            toggle.navn,
            properties =
                mapOf(
                    UnleashContextFields.FAGSAK_ID to fagsakId.toString(),
                ),
        )

    fun isEnabled(toggle: FeatureToggle): Boolean =
        unleashService.isEnabled(
            toggle.navn,
            properties =
                mapOf(
                    UnleashContextFields.NAV_IDENT to SikkerhetContext.hentSaksbehandler(),
                    UnleashContextFields.EPOST to SikkerhetContext.hentSaksbehandlerEpost(),
                ),
        )

    fun isEnabled(
        toggle: FeatureToggle,
        defaultValue: Boolean,
    ): Boolean = unleashService.isEnabled(toggle.navn, defaultValue)

    fun isEnabled(toggleId: String) =
        unleashService.isEnabled(
            toggleId,
            properties =
                mapOf(
                    UnleashContextFields.NAV_IDENT to SikkerhetContext.hentSaksbehandler(),
                    UnleashContextFields.EPOST to SikkerhetContext.hentSaksbehandlerEpost(),
                ),
        )
}
