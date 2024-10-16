package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.cucumber.StepDefinition
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndelRepository

fun mockOvergangsordningAndelRepository(stepDefinition: StepDefinition): OvergangsordningAndelRepository =
    mockk<OvergangsordningAndelRepository>().apply {
        every { hentOvergangsordningAndelerForBehandling(any()) } answers {
            val behandlingId = firstArg<Long>()
            stepDefinition.overgangsordningAndeler[behandlingId] ?: emptyList()
        }
    }
