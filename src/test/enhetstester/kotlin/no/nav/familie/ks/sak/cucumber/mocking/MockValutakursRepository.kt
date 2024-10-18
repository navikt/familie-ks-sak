package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.cucumber.StepDefinition
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs

fun mockValutakursRepository(stepDefinition: StepDefinition): ValutakursRepository {
    val valutakursRepository = mockk<ValutakursRepository>()
    every { valutakursRepository.findByBehandlingId(any()) } answers {
        val behandlingId = firstArg<Long>()
        stepDefinition.valutakurs[behandlingId] ?: emptyList()
    }
    every { valutakursRepository.deleteAll(any<Iterable<Valutakurs>>()) } answers {
        val valutakurser = firstArg<Iterable<Valutakurs>>()
        valutakurser.forEach {
            stepDefinition.valutakurs[it.behandlingId] =
                stepDefinition.valutakurs[it.behandlingId]?.filter { valutakurs -> valutakurs != it } ?: emptyList()
        }
    }
    every { valutakursRepository.saveAll(any<Iterable<Valutakurs>>()) } answers {
        val valutakurser = firstArg<Iterable<Valutakurs>>()
        valutakurser.forEach {
            stepDefinition.valutakurs[it.behandlingId] =
                (stepDefinition.valutakurs[it.behandlingId] ?: emptyList()) + it
        }
        valutakurser.toList()
    }

    return valutakursRepository
}
