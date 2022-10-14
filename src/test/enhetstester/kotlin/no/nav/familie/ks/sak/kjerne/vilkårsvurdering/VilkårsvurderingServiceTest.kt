package no.nav.familie.ks.sak.kjerne.vilkårsvurdering

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VilkårsvurderingServiceTest : OppslagSpringRunnerTest() {

    @MockK
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var personidentService: PersonidentService

    @InjectMockKs
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @BeforeEach
    fun beforeEach() {
        opprettSøkerFagsakOgBehandling()
    }

    @Test
    fun `opprettVilkårsvurdering - skal opprette tom vilkårsvurdering dersom det ikke finnes tidligere vedtatte behandlinger på fagsak`() {
        val barn1 = lagreAktør(randomAktør())
        val barn2 = lagreAktør(randomAktør())
        val lagretVilkårsvurderingSlot = slot<Vilkårsvurdering>()
        every { vilkårsvurderingRepository.finnAktivForBehandling(any()) } returns null
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer(),
            barnasIdenter = listOf(barn1.aktivFødselsnummer(), barn2.aktivFødselsnummer())
        )
        every { vilkårsvurderingRepository.save(capture(lagretVilkårsvurderingSlot)) } returns mockk()

        vilkårsvurderingService.opprettVilkårsvurdering(behandling, null)

        val lagretVilkårsvurdering = lagretVilkårsvurderingSlot.captured

        assertEquals(3, lagretVilkårsvurdering.personResultater.size)
        assertThat(
            lagretVilkårsvurdering.personResultater.find { it.aktør.aktivFødselsnummer() === søker.aktivFødselsnummer() }?.vilkårResultater?.map { it.vilkårType },
            containsInAnyOrder(Vilkår.BOSATT_I_RIKET, Vilkår.MEDLEMSKAP)
        )
        assertThat(
            lagretVilkårsvurdering.personResultater.find { it.aktør.aktivFødselsnummer() === barn1.aktivFødselsnummer() }?.vilkårResultater?.map { it.vilkårType },
            containsInAnyOrder(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.BARNEHAGEPLASS,
                Vilkår.BOR_MED_SØKER,
                Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                Vilkår.MELLOM_1_OG_2_ELLER_ADOPTERT
            )
        )
    }

    @Test
    fun `opprettVilkårsvurdering - skal opprette tom vilkårsvurdering og deaktivere eksisterende dersom det ikke finnes tidligere vedtatte behandlinger på fagsak`() {
        val barn1 = lagreAktør(randomAktør())
        val barn2 = lagreAktør(randomAktør())
        val lagretDeaktivertVilkårsvurderingSlot = slot<Vilkårsvurdering>()
        val lagretVilkårsvurderingSlot = slot<Vilkårsvurdering>()
        every { vilkårsvurderingRepository.finnAktivForBehandling(any()) } returns lagVilkårsvurdering(
            søker,
            behandling,
            Resultat.OPPFYLT
        )
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer(),
            barnasIdenter = listOf(barn1.aktivFødselsnummer(), barn2.aktivFødselsnummer())
        )
        every { vilkårsvurderingRepository.saveAndFlush(capture(lagretDeaktivertVilkårsvurderingSlot)) } returns mockk()
        every { vilkårsvurderingRepository.save(capture(lagretVilkårsvurderingSlot)) } returns mockk()

        vilkårsvurderingService.opprettVilkårsvurdering(behandling, null)

        val lagretVilkårsvurdering = lagretVilkårsvurderingSlot.captured
        val lagretDeaktivertVilkårsvurdering = lagretDeaktivertVilkårsvurderingSlot.captured

        assertEquals(3, lagretVilkårsvurdering.personResultater.size)
        assertFalse(lagretDeaktivertVilkårsvurdering.aktiv)
    }
}
