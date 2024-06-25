package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.cucumber.StepDefinition
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp

fun mockUtenlandskPeriodebeløpRepository(stepDefinition: StepDefinition): UtenlandskPeriodebeløpRepository {
    val utenlandskPeriodebeløpRepository = mockk<UtenlandskPeriodebeløpRepository>()
    every { utenlandskPeriodebeløpRepository.findByBehandlingId(any()) } answers {
        val behandlingId = firstArg<Long>()
        stepDefinition.utenlandskPeriodebeløp[behandlingId] ?: emptyList()
    }
    every { utenlandskPeriodebeløpRepository.deleteAll(any<Iterable<UtenlandskPeriodebeløp>>()) } answers {
        val utenlandskPeriodebeløp = firstArg<Iterable<UtenlandskPeriodebeløp>>()
        utenlandskPeriodebeløp.forEach {
            stepDefinition.utenlandskPeriodebeløp[it.behandlingId] =
                stepDefinition.utenlandskPeriodebeløp[it.behandlingId]?.filter { utenlandskPeriodebeløp -> utenlandskPeriodebeløp != it }
                    ?: emptyList()
        }
    }
    every { utenlandskPeriodebeløpRepository.saveAll(any<Iterable<UtenlandskPeriodebeløp>>()) } answers {
        val utenlandskPeriodebeløp = firstArg<Iterable<UtenlandskPeriodebeløp>>()
        utenlandskPeriodebeløp.forEach {
            stepDefinition.utenlandskPeriodebeløp[it.behandlingId] =
                (stepDefinition.utenlandskPeriodebeløp[it.behandlingId] ?: emptyList()) + it
        }
        utenlandskPeriodebeløp.toList()
    }
    return utenlandskPeriodebeløpRepository
}
