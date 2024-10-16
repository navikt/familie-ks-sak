package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.cucumber.StepDefinition
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseRepository

fun mockKompetanseRepository(stepDefinition: StepDefinition): KompetanseRepository {
    val kompetanseRepository = mockk<KompetanseRepository>()
    every { kompetanseRepository.findByBehandlingId(any()) } answers {
        val behandlingId = firstArg<Long>()
        stepDefinition.kompetanser[behandlingId] ?: emptyList()
    }
    every { kompetanseRepository.deleteAll(any<Iterable<Kompetanse>>()) } answers {
        val kompetanser = firstArg<Iterable<Kompetanse>>()
        kompetanser.forEach {
            stepDefinition.kompetanser[it.behandlingId] =
                stepDefinition.kompetanser[it.behandlingId]?.filter { kompetanse -> kompetanse != it } ?: emptyList()
        }
    }
    every { kompetanseRepository.saveAll(any<Iterable<Kompetanse>>()) } answers {
        val kompetanser = firstArg<Iterable<Kompetanse>>()
        kompetanser.forEach {
            stepDefinition.kompetanser[it.behandlingId] =
                (stepDefinition.kompetanser[it.behandlingId] ?: emptyList()) + it
        }
        kompetanser.toMutableList()
    }
    return kompetanseRepository
}
