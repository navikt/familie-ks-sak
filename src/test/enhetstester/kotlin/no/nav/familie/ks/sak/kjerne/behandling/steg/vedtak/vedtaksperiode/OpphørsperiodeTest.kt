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
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OpphørsperiodeTest {
    val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val søkerFnr = randomFnr()
    private val barn1Fnr = randomFnr()

    val personopplysningGrunnlag = lagPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr))

    val søker = fnrTilAktør(søkerFnr)
    val barn1 = fnrTilAktør(barn1Fnr)

    val vilkårsvurdering =
        Vilkårsvurdering(
            behandling = behandling,
        )

    @Test
    fun `mapTilOpphørsperioder skal utlede opphørsperiode mellom oppfylte perioder`() {
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
                erToggleForLovendringAugust2024På = true,
            )

        assertEquals(2, opphørsperioder.size)
        assertEquals(periodeTomFørsteAndel.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(periodeFomAndreAndel.forrigeMåned(), opphørsperioder[0].periodeTom?.toYearMonth())
        assertEquals(periodeTomAndreAndel.nesteMåned(), opphørsperioder[1].periodeFom.toYearMonth())
        assertEquals(periodeFomSisteAndel.forrigeMåned(), opphørsperioder[1].periodeTom?.toYearMonth())
    }

    @Test
    fun `mapTilOpphørsperioder skal utlede opphørsperiode når siste utbetalingsperiode er før neste måned`() {
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
                erToggleForLovendringAugust2024På = true,
            )

        assertEquals(1, opphørsperioder.size)
        assertEquals(periodeTomFørsteAndel.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(null, opphørsperioder[0].periodeTom)
    }

    @Test
    fun `mapTilOpphørsperioder skal utlede opphørsperiode fra neste måned når siste utbetalingsperiode er inneværende måned`() {
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
                erToggleForLovendringAugust2024På = true,
            )

        assertEquals(1, opphørsperioder.size)
        assertEquals(periodeTomFørsteAndel.nesteMåned(), opphørsperioder[0].periodeFom.toYearMonth())
        assertEquals(null, opphørsperioder[0].periodeTom)
    }

    @Test
    fun `mapTilOpphørsperioder skal slå sammen to like opphørsperioder`() {
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
    fun `mapTilOpphørsperioder skal slå sammen to opphørsperioder med ulik sluttdato`() {
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
    fun `mapTilOpphørsperioder skal slå sammen to opphørsperioder med ulik startdato`() {
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
    fun `mapTilOpphørsperioder skal slå sammen to opphørsperioder som overlapper`() {
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
