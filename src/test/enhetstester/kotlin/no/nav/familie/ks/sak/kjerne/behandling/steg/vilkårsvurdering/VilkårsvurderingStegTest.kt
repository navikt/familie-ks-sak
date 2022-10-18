package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import org.hamcrest.CoreMatchers.`is` as Is


@ExtendWith(MockKExtension::class)
class VilkårsvurderingStegTest {

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @InjectMockKs
    private lateinit var vilkårsvurderingSteg: VilkårsvurderingSteg

    private val søker = randomAktør()

    private val fagsak = lagFagsak(søker)

    private val behandling = lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.DØDSFALL)

    @Test
    fun `utførSteg - skal kaste funksjonell feil hvis behandlingsårsak er DØDSFALL og det eksisterer vilkår lengre fram i tid enn søkers dødsdato`() {
        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktørId,
            søkerAktør = søker,
            søkerDødsDato = LocalDate.of(2020, 12, 12)
        )

        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id
                ),
            )
        )

        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat)

        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker


        val feil = assertThrows<FunksjonellFeil> {
            vilkårsvurderingSteg.utførSteg(behandling.id)
        }

        assertThat(
            feil.message,
            Is("Ved behandlingsårsak \"Dødsfall\" må vilkårene på søker avsluttes senest dagen søker døde, men \"Bosatt i riket\" vilkåret til søker slutter etter søkers død.")
        )

        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
    }

    @Test
    fun `utførSteg - skal kaste funksjonell feil hvis det er periode overlapp mellom delt bosted og gradert barnehageplass vilkår`() {
        val barn = randomAktør()

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktørId,
            søkerAktør = søker
        )

        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker)
        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = barn)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id
                ),
            )
        )

        barnPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                    antallTimer = BigDecimal(25)
                ),
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                    periodeFom = LocalDate.of(2020, 12, 12),
                    periodeTom = LocalDate.of(2022, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id
                )
            )
        )

        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker


        val feil = assertThrows<FunksjonellFeil> {
            vilkårsvurderingSteg.utførSteg(behandling.id)
        }

        assertThat(
            feil.message,
            Is("Det er lagt inn gradert barnehageplass og delt bosted for samme periode.")
        )

        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
    }

    @Test
    fun `utførSteg - skal kaste validere vilkårsvurderingen`() {
        val barn = randomAktør()

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktørId,
            søkerAktør = søker
        )

        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker)
        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = barn)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 10, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id
                ),
            )
        )

        barnPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 10, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                    antallTimer = BigDecimal(25)
                ),
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                    periodeFom = LocalDate.of(2022, 11, 12),
                    periodeTom = LocalDate.of(2022, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id
                )
            )
        )

        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker

        vilkårsvurderingSteg.utførSteg(behandling.id)

        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
    }
}
