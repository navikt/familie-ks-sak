package no.nav.familie.ks.sak.fake

import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.unleash.UnleashService
import java.util.concurrent.ConcurrentHashMap

class FakeFeatureToggleService(
    unleashService: UnleashService,
    behandlingRepository: BehandlingRepository,
    arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
) : FeatureToggleService(
        unleashService = unleashService,
        behandlingRepository = behandlingRepository,
        arbeidsfordelingPåBehandlingRepository = arbeidsfordelingPåBehandlingRepository,
    ) {
    private val overrides = mutableMapOf<String, Boolean>()

    fun set(
        toggle: FeatureToggle,
        enabled: Boolean,
    ) {
        overrides[toggle.navn] = enabled
    }

    fun reset() {
        overrides.clear()
    }

    override fun isEnabled(toggleId: String): Boolean {
        val mockUnleashServiceAnswer = System.getProperty("mockFeatureToggleAnswer")?.toBoolean() ?: true
        return overrides[toggleId] ?: mockUnleashServiceAnswer
    }

    override fun isEnabled(
        toggle: FeatureToggle,
    ): Boolean = isEnabled(toggle.navn)

    override fun isEnabled(
        toggle: FeatureToggle,
        defaultValue: Boolean,
    ): Boolean = isEnabled(toggle.navn)

    override fun isEnabled(
        toggle: FeatureToggle,
        behandlingId: Long,
    ): Boolean = isEnabled(toggle.navn)
}
