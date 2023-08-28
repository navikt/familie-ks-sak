package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårsvurderingOppfylt
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
class BehandlingsresultatServiceTest {

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService

    @MockK
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @MockK
    private lateinit var søknadGrunnlagService: SøknadGrunnlagService

    @MockK
    private lateinit var personidentService: PersonidentService

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @InjectMockKs
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @Test
    fun `lagBehandlingsresulatPersoner - skal returnere BehandlingsresultatPerson som ikke er eksplisittAvslag hvis det ikke er avslag i vilkårsvurdering eller endretutbetaling`() {
        val behandling = mockk<Behandling>(relaxed = true)
        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(behandlingId = behandling.id, barnasIdenter = listOf(randomFnr(), randomFnr()))
        val vilkårsvurdering = lagVilkårsvurderingOppfylt(personopplysningGrunnlag.personer)

        val andelerMedEndringer = personopplysningGrunnlag.personer.map {
            EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                EndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    erEksplisittAvslagPåSøknad = false,
                    person = it,
                ),
                emptyList(),
            )
        }

        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) } returns personopplysningGrunnlag
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(
                behandlingId = behandling.id,
            )
        } returns andelerMedEndringer

        val behandlingsresulatPersoner = behandlingsresultatService.lagBehandlingsresulatPersoner(
            behandling = behandling,
            personerFremslitKravFor = emptyList(),
            forrigeAndelerMedEndringer = emptyList(),
            vilkårsvurdering = vilkårsvurdering,
            andelerMedEndringer = emptyList(),
        )

        assertThat(behandlingsresulatPersoner.all { it.eksplisittAvslag }, Is(false))
    }

    @Test
    fun `lagBehandlingsresulatPersoner - skal returnere BehandlingsresultatPerson som er eksplisittAvslag hvis det er avslag i endretutbetaling`() {
        val behandling = mockk<Behandling>(relaxed = true)
        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(behandlingId = behandling.id, barnasIdenter = listOf(randomFnr(), randomFnr()))
        val vilkårsvurdering = lagVilkårsvurderingOppfylt(personopplysningGrunnlag.personer)

        val andelerMedEndringer = personopplysningGrunnlag.personer.map {
            EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                EndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    erEksplisittAvslagPåSøknad = true,
                    person = it,
                ),
                emptyList(),
            )
        }

        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) } returns personopplysningGrunnlag
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(
                behandlingId = behandling.id,
            )
        } returns andelerMedEndringer

        val behandlingsresulatPersoner = behandlingsresultatService.lagBehandlingsresulatPersoner(
            behandling = behandling,
            personerFremslitKravFor = emptyList(),
            forrigeAndelerMedEndringer = emptyList(),
            vilkårsvurdering = vilkårsvurdering,
            andelerMedEndringer = emptyList(),
        )

        assertThat(behandlingsresulatPersoner.all { it.eksplisittAvslag }, Is(true))
    }

    @Test
    fun `lagBehandlingsresulatPersoner - skal returnere BehandlingsresultatPerson som er eksplisittAvslag hvis det er avslag i vilkårsvurderingen`() {
        val behandling = mockk<Behandling>(relaxed = true)
        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(behandlingId = behandling.id, barnasIdenter = listOf(randomFnr(), randomFnr()))
        val vilkårsvurdering =
            lagVilkårsvurderingOppfylt(personer = personopplysningGrunnlag.personer, erEksplisittAvslagPåSøknad = true)

        val andelerMedEndringer = personopplysningGrunnlag.personer.map {
            EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                EndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    erEksplisittAvslagPåSøknad = false,
                    person = it,
                ),
                emptyList(),
            )
        }

        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) } returns personopplysningGrunnlag
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(
                behandlingId = behandling.id,
            )
        } returns andelerMedEndringer

        val behandlingsresulatPersoner = behandlingsresultatService.lagBehandlingsresulatPersoner(
            behandling = behandling,
            personerFremslitKravFor = emptyList(),
            forrigeAndelerMedEndringer = emptyList(),
            vilkårsvurdering = vilkårsvurdering,
            andelerMedEndringer = emptyList(),
        )

        assertThat(behandlingsresulatPersoner.all { it.eksplisittAvslag }, Is(true))
    }
}
