package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.cucumber.StepDefinition
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository

fun mockFagsakRepository(stepDefinition: StepDefinition): FagsakRepository {
    val fagsakRepository = mockk<FagsakRepository>()
    every { fagsakRepository.finnFagsak(any()) } answers {
        val id = firstArg<Long>()
        stepDefinition.fagsaker.values.single { it.id == id }
    }
    return fagsakRepository
}
