package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.cucumber.StepDefinition
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository

fun mockPersonopplysningGrunnlagRepository(stepDefinition: StepDefinition): PersonopplysningGrunnlagRepository {
    val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()

    every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } answers {
        val behandlingsId = firstArg<Long>()
        stepDefinition.personopplysningGrunnlagMap[behandlingsId]
            ?: throw Feil("Fant ikke personopplysninggrunnlag for behandling $behandlingsId")
    }
    return personopplysningGrunnlagRepository
}
