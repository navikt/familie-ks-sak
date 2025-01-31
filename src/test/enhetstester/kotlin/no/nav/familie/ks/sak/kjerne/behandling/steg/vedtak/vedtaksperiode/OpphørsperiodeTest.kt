package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import no.nav.familie.ks.sak.common.util.forrigeMåned
import no.nav.familie.ks.sak.common.util.inneværendeMåned
import no.nav.familie.ks.sak.common.util.nesteMåned
import no.nav.familie.ks.sak.common.util.toLocalDate
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.fnrTilAktør
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår

class OpphørsperiodeTest {
    val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val søkerFnr = randomFnr()
    private val barn1Fnr = randomFnr()
    private val barn2Fnr = randomFnr()

    val personopplysningGrunnlag = lagPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))

    val søker = fnrTilAktør(søkerFnr)
    val barn1 = fnrTilAktør(barn1Fnr)
    val barn2 = fnrTilAktør(barn2Fnr)

    val vilkårsvurdering =
        Vilkårsvurdering(
            behandling = behandling,
        )

    @Nested
    inner class MapTilOpphørsperioderTest {
        @Test
        fun `asdfasda`() {
            val periodeTomFørsteAndel = inneværendeMåned().minusYears(2)
            val periodeFomAndreAndel = inneværendeMåned().minusYears(1)
            val periodeTomAndreAndel = inneværendeMåned().minusMonths(10)
            val periodeFomSisteAndel = inneværendeMåned().minusMonths(4)

            val andel1TilBarn1 =
                AndelTilkjentYtelseMedEndreteUtbetalinger(
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        stønadFom = inneværendeMåned().minusYears(4),
                        stønadTom = periodeTomFørsteAndel,
                        sats = 1054,
                        aktør = barn1,
                    ),
                    emptyList(),
                )

            val andel2TilBarn1 =
                AndelTilkjentYtelseMedEndreteUtbetalinger(
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        stønadFom = periodeFomAndreAndel,
                        stønadTom = periodeTomAndreAndel,
                        sats = 1054,
                        aktør = barn1,
                    ),
                    emptyList(),
                )

            val vilkårsvurdering = lagVilkårsvurdering(
                lagPersonResultat = { vilkårsvurdering ->
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagVilkårResultat(
                                    personResultat = personResultat,
                                    vilkårType = Vilkår.BARNEHAGEPLASS,
                                    periodeTom = LocalDate.of(2024, 1, 1),
                                )
                            )
                        }
                    )
                }
            )

            val opphørsperioder =
                mapTilOpphørsperioder(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    andelerTilkjentYtelse = listOf(andel1TilBarn1, andel2TilBarn1),
                    vilkårsvurdering = vilkårsvurdering,
                )

            assertEquals(2, opphørsperioder.size)
            assertEquals(periodeTomFørsteAndel.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
            assertEquals(periodeFomAndreAndel.forrigeMåned(), opphørsperioder[0].periodeTom?.toYearMonth())
            assertEquals(periodeTomAndreAndel.nesteMåned(), opphørsperioder[1].periodeFom.toYearMonth())
            assertEquals(periodeFomSisteAndel.forrigeMåned(), opphørsperioder[1].periodeTom?.toYearMonth())
        }

        @Test
        fun `skal utlede opphørsperiode mellom oppfylte perioder`() {
            val periodeTomFørsteAndel = inneværendeMåned().minusYears(2)
            val periodeFomAndreAndel = inneværendeMåned().minusYears(1)
            val periodeTomAndreAndel = inneværendeMåned().minusMonths(10)
            val periodeFomSisteAndel = inneværendeMåned().minusMonths(4)

            val andelBarn1 =
                AndelTilkjentYtelseMedEndreteUtbetalinger(
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        stønadFom = inneværendeMåned().minusYears(4),
                        stønadTom = periodeTomFørsteAndel,
                        sats = 1054,
                        aktør = barn1,
                    ),
                    emptyList(),
                )

            val andel2Barn1 =
                AndelTilkjentYtelseMedEndreteUtbetalinger(
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        stønadFom = periodeFomAndreAndel,
                        stønadTom = periodeTomAndreAndel,
                        sats = 1054,
                        aktør = barn1,
                    ),
                    emptyList(),
                )

            val andel3Barn1 =
                AndelTilkjentYtelseMedEndreteUtbetalinger(
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        stønadFom = periodeFomSisteAndel,
                        stønadTom = inneværendeMåned().plusMonths(12),
                        sats = 1054,
                        aktør = barn1,
                    ),
                    emptyList(),
                )

            val opphørsperioder =
                mapTilOpphørsperioder(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    andelerTilkjentYtelse = listOf(andelBarn1, andel2Barn1, andel3Barn1),
                    vilkårsvurdering = vilkårsvurdering,
                )

            assertEquals(2, opphørsperioder.size)
            assertEquals(periodeTomFørsteAndel.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
            assertEquals(periodeFomAndreAndel.forrigeMåned(), opphørsperioder[0].periodeTom?.toYearMonth())
            assertEquals(periodeTomAndreAndel.nesteMåned(), opphørsperioder[1].periodeFom.toYearMonth())
            assertEquals(periodeFomSisteAndel.forrigeMåned(), opphørsperioder[1].periodeTom?.toYearMonth())
        }

        @Test
        fun `skal utlede opphørsperiode når siste utbetalingsperiode er før neste måned`() {
            val periodeTomFørsteAndel = inneværendeMåned().minusYears(1)
            val andelBarn1 =
                AndelTilkjentYtelseMedEndreteUtbetalinger(
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        stønadFom = inneværendeMåned().minusYears(4),
                        stønadTom = periodeTomFørsteAndel,
                        sats = 1054,
                        aktør = barn1,
                    ),
                    emptyList(),
                )

            val opphørsperioder =
                mapTilOpphørsperioder(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    andelerTilkjentYtelse = listOf(andelBarn1),
                    vilkårsvurdering = vilkårsvurdering,
                )

            assertEquals(1, opphørsperioder.size)
            assertEquals(periodeTomFørsteAndel.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
            assertEquals(null, opphørsperioder[0].periodeTom)
        }

        @Test
        fun `skal utlede opphørsperiode fra neste måned når siste utbetalingsperiode er inneværende måned`() {
            val periodeTomFørsteAndel = inneværendeMåned()
            val andelBarn1 =
                AndelTilkjentYtelseMedEndreteUtbetalinger(
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        stønadFom = inneværendeMåned().minusYears(4),
                        stønadTom = periodeTomFørsteAndel,
                        sats = 1054,
                        aktør = barn1,
                    ),
                    emptyList(),
                )

            val opphørsperioder =
                mapTilOpphørsperioder(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    andelerTilkjentYtelse = listOf(andelBarn1),
                    vilkårsvurdering = vilkårsvurdering,
                )

            assertEquals(1, opphørsperioder.size)
            assertEquals(periodeTomFørsteAndel.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
            assertEquals(null, opphørsperioder[0].periodeTom)
        }
    }

    @Nested
    inner class SlåSammenOpphørsperioderTest {
        @Test
        fun `skal slå sammen to like opphørsperioder`() {
            val periode12MånederFraInneværendeMåned = inneværendeMåned().minusMonths(12).toLocalDate()

            val toLikePerioder =
                listOf(
                    Opphørsperiode(
                        periodeFom = periode12MånederFraInneværendeMåned,
                        periodeTom = inneværendeMåned().toLocalDate(),
                    ),
                    Opphørsperiode(
                        periodeFom = periode12MånederFraInneværendeMåned,
                        periodeTom = inneværendeMåned().toLocalDate(),
                    ),
                )

            assertEquals(1, slåSammenOpphørsperioder(toLikePerioder).size)
        }

        @Test
        fun `skal slå sammen to opphørsperioder med ulik sluttdato`() {
            val toPerioderMedUlikSluttdato =
                listOf(
                    Opphørsperiode(
                        periodeFom = inneværendeMåned().minusMonths(12).toLocalDate(),
                        periodeTom = inneværendeMåned().toLocalDate(),
                    ),
                    Opphørsperiode(
                        periodeFom = inneværendeMåned().minusMonths(12).toLocalDate(),
                        periodeTom = inneværendeMåned().nesteMåned().toLocalDate(),
                    ),
                )
            val enPeriodeMedSluttDatoNesteMåned = slåSammenOpphørsperioder(toPerioderMedUlikSluttdato)

            assertEquals(1, enPeriodeMedSluttDatoNesteMåned.size)
            assertEquals(inneværendeMåned().nesteMåned().toLocalDate(), enPeriodeMedSluttDatoNesteMåned.first().periodeTom)
        }

        @Test
        fun `skal slå sammen to opphørsperioder med ulik startdato`() {
            val toPerioderMedUlikStartdato =
                listOf(
                    Opphørsperiode(
                        periodeFom = inneværendeMåned().minusMonths(12).toLocalDate(),
                        periodeTom = inneværendeMåned().toLocalDate(),
                    ),
                    Opphørsperiode(
                        periodeFom = inneværendeMåned().minusMonths(13).toLocalDate(),
                        periodeTom = inneværendeMåned().toLocalDate(),
                    ),
                )
            val enPeriodeMedStartDato13MånederTilbake = slåSammenOpphørsperioder(toPerioderMedUlikStartdato)

            assertEquals(1, enPeriodeMedStartDato13MånederTilbake.size)
            assertEquals(
                inneværendeMåned().minusMonths(13).toLocalDate(),
                enPeriodeMedStartDato13MånederTilbake.first().periodeFom,
            )
        }

        @Test
        fun `skal slå sammen to opphørsperioder som overlapper`() {
            val førsteOpphørsperiodeFom = inneværendeMåned().minusMonths(12).toLocalDate()
            val sisteOpphørsperiodeTom = inneværendeMåned().plusMonths(1).toLocalDate()
            val toPerioderMedUlikStartdato =
                listOf(
                    Opphørsperiode(
                        periodeFom = førsteOpphørsperiodeFom,
                        periodeTom = inneværendeMåned().minusMonths(2).toLocalDate(),
                    ),
                    Opphørsperiode(
                        periodeFom = inneværendeMåned().minusMonths(6).toLocalDate(),
                        periodeTom = sisteOpphørsperiodeTom,
                    ),
                )
            val enOpphørsperiodeMedFørsteFomOgSisteTom = slåSammenOpphørsperioder(toPerioderMedUlikStartdato)

            assertEquals(1, enOpphørsperiodeMedFørsteFomOgSisteTom.size)
            assertEquals(førsteOpphørsperiodeFom, enOpphørsperiodeMedFørsteFomOgSisteTom.first().periodeFom)
            assertEquals(sisteOpphørsperiodeTom, enOpphørsperiodeMedFørsteFomOgSisteTom.first().periodeTom)
        }
    }

    @Nested
    inner class TilVedtaksperiodeMedBegrunnelse {
        @Test
        fun `Skal opprette vedtaksperiode fra opphørsperiode`() {
            // Arrange
            val vedtak = lagVedtak()
            val førsteAugust2024 = LocalDate.of(2024, 8, 1)

            val opphørsperiode =
                Opphørsperiode(
                    periodeFom = førsteAugust2024,
                    periodeTom = null,
                    vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
                    begrunnelser = emptyList(),
                )

            // Act
            val vedtaksperiodeMedBegrunnelser = opphørsperiode.tilVedtaksperiodeMedBegrunnelse(vedtak)

            // Assert
            assertThat(vedtaksperiodeMedBegrunnelser.begrunnelser).isEmpty()
            assertThat(vedtaksperiodeMedBegrunnelser.fom).isEqualTo(førsteAugust2024)
            assertThat(vedtaksperiodeMedBegrunnelser.tom).isNull()
        }

        @Test
        fun `Skal legge til eøs overgangsbegrunnelser dersom behandlingen er EØS Overgangsordning behandling`() {
            // Arrange
            val vedtak = lagVedtak(lagBehandling(opprettetÅrsak = BehandlingÅrsak.OVERGANGSORDNING_2024, kategori = BehandlingKategori.EØS))
            val førsteAugust2024 = LocalDate.of(2024, 8, 1)

            val opphørsperiode =
                Opphørsperiode(
                    periodeFom = førsteAugust2024,
                    periodeTom = null,
                    vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
                )

            // Act
            val vedtaksperiodeMedBegrunnelser = opphørsperiode.tilVedtaksperiodeMedBegrunnelse(vedtak)

            // Assert
            assertThat(vedtaksperiodeMedBegrunnelser.eøsBegrunnelser.size).isEqualTo(1)
            assertThat(vedtaksperiodeMedBegrunnelser.eøsBegrunnelser.single().begrunnelse).isEqualTo(EØSBegrunnelse.OPPHØR_OVERGANGSORDNING_OPPHØR_EØS)
            assertThat(vedtaksperiodeMedBegrunnelser.fom).isEqualTo(førsteAugust2024)
            assertThat(vedtaksperiodeMedBegrunnelser.tom).isNull()
        }

        @Test
        fun `Skal legge til nasjonal overgangsbegrunnelser dersom behandlingen er nasjonal Overgangsordning behandling`() {
            // Arrange
            val vedtak = lagVedtak(lagBehandling(opprettetÅrsak = BehandlingÅrsak.OVERGANGSORDNING_2024, kategori = BehandlingKategori.NASJONAL))
            val førsteAugust2024 = LocalDate.of(2024, 8, 1)

            val opphørsperiode =
                Opphørsperiode(
                    periodeFom = førsteAugust2024,
                    periodeTom = null,
                    vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
                )

            // Act
            val vedtaksperiodeMedBegrunnelser = opphørsperiode.tilVedtaksperiodeMedBegrunnelse(vedtak)

            // Assert
            assertThat(vedtaksperiodeMedBegrunnelser.begrunnelser.size).isEqualTo(1)
            assertThat(vedtaksperiodeMedBegrunnelser.begrunnelser.single().nasjonalEllerFellesBegrunnelse).isEqualTo(NasjonalEllerFellesBegrunnelse.OPPHØR_OVERGANGSORDNING_OPPHØR)
            assertThat(vedtaksperiodeMedBegrunnelser.fom).isEqualTo(førsteAugust2024)
            assertThat(vedtaksperiodeMedBegrunnelser.tom).isNull()
        }

        @ParameterizedTest
        @EnumSource(value = BehandlingÅrsak::class, names = ["OVERGANGSORDNING_2024"], mode = EnumSource.Mode.EXCLUDE)
        fun `Skal legge til begrunnelser fra selve vedtaksperioden dersom behandlingen ikke har overgangsordning som årsak`(behandlingÅrsak: BehandlingÅrsak) {
            // Arrange
            val vedtak = lagVedtak(lagBehandling(opprettetÅrsak = behandlingÅrsak, kategori = BehandlingKategori.NASJONAL))
            val førsteAugust2024 = LocalDate.of(2024, 8, 1)

            val opphørsperiode =
                Opphørsperiode(
                    periodeFom = førsteAugust2024,
                    periodeTom = null,
                    vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.OPPHØR_FLYTTET_FRA_NORGE),
                )

            // Act
            val vedtaksperiodeMedBegrunnelser = opphørsperiode.tilVedtaksperiodeMedBegrunnelse(vedtak)

            // Assert
            assertThat(vedtaksperiodeMedBegrunnelser.begrunnelser.size).isEqualTo(1)
            assertThat(vedtaksperiodeMedBegrunnelser.begrunnelser.single().nasjonalEllerFellesBegrunnelse).isEqualTo(NasjonalEllerFellesBegrunnelse.OPPHØR_FLYTTET_FRA_NORGE)
            assertThat(vedtaksperiodeMedBegrunnelser.fom).isEqualTo(førsteAugust2024)
            assertThat(vedtaksperiodeMedBegrunnelser.tom).isNull()
        }
    }
}
