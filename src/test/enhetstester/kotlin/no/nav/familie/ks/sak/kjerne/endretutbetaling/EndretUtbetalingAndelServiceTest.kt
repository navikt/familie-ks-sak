package no.nav.familie.ks.sak.kjerne.endretutbetaling

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseType
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
class EndretUtbetalingAndelServiceEnhetstest {
    @MockK
    private lateinit var endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository

    @MockK
    private lateinit var beregningService: BeregningService

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @MockK
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @InjectMockKs
    private lateinit var endretUtbetalingAndelService: EndretUtbetalingAndelService

    @Test
    fun `kopierEndretUtbetalingAndelFraForrigeBehandling - skal kopiere over endrete utbetaling i forrige behandling og lagre disse på ny`() {
        val gammelBehandling = lagBehandling()
        val nyBehandling = lagBehandling()

        every { endretUtbetalingAndelRepository.hentEndretUtbetalingerForBehandling(gammelBehandling.id) } returns
            listOf(
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
            )
        every { endretUtbetalingAndelRepository.save(any()) } returns mockk()

        endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(nyBehandling, gammelBehandling)

        verify(exactly = 1) { endretUtbetalingAndelRepository.hentEndretUtbetalingerForBehandling(gammelBehandling.id) }
        verify(exactly = 3) { endretUtbetalingAndelRepository.save(any()) }
    }

    @Test
    fun `opprettTomEndretUtbetalingAndel - skal opprette en tom EndretUtbetalingAndel med bare behandling satt`() {
        val behandling = lagBehandling()

        every { endretUtbetalingAndelRepository.save(any()) } returnsArgument 0

        val tomEndretUtbetalingAndelMedBehandlingSatt =
            endretUtbetalingAndelService.opprettTomEndretUtbetalingAndel(behandling)

        assertThat(tomEndretUtbetalingAndelMedBehandlingSatt.behandlingId, Is(behandling.id))
        assertThat(tomEndretUtbetalingAndelMedBehandlingSatt.årsak, Is(nullValue()))
        assertThat(tomEndretUtbetalingAndelMedBehandlingSatt.person, Is(nullValue()))
        assertThat(tomEndretUtbetalingAndelMedBehandlingSatt.tom, Is(nullValue()))
        assertThat(tomEndretUtbetalingAndelMedBehandlingSatt.fom, Is(nullValue()))

        verify(exactly = 1) { endretUtbetalingAndelRepository.save(any()) }
    }

    @Test
    fun `fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse - skal slette endret utbetaling andeler og oppdatere tilkjent ytelse`() {
        val behandling = lagBehandling()

        every { endretUtbetalingAndelRepository.deleteById(200) } just runs
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) } returns mockk()
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns mockk()
        every { beregningService.oppdaterTilkjentYtelsePåBehandling(behandling, any(), any()) } just runs

        endretUtbetalingAndelService.fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse(behandling, 200)

        verify(exactly = 1) { endretUtbetalingAndelRepository.deleteById(200) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
        verify(exactly = 1) { beregningService.oppdaterTilkjentYtelsePåBehandling(behandling, any(), any()) }
    }

    @Test
    fun `sanityBegrunnelserMedEndringsårsak - skal returnere et map med begrunnelsestyper mappet mot liste av begrunnelser`() {
        val sanityTekster =
            listOf(
                SanityBegrunnelse(
                    NasjonalEllerFellesBegrunnelse.AVSLAG_ENDRINGSPERIODE_ALLEREDE_UTBETALT_ANNEN_FORELDER.sanityApiNavn,
                    "avslagAlleredeUtbataltAnnenForelderEndringsperiode",
                    SanityBegrunnelseType.ENDRINGSPERIODE,
                    Vilkår.values().toList(),
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = listOf(Årsak.ALLEREDE_UTBETALT),
                    resultat = SanityResultat.AVSLAG,
                    skalAlltidVises = false,
                    støtterFritekst = false,
                ),
                SanityBegrunnelse(
                    NasjonalEllerFellesBegrunnelse.AVSLAG_ENDRINGSPERIODE_ALLEREDE_UTBETALT_SOKER.sanityApiNavn,
                    "avslagAlleredeUtbetaltSokerEndringsperiode",
                    SanityBegrunnelseType.ENDRINGSPERIODE,
                    Vilkår.values().toList(),
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = listOf(Årsak.ALLEREDE_UTBETALT),
                    resultat = SanityResultat.AVSLAG,
                    skalAlltidVises = false,
                    støtterFritekst = false,
                ),
            )

        val endringsårsakbegrunnelser = endretUtbetalingAndelService.sanityBegrunnelserMedEndringsårsak(sanityTekster)

        assertEquals(9, endringsårsakbegrunnelser.size)
        assertEquals(2, endringsårsakbegrunnelser[BegrunnelseType.AVSLAG]?.size)
    }
}
