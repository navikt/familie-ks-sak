package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.cucumber.StepDefinition
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository

fun mockEndretUtbetalingAndelRepository(stepDefinition: StepDefinition): EndretUtbetalingAndelRepository {
    val endretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()
    every { endretUtbetalingAndelRepository.hentEndretUtbetalingerForBehandling(any()) } answers {
        val behandlingId = firstArg<Long>()
        stepDefinition.endredeUtbetalinger[behandlingId] ?: emptyList()
    }
    return endretUtbetalingAndelRepository
}
