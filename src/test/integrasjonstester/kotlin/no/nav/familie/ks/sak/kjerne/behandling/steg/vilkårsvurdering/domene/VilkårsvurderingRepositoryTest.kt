package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.shouldNotBeNull
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VilkårsvurderingRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @BeforeEach
    fun beforeEach() {
        opprettSøkerFagsakOgBehandling(randomAktør(), fagsakStatus = FagsakStatus.LØPENDE)
    }

    @Test
    fun `finnAktivForBehandling - skal returnere aktiv vilkårsvurdering for behandling`() {
        opprettVilkårsvurdering(søker, behandling, Resultat.IKKE_VURDERT)

        val hentetVilkårsvurdering = vilkårsvurderingRepository.finnAktivForBehandling(behandling.id).shouldNotBeNull()

        assertEquals(behandling.id, hentetVilkårsvurdering.behandling.id)
    }

    @Test
    fun `finnAktivForBehandling - skal returnere null dersom vilkårsvurdering for behandling ikke finnes`() {
        val hentetVilkårsvurdering = vilkårsvurderingRepository.finnAktivForBehandling(404L)

        assertNull(hentetVilkårsvurdering)
    }
}
