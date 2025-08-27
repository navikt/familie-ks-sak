package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService

fun mockUnleashNextMedContextService(isEnabledDefault: Boolean = true): FeatureToggleService {
    val featureToggleService = mockk<FeatureToggleService>()
    every { featureToggleService.isEnabled(any<String>()) } returns isEnabledDefault
    every { featureToggleService.isEnabled(any<FeatureToggle>()) } returns isEnabledDefault
    return featureToggleService
}
