package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.tilFørskjøvetOppfylteVilkårResultatTidslinjeMap
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class UtbetalingsperiodeUtilTest {
    val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val person1Fnr = randomFnr()
    private val person2Fnr = randomFnr()

    val personopplysningGrunnlag = lagPersonopplysningGrunnlag(barnasIdenter = listOf(person1Fnr, person2Fnr))

    private val person1 = personopplysningGrunnlag.barna[0].aktør
    private val person2 = personopplysningGrunnlag.barna[1].aktør

    @Test
    fun `hentPerioderMedUtbetaling skal beholde split i andel tilkjent ytelse`() {
        val mars2020 = YearMonth.of(2020, 3)
        val april2020 = YearMonth.of(2020, 4)
        val mai2020 = YearMonth.of(2020, 5)
        val juli2020 = YearMonth.of(2020, 7)

        val vedtak = Vedtak(behandling = behandling)

        val andelPerson1MarsTilApril =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = mars2020,
                    stønadTom = april2020,
                    sats = 1000,
                    aktør = person1,
                ),
                emptyList(),
            )

        val andelPerson1MaiTilJuli =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = mai2020,
                    stønadTom = juli2020,
                    sats = 1000,
                    aktør = person1,
                ),
                emptyList(),
            )

        val andelPerson2MarsTilJuli =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = mars2020,
                    stønadTom = juli2020,
                    sats = 1000,
                    aktør = person2,
                ),
                emptyList(),
            )

        val forventetResultat =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    vedtak = vedtak,
                    fom = mars2020.førsteDagIInneværendeMåned(),
                    tom = april2020.sisteDagIInneværendeMåned(),
                    type = Vedtaksperiodetype.UTBETALING,
                    begrunnelser = mutableSetOf(),
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    vedtak = vedtak,
                    fom = mai2020.førsteDagIInneværendeMåned(),
                    tom = juli2020.sisteDagIInneværendeMåned(),
                    type = Vedtaksperiodetype.UTBETALING,
                    begrunnelser = mutableSetOf(),
                ),
            )

        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        val personResultater =
            setOf(
                vilkårsvurdering.lagGodkjentPersonResultatForBarn(person1),
                vilkårsvurdering.lagGodkjentPersonResultatForBarn(person2),
            )

        val faktiskResultat =
            hentPerioderMedUtbetaling(
                listOf(andelPerson1MarsTilApril, andelPerson1MaiTilJuli, andelPerson2MarsTilJuli),
                vedtak,
                personResultater.tilFørskjøvetOppfylteVilkårResultatTidslinjeMap(personopplysningGrunnlag),
            )

        assertEquals(
            forventetResultat.map { Periode(it, it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
            faktiskResultat.map { Periode(it, it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
        )

        assertEquals(
            forventetResultat.map { it.type }.toSet(),
            faktiskResultat.map { it.type }.toSet(),
        )
    }

    @Test
    fun `hentPerioderMedUtbetaling skal splitte på forskjellige personer`() {
        val mars2020 = YearMonth.of(2020, 3)
        val april2020 = YearMonth.of(2020, 4)
        val mai2020 = YearMonth.of(2020, 5)
        val juni2020 = YearMonth.of(2020, 6)
        val juli2020 = YearMonth.of(2020, 7)

        val vedtak = Vedtak(behandling = behandling)

        val andelPerson1MarsTilMai =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = mars2020,
                    stønadTom = mai2020,
                    sats = 1000,
                    aktør = person1,
                ),
                emptyList(),
            )

        val andelPerson2MaiTilJuli =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = mai2020,
                    stønadTom = juli2020,
                    sats = 1000,
                    aktør = person2,
                ),
                emptyList(),
            )

        val forventetResultat =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    vedtak = vedtak,
                    fom = mars2020.førsteDagIInneværendeMåned(),
                    tom = april2020.sisteDagIInneværendeMåned(),
                    type = Vedtaksperiodetype.UTBETALING,
                    begrunnelser = mutableSetOf(),
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    vedtak = vedtak,
                    fom = mai2020.førsteDagIInneværendeMåned(),
                    tom = mai2020.sisteDagIInneværendeMåned(),
                    type = Vedtaksperiodetype.UTBETALING,
                    begrunnelser = mutableSetOf(),
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    vedtak = vedtak,
                    fom = juni2020.førsteDagIInneværendeMåned(),
                    tom = juli2020.sisteDagIInneværendeMåned(),
                    type = Vedtaksperiodetype.UTBETALING,
                    begrunnelser = mutableSetOf(),
                ),
            )

        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        val personResultater =
            setOf(
                vilkårsvurdering.lagGodkjentPersonResultatForBarn(person1),
                vilkårsvurdering.lagGodkjentPersonResultatForBarn(person2),
            )

        val faktiskResultat =
            hentPerioderMedUtbetaling(
                listOf(andelPerson1MarsTilMai, andelPerson2MaiTilJuli),
                vedtak,
                personResultater.tilFørskjøvetOppfylteVilkårResultatTidslinjeMap(personopplysningGrunnlag),
            )

        assertEquals(
            forventetResultat.map { Periode(it, it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
            faktiskResultat.map { Periode(it, it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
        )

        assertEquals(
            forventetResultat.map { it.type }.toSet(),
            faktiskResultat.map { it.type }.toSet(),
        )
    }

    private fun Vilkårsvurdering.lagGodkjentPersonResultatForBarn(aktør: Aktør) =
        lagPersonResultat(
            vilkårsvurdering = this,
            aktør = aktør,
            resultat = Resultat.OPPFYLT,
            periodeFom = LocalDate.now().minusYears(1),
            periodeTom = LocalDate.now().plusYears(2),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
        )
}
