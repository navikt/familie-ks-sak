package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.unleash.UnleashService

fun mockUnleashNextMedContextService(): UnleashNextMedContextService {
    val unleashNextMedContextService = mockk<UnleashNextMedContextService>()
    every { unleashNextMedContextService.isEnabled(any()) } returns true
    return unleashNextMedContextService
}

fun mockUnleashService(isEnabledDefault: Boolean = true): UnleashService {
    val unleashService = mockk<UnleashService>()
    every { unleashService.isEnabled(any()) } returns isEnabledDefault
    every { unleashService.isEnabled(any(), defaultValue = any()) } returns isEnabledDefault
    return unleashService
}
