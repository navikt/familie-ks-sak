package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.cucumber.StepDefinition
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository

fun mockVilkårsvurderingRepository(stepDefinition: StepDefinition): VilkårsvurderingRepository {
    val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()

    every { vilkårsvurderingRepository.save(any()) } answers {
        val vilkårsvurdering = firstArg<Vilkårsvurdering>()

        stepDefinition.vilkårsvurdering[vilkårsvurdering.behandling.id] = vilkårsvurdering

        vilkårsvurdering
    }

    every { vilkårsvurderingRepository.finnAktivForBehandling(any()) } answers {
        val behandlingId = firstArg<Long>()

        stepDefinition.vilkårsvurdering[behandlingId]
    }

    return vilkårsvurderingRepository
}
