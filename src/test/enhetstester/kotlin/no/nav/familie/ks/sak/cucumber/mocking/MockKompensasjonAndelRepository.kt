package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.cucumber.StepDefinition
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.KompensasjonAndelRepository

fun mockKompensasjonAndelRepository(stepDefinition: StepDefinition): KompensasjonAndelRepository =
    mockk<KompensasjonAndelRepository>().apply {
        every { hentKompensasjonAndelerForBehandling(any()) } answers {
            val behandlingId = firstArg<Long>()
            stepDefinition.kompensasjonAndeler[behandlingId] ?: emptyList()
        }
    }
