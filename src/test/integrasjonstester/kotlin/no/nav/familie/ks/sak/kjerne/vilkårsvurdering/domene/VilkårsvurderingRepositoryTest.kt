package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VilkårsvurderingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Test
    fun `finnAktiv - skal returnere aktiv vilkårsvurdering for behandling`() {
        val vilkårsvurdering = lagVilkårsvurdering(søker, behandling, Resultat.IKKE_VURDERT)
        vilkårsvurderingRepository.saveAndFlush(vilkårsvurdering)

        val hentetVilkårsvurdering = vilkårsvurderingRepository.finnAktiv(behandling.id)

        assertNotNull(hentetVilkårsvurdering)
        assertEquals(behandling.id, hentetVilkårsvurdering!!.behandling.id)
    }

    @Test
    fun `finnAktiv - skal returnere null dersom vilkårsvurdering for behandling ikke finnes`() {
        val hentetVilkårsvurdering = vilkårsvurderingRepository.finnAktiv(404L)

        assertNull(hentetVilkårsvurdering)
    }
}
