package no.nav.familie.ks.sak.kjerne.endretutbetaling

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.EndretUtbetalingAndelRequestDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndel
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagTestPersonopplysningGrunnlag
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseType
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

class EndretUtbetalingAndelServiceTest {
    private val endretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()
    private val beregningService = mockk<BeregningService>()
    private val personopplysningGrunnlagService = mockk<PersonopplysningGrunnlagService>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val sanityService = mockk<SanityService>()

    private val endretUtbetalingAndelService =
        EndretUtbetalingAndelService(
            endretUtbetalingAndelRepository = endretUtbetalingAndelRepository,
            beregningService = beregningService,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            vilkårsvurderingService = vilkårsvurderingService,
            sanityService = sanityService,
        )

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
    fun `kopierEndretUtbetalingAndelFraForrigeBehandling - skal sette avslag til false og tømme begrunnelser ved kopiering av endret utbetaling andel`() {
        // Arrange
        val gammelBehandling = lagBehandling()
        val nyBehandling = lagBehandling()
        val endretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                årsak = Årsak.ETTERBETALING_3MND,
                begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.AVSLAG_SØKT_FOR_SENT_ENDRINGSPERIODE),
                erEksplisittAvslagPåSøknad = true,
            )
        val lagretEndretUtbetalingAndelSlot = slot<EndretUtbetalingAndel>()

        every { endretUtbetalingAndelRepository.hentEndretUtbetalingerForBehandling(gammelBehandling.id) } returns
            listOf(
                endretUtbetalingAndel,
            )
        every { endretUtbetalingAndelRepository.save(capture(lagretEndretUtbetalingAndelSlot)) } returnsArgument 0

        // Act
        endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(nyBehandling, gammelBehandling)

        // Assert
        verify(exactly = 1) { endretUtbetalingAndelRepository.hentEndretUtbetalingerForBehandling(gammelBehandling.id) }
        verify(exactly = 1) { endretUtbetalingAndelRepository.save(capture(lagretEndretUtbetalingAndelSlot)) }

        val lagretEndretUtbetalingAndel = lagretEndretUtbetalingAndelSlot.captured

        assertThat(lagretEndretUtbetalingAndel.vedtaksbegrunnelser, Is(emptyList()))
        assertThat(lagretEndretUtbetalingAndel.erEksplisittAvslagPåSøknad, Is(false))
        assertThat(lagretEndretUtbetalingAndel.årsak, Is(Årsak.ETTERBETALING_3MND))
    }

    @Test
    fun `opprettTomEndretUtbetalingAndel - skal opprette en tom EndretUtbetalingAndel med bare behandling satt`() {
        val behandling = lagBehandling()

        every { endretUtbetalingAndelRepository.save(any()) } returnsArgument 0

        val tomEndretUtbetalingAndelMedBehandlingSatt =
            endretUtbetalingAndelService.opprettTomEndretUtbetalingAndel(behandling)

        assertThat(tomEndretUtbetalingAndelMedBehandlingSatt.behandlingId, Is(behandling.id))
        assertThat(tomEndretUtbetalingAndelMedBehandlingSatt.årsak, Is(nullValue()))
        assertThat(tomEndretUtbetalingAndelMedBehandlingSatt.personer, Is(emptySet()))
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
    fun `hentSanityBegrunnelserMedEndringsårsak - skal returnere et map med begrunnelsestyper mappet mot liste av begrunnelser`() {
        // Arrange
        every { sanityService.hentSanityBegrunnelser() } returns
            listOf(
                SanityBegrunnelse(
                    NasjonalEllerFellesBegrunnelse.AVSLAG_ENDRINGSPERIODE_ALLEREDE_UTBETALT_ANNEN_FORELDER.sanityApiNavn,
                    "avslagAlleredeUtbataltAnnenForelderEndringsperiode",
                    SanityBegrunnelseType.ENDRINGSPERIODE,
                    Vilkår.entries.toList(),
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = listOf(Årsak.ALLEREDE_UTBETALT),
                    resultat = SanityResultat.AVSLAG,
                    skalAlltidVises = false,
                    støtterFritekst = false,
                    ikkeIBruk = false,
                ),
                SanityBegrunnelse(
                    NasjonalEllerFellesBegrunnelse.AVSLAG_ENDRINGSPERIODE_ALLEREDE_UTBETALT_SØKER.sanityApiNavn,
                    "avslagAlleredeUtbetaltSokerEndringsperiode",
                    SanityBegrunnelseType.ENDRINGSPERIODE,
                    Vilkår.entries.toList(),
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = listOf(Årsak.ALLEREDE_UTBETALT),
                    resultat = SanityResultat.AVSLAG,
                    skalAlltidVises = false,
                    støtterFritekst = false,
                    ikkeIBruk = false,
                ),
            )

        // Act
        val endringsårsakbegrunnelser = endretUtbetalingAndelService.hentSanityBegrunnelserMedEndringsårsak()

        // Assert
        assertThat(endringsårsakbegrunnelser.size, Is(9))
        assertThat(endringsårsakbegrunnelser[BegrunnelseType.AVSLAG]?.size, Is(2))
    }

    @Test
    fun `Skal håndtere EndretUtbetalingAndelRequestDto med blanding av nytt og gammelt felt for personIdent(er)`() {
        // Arrange
        val behandling = lagBehandling()
        val barn1 = lagPerson(personType = PersonType.BARN)
        val barn2 = lagPerson(personType = PersonType.BARN)

        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)

        val andelerTilkjentYtelse =
            listOf(
                lagAndelTilkjentYtelse(
                    person = barn1,
                    fom = YearMonth.of(2025, 1),
                    tom = YearMonth.of(2025, 1),
                ),
                lagAndelTilkjentYtelse(
                    person = barn2,
                    fom = YearMonth.of(2025, 1),
                    tom = YearMonth.of(2025, 1),
                ),
            )

        val lagretEndretUtbetalingAndel = slot<EndretUtbetalingAndel>()

        every { endretUtbetalingAndelRepository.getReferenceById(any()) } returns endretUtbetalingAndel
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns lagTestPersonopplysningGrunnlag(behandling.id, barn1, barn2)
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id) } returns andelerTilkjentYtelse
        every { endretUtbetalingAndelRepository.hentEndretUtbetalingerForBehandling(behandlingId = behandling.id) } returns listOf(endretUtbetalingAndel)
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id) } returns Vilkårsvurdering(behandling = behandling)
        every { endretUtbetalingAndelRepository.saveAndFlush(capture(lagretEndretUtbetalingAndel)) } returns mockk()
        every { beregningService.oppdaterTilkjentYtelsePåBehandling(any(), any(), any(), any()) } returns mockk()

        var endretUtbetalingAndelRequestDto =
            EndretUtbetalingAndelRequestDto(
                id = 0,
                personIdent = null,
                personIdenter = null,
                prosent = BigDecimal.ZERO,
                fom = YearMonth.of(2025, 1),
                tom = YearMonth.of(2025, 1),
                årsak = Årsak.ALLEREDE_UTBETALT,
                søknadstidspunkt = LocalDate.now(),
                begrunnelse = "Test begrunnelse",
                erEksplisittAvslagPåSøknad = false,
                vedtaksbegrunnelser = emptyList(),
            )

        // Act & Assert
        assertThrows<FunksjonellFeil> {
            endretUtbetalingAndelService.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
                behandling = behandling,
                endretUtbetalingAndelRequestDto = endretUtbetalingAndelRequestDto,
            )
        }

        // Arrange
        endretUtbetalingAndelRequestDto =
            endretUtbetalingAndelRequestDto.copy(
                personIdenter = null,
                personIdent = barn1.aktør.aktivFødselsnummer(),
            )

        // Act & Assert
        assertDoesNotThrow {
            endretUtbetalingAndelService.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
                behandling = behandling,
                endretUtbetalingAndelRequestDto = endretUtbetalingAndelRequestDto,
            )
        }

        // Assert
        assertThat(lagretEndretUtbetalingAndel.captured).isEqualTo(
            endretUtbetalingAndel.copy(
                personer = mutableSetOf(barn1),
            ),
        )

        // Arrange
        endretUtbetalingAndelRequestDto =
            endretUtbetalingAndelRequestDto.copy(
                personIdenter = listOf(barn1.aktør.aktivFødselsnummer(), barn2.aktør.aktivFødselsnummer()),
                personIdent = barn1.aktør.aktivFødselsnummer(),
            )

        // Act & Assert
        assertDoesNotThrow {
            endretUtbetalingAndelService.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
                behandling = behandling,
                endretUtbetalingAndelRequestDto = endretUtbetalingAndelRequestDto,
            )
        }

        // Assert
        assertThat(lagretEndretUtbetalingAndel.captured).isEqualTo(
            endretUtbetalingAndel.copy(
                personer = mutableSetOf(barn1, barn2),
            ),
        )
    }
}
