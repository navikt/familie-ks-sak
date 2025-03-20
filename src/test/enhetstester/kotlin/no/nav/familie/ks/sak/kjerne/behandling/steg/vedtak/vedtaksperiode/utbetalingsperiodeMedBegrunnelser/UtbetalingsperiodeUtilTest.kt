package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser

import apr
import io.mockk.every
import io.mockk.mockk
import mars
import mockAdopsjonService
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagKompetanse
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.tilForskjøvetOppfylteVilkårResultatTidslinjeMap
import no.nav.familie.ks.sak.kjerne.beregning.AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.BeregnAndelTilkjentYtelseService
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseService
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.lovverkFebruar2025.LovverkFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.lovverkFørFebruar2025.LovverkFørFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndelRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.praksisendring.Praksisendring2024Service
import no.nav.familie.tidslinje.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class UtbetalingsperiodeUtilTest {
    val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    val vedtak = Vedtak(behandling = behandling)

    private val barn1Fnr = randomFnr(fødselsdato = 15.mars(2020))
    private val barn2Fnr = randomFnr(fødselsdato = 15.apr(2022))
    val personopplysningGrunnlag = lagPersonopplysningGrunnlag(barnasIdenter = listOf(barn1Fnr, barn2Fnr))

    private val søker = personopplysningGrunnlag.søker
    private val barn1 = personopplysningGrunnlag.barna[0]
    private val barn2 = personopplysningGrunnlag.barna[1]

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
                    aktør = barn1.aktør,
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
                    aktør = barn1.aktør,
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
                    aktør = barn2.aktør,
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
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    vedtak = vedtak,
                    fom = mai2020.førsteDagIInneværendeMåned(),
                    tom = juli2020.sisteDagIInneværendeMåned(),
                    type = Vedtaksperiodetype.UTBETALING,
                ),
            )

        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        val personResultater =
            setOf(
                vilkårsvurdering.lagGodkjentPersonResultatForSøker(søker),
                vilkårsvurdering.lagGodkjentPersonResultatForBarn(barn1),
                vilkårsvurdering.lagGodkjentPersonResultatForBarn(barn2),
            )

        val faktiskResultat =
            hentPerioderMedUtbetaling(
                andelerTilkjentYtelse = listOf(andelPerson1MarsTilApril, andelPerson1MaiTilJuli, andelPerson2MarsTilJuli),
                vedtak = vedtak,
                forskjøvetVilkårResultatTidslinjeMap =
                    personResultater.tilForskjøvetOppfylteVilkårResultatTidslinjeMap(
                        personopplysningGrunnlag = personopplysningGrunnlag,
                        adopsjonerIBehandling = emptyList(),
                    ),
                kompetanser = emptyList(),
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
                    aktør = barn1.aktør,
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
                    aktør = barn2.aktør,
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
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    vedtak = vedtak,
                    fom = mai2020.førsteDagIInneværendeMåned(),
                    tom = mai2020.sisteDagIInneværendeMåned(),
                    type = Vedtaksperiodetype.UTBETALING,
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    vedtak = vedtak,
                    fom = juni2020.førsteDagIInneværendeMåned(),
                    tom = juli2020.sisteDagIInneværendeMåned(),
                    type = Vedtaksperiodetype.UTBETALING,
                ),
            )

        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        val personResultater =
            setOf(
                vilkårsvurdering.lagGodkjentPersonResultatForSøker(søker),
                vilkårsvurdering.lagGodkjentPersonResultatForBarn(barn1),
                vilkårsvurdering.lagGodkjentPersonResultatForBarn(barn2),
            )

        val faktiskResultat =
            hentPerioderMedUtbetaling(
                andelerTilkjentYtelse = listOf(andelPerson1MarsTilMai, andelPerson2MaiTilJuli),
                vedtak = vedtak,
                forskjøvetVilkårResultatTidslinjeMap =
                    personResultater.tilForskjøvetOppfylteVilkårResultatTidslinjeMap(
                        personopplysningGrunnlag = personopplysningGrunnlag,
                        adopsjonerIBehandling = emptyList(),
                    ),
                kompetanser = emptyList(),
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

    @Nested
    inner class EØS {
        @Test
        fun `Skal lage ny vedtaksperiode dersom vi får en ny kompetansene`() {
            val (vilkårsvurdering, tilkjentYtelse) = kjørBehandlingFramTilBehandlingsresultatMedAltGodkjent()
            val forskjøvetVilkårResultatTidslinjeMap =
                vilkårsvurdering.personResultater.tilForskjøvetOppfylteVilkårResultatTidslinjeMap(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    adopsjonerIBehandling = emptyList(),
                )

            val vedtaksperioderUtenKompetanse =
                hentPerioderMedUtbetaling(
                    andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.map { AndelTilkjentYtelseMedEndreteUtbetalinger(it, emptyList()) },
                    vedtak = vedtak,
                    forskjøvetVilkårResultatTidslinjeMap = forskjøvetVilkårResultatTidslinjeMap,
                    kompetanser = emptyList(),
                )

            assertThat(vedtaksperioderUtenKompetanse.size).isEqualTo(2)

            val kompetanser =
                listOf(
                    lagKompetanse(
                        fom = tilkjentYtelse.andelerTilkjentYtelse.first().stønadFom,
                        tom =
                            tilkjentYtelse.andelerTilkjentYtelse
                                .first()
                                .stønadFom
                                .plusMonths(1),
                        barnAktører = setOf(barn1.aktør),
                    ),
                )

            val vedtaksperioderMedKompetanse =
                hentPerioderMedUtbetaling(
                    andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.map { AndelTilkjentYtelseMedEndreteUtbetalinger(it, emptyList()) },
                    vedtak = vedtak,
                    forskjøvetVilkårResultatTidslinjeMap = forskjøvetVilkårResultatTidslinjeMap,
                    kompetanser = kompetanser,
                )

            assertThat(vedtaksperioderMedKompetanse.size).isEqualTo(3)
        }

        @Test
        fun `Skal lage ny vedtaksperiode dersom det er endring i kompetansene`() {
            val (vilkårsvurdering, tilkjentYtelse) = kjørBehandlingFramTilBehandlingsresultatMedAltGodkjent()
            val forskjøvetVilkårResultatTidslinjeMap =
                vilkårsvurdering.personResultater.tilForskjøvetOppfylteVilkårResultatTidslinjeMap(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    adopsjonerIBehandling = emptyList(),
                )

            val vedtaksperioderUtenKompetanse =
                hentPerioderMedUtbetaling(
                    andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.map { AndelTilkjentYtelseMedEndreteUtbetalinger(it, emptyList()) },
                    vedtak = vedtak,
                    forskjøvetVilkårResultatTidslinjeMap = forskjøvetVilkårResultatTidslinjeMap,
                    kompetanser = emptyList(),
                )

            assertThat(vedtaksperioderUtenKompetanse.size).isEqualTo(2)

            val kompetanser =
                listOf(
                    lagKompetanse(
                        fom = tilkjentYtelse.andelerTilkjentYtelse.first().stønadFom,
                        tom = tilkjentYtelse.andelerTilkjentYtelse.first().stønadFom,
                        barnAktører = setOf(barn1.aktør),
                        annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER,
                    ),
                    lagKompetanse(
                        fom =
                            tilkjentYtelse.andelerTilkjentYtelse
                                .first()
                                .stønadFom
                                .plusMonths(1),
                        tom =
                            tilkjentYtelse.andelerTilkjentYtelse
                                .first()
                                .stønadFom
                                .plusMonths(1),
                        barnAktører = setOf(barn1.aktør),
                        annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                    ),
                )

            val vedtaksperioderMedKompetanse =
                hentPerioderMedUtbetaling(
                    andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.map { AndelTilkjentYtelseMedEndreteUtbetalinger(it, emptyList()) },
                    vedtak = vedtak,
                    forskjøvetVilkårResultatTidslinjeMap = forskjøvetVilkårResultatTidslinjeMap,
                    kompetanser = kompetanser,
                )

            assertThat(vedtaksperioderMedKompetanse.size).isEqualTo(4)
        }
    }

    @Nested
    inner class HentBegrunnelserForOvergangsordningPerioder {
        @Test
        fun `skal ikke returnere noe begrunnelser dersom det ikke finnes noe overgangsordningandeler i perioden`() {
            val andeler =
                listOf(
                    AndelTilkjentYtelseMedEndreteUtbetalinger(
                        andelTilkjentYtelse = lagAndelTilkjentYtelse(stønadFom = YearMonth.of(2024, 8), stønadTom = YearMonth.of(2024, 12)),
                        endreteUtbetalingerAndeler = emptyList(),
                    ),
                )

            val splittKriteriePeriode =
                Periode(
                    SplittkriterierForVedtaksperiode(
                        andelerTilkjentYtelse = andeler,
                        splittkriterierForVilkår = emptyMap(),
                        splittkriterierForKompetanse = emptyMap(),
                    ),
                    fom = LocalDate.of(2024, 8, 1),
                    tom = LocalDate.of(2024, 12, 31),
                )

            val begrunnelser = hentBegrunnelserForOvergangsordningPerioder(splittKriteriePeriode)

            assertThat(begrunnelser).isEmpty()
        }

        @Test
        fun `skal ikke returnere noe begrunnelser dersom overgangsordning andelene ikke overlapper med vedtaksperioden`() {
            val andeler =
                listOf(
                    AndelTilkjentYtelseMedEndreteUtbetalinger(
                        andelTilkjentYtelse = lagAndelTilkjentYtelse(stønadFom = YearMonth.of(2024, 6), stønadTom = YearMonth.of(2024, 7), ytelseType = YtelseType.OVERGANGSORDNING),
                        endreteUtbetalingerAndeler = emptyList(),
                    ),
                )

            val splittKriteriePeriode =
                Periode(
                    SplittkriterierForVedtaksperiode(
                        andelerTilkjentYtelse = andeler,
                        splittkriterierForVilkår = emptyMap(),
                        splittkriterierForKompetanse = emptyMap(),
                    ),
                    fom = LocalDate.of(2024, 8, 1),
                    tom = LocalDate.of(2024, 12, 31),
                )

            val begrunnelser = hentBegrunnelserForOvergangsordningPerioder(splittKriteriePeriode)

            assertThat(begrunnelser).isEmpty()
        }

        @Test
        fun `skal returnere noe begrunnelser dersom overgangsordning andelene ikke overlapper med vedtaksperioden`() {
            val andeler =
                listOf(
                    AndelTilkjentYtelseMedEndreteUtbetalinger(
                        andelTilkjentYtelse = lagAndelTilkjentYtelse(stønadFom = YearMonth.of(2024, 6), stønadTom = YearMonth.of(2024, 7), ytelseType = YtelseType.OVERGANGSORDNING),
                        endreteUtbetalingerAndeler = emptyList(),
                    ),
                )

            val splittKriteriePeriode =
                Periode(
                    SplittkriterierForVedtaksperiode(
                        andelerTilkjentYtelse = andeler,
                        splittkriterierForVilkår = emptyMap(),
                        splittkriterierForKompetanse = emptyMap(),
                    ),
                    fom = LocalDate.of(2024, 8, 1),
                    tom = LocalDate.of(2024, 12, 31),
                )

            val begrunnelser = hentBegrunnelserForOvergangsordningPerioder(splittKriteriePeriode)

            assertThat(begrunnelser).isEmpty()
        }

        @Test
        fun `skal returnere INNVILGET_OVERGANGSORDNING begrunnelse dersom det eksisterer overgangsordning andeler med full sats som overlapper med vedtak perioden`() {
            val andeler =
                listOf(
                    AndelTilkjentYtelseMedEndreteUtbetalinger(
                        andelTilkjentYtelse = lagAndelTilkjentYtelse(stønadFom = YearMonth.of(2024, 7), stønadTom = YearMonth.of(2024, 9), ytelseType = YtelseType.OVERGANGSORDNING),
                        endreteUtbetalingerAndeler = emptyList(),
                    ),
                )

            val splittKriteriePeriode =
                Periode(
                    SplittkriterierForVedtaksperiode(
                        andelerTilkjentYtelse = andeler,
                        splittkriterierForVilkår = emptyMap(),
                        splittkriterierForKompetanse = emptyMap(),
                    ),
                    fom = LocalDate.of(2024, 8, 1),
                    tom = LocalDate.of(2024, 12, 31),
                )

            val begrunnelser = hentBegrunnelserForOvergangsordningPerioder(splittKriteriePeriode)

            assertThat(begrunnelser).size().isEqualTo(1)
            assertThat(begrunnelser.single()).isEqualTo(NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING)
        }

        @Test
        fun `skal returnere INNVILGET_OVERGANGSORDNING_GRADERT_UTBETALING begrunnelse dersom det eksisterer overgangsordning andeler med gradert sats som overlapper med vedtak perioden`() {
            val andeler =
                listOf(
                    AndelTilkjentYtelseMedEndreteUtbetalinger(
                        andelTilkjentYtelse = lagAndelTilkjentYtelse(stønadFom = YearMonth.of(2024, 7), stønadTom = YearMonth.of(2024, 9), ytelseType = YtelseType.OVERGANGSORDNING, prosent = BigDecimal(60)),
                        endreteUtbetalingerAndeler = emptyList(),
                    ),
                )

            val splittKriteriePeriode =
                Periode(
                    SplittkriterierForVedtaksperiode(
                        andelerTilkjentYtelse = andeler,
                        splittkriterierForVilkår = emptyMap(),
                        splittkriterierForKompetanse = emptyMap(),
                    ),
                    fom = LocalDate.of(2024, 8, 1),
                    tom = LocalDate.of(2024, 12, 31),
                )

            val begrunnelser = hentBegrunnelserForOvergangsordningPerioder(splittKriteriePeriode)

            assertThat(begrunnelser).size().isEqualTo(1)
            assertThat(begrunnelser.single()).isEqualTo(NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING_GRADERT_UTBETALING)
        }

        @Test
        fun `skal returnere INNVILGET_OVERGANGSORDNING_DELT_BOSTED begrunnelse dersom det eksisterer overgangsordning andeler med halv sats som overlapper med vedtak perioden`() {
            val andeler =
                listOf(
                    AndelTilkjentYtelseMedEndreteUtbetalinger(
                        andelTilkjentYtelse = lagAndelTilkjentYtelse(stønadFom = YearMonth.of(2024, 7), stønadTom = YearMonth.of(2024, 9), ytelseType = YtelseType.OVERGANGSORDNING, prosent = BigDecimal(50)),
                        endreteUtbetalingerAndeler = emptyList(),
                    ),
                )

            val splittKriteriePeriode =
                Periode(
                    SplittkriterierForVedtaksperiode(
                        andelerTilkjentYtelse = andeler,
                        splittkriterierForVilkår = emptyMap(),
                        splittkriterierForKompetanse = emptyMap(),
                    ),
                    fom = LocalDate.of(2024, 8, 1),
                    tom = LocalDate.of(2024, 12, 31),
                )

            val begrunnelser = hentBegrunnelserForOvergangsordningPerioder(splittKriteriePeriode)

            assertThat(begrunnelser).size().isEqualTo(1)
            assertThat(begrunnelser.single()).isEqualTo(NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING_DELT_BOSTED)
        }

        @Test
        fun `skal returnere INNVILGET_OVERGANGSORDNING_GRADERT_UTBETALING begrunnelse dersom det både eksisterer overgangsordning andeler med gradert sats og full sats som overlapper med vedtak perioden`() {
            val andeler =
                listOf(
                    AndelTilkjentYtelseMedEndreteUtbetalinger(
                        andelTilkjentYtelse = lagAndelTilkjentYtelse(stønadFom = YearMonth.of(2024, 7), stønadTom = YearMonth.of(2024, 9), ytelseType = YtelseType.OVERGANGSORDNING, prosent = BigDecimal(100)),
                        endreteUtbetalingerAndeler = emptyList(),
                    ),
                    AndelTilkjentYtelseMedEndreteUtbetalinger(
                        andelTilkjentYtelse = lagAndelTilkjentYtelse(stønadFom = YearMonth.of(2024, 7), stønadTom = YearMonth.of(2024, 9), ytelseType = YtelseType.OVERGANGSORDNING, prosent = BigDecimal(60)),
                        endreteUtbetalingerAndeler = emptyList(),
                    ),
                )

            val splittKriteriePeriode =
                Periode(
                    SplittkriterierForVedtaksperiode(
                        andelerTilkjentYtelse = andeler,
                        splittkriterierForVilkår = emptyMap(),
                        splittkriterierForKompetanse = emptyMap(),
                    ),
                    fom = LocalDate.of(2024, 8, 1),
                    tom = LocalDate.of(2024, 12, 31),
                )

            val begrunnelser = hentBegrunnelserForOvergangsordningPerioder(splittKriteriePeriode)

            assertThat(begrunnelser).size().isEqualTo(1)
            assertThat(begrunnelser.single()).isEqualTo(NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING_GRADERT_UTBETALING)
        }

        @Test
        fun `skal returnere INNVILGET_OVERGANGSORDNING_DELT_BOSTED begrunnelse dersom det både eksisterer overgangsordning andeler med halv sats og full sats som overlapper med vedtak perioden`() {
            val andeler =
                listOf(
                    AndelTilkjentYtelseMedEndreteUtbetalinger(
                        andelTilkjentYtelse = lagAndelTilkjentYtelse(stønadFom = YearMonth.of(2024, 7), stønadTom = YearMonth.of(2024, 9), ytelseType = YtelseType.OVERGANGSORDNING, prosent = BigDecimal(100)),
                        endreteUtbetalingerAndeler = emptyList(),
                    ),
                    AndelTilkjentYtelseMedEndreteUtbetalinger(
                        andelTilkjentYtelse = lagAndelTilkjentYtelse(stønadFom = YearMonth.of(2024, 7), stønadTom = YearMonth.of(2024, 9), ytelseType = YtelseType.OVERGANGSORDNING, prosent = BigDecimal(50)),
                        endreteUtbetalingerAndeler = emptyList(),
                    ),
                )

            val splittKriteriePeriode =
                Periode(
                    SplittkriterierForVedtaksperiode(
                        andelerTilkjentYtelse = andeler,
                        splittkriterierForVilkår = emptyMap(),
                        splittkriterierForKompetanse = emptyMap(),
                    ),
                    fom = LocalDate.of(2024, 8, 1),
                    tom = LocalDate.of(2024, 12, 31),
                )

            val begrunnelser = hentBegrunnelserForOvergangsordningPerioder(splittKriteriePeriode)

            assertThat(begrunnelser).size().isEqualTo(1)
            assertThat(begrunnelser.single()).isEqualTo(NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING_DELT_BOSTED)
        }
    }

    data class DataEtterVilkårsvurdering(
        val vilkårsvurdering: Vilkårsvurdering,
        val tilkjentYtelse: TilkjentYtelse,
    )

    private fun kjørBehandlingFramTilBehandlingsresultatMedAltGodkjent(): DataEtterVilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val personResultater =
            setOf(
                vilkårsvurdering.lagGodkjentPersonResultatForSøker(søker),
                vilkårsvurdering.lagGodkjentPersonResultatForBarn(barn1),
                vilkårsvurdering.lagGodkjentPersonResultatForBarn(barn2),
            )
        vilkårsvurdering.personResultater = personResultater

        val tilkjentYtelseService =
            TilkjentYtelseService(
                beregnAndelTilkjentYtelseService =
                    BeregnAndelTilkjentYtelseService(
                        andelGeneratorLookup = AndelGenerator.Lookup(listOf(LovverkFebruar2025AndelGenerator(), LovverkFørFebruar2025AndelGenerator())),
                        adopsjonService = mockAdopsjonService(),
                    ),
                overgangsordningAndelRepository = mockOvergangsordningAndelRepository(),
                praksisendring2024Service = mockPraksisendring2024Service(),
            )

        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )
        return DataEtterVilkårsvurdering(vilkårsvurdering, tilkjentYtelse)
    }

    private fun Vilkårsvurdering.lagGodkjentPersonResultatForBarn(person: Person) =
        lagPersonResultat(
            vilkårsvurdering = this,
            aktør = person.aktør,
            resultat = Resultat.OPPFYLT,
            periodeFom = person.fødselsdato.plusYears(1),
            periodeTom = person.fødselsdato.plusYears(2),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
        )

    private fun Vilkårsvurdering.lagGodkjentPersonResultatForSøker(person: Person) =
        lagPersonResultat(
            vilkårsvurdering = this,
            aktør = person.aktør,
            resultat = Resultat.OPPFYLT,
            periodeFom = person.fødselsdato,
            periodeTom = null,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER,
        )

    private fun mockOvergangsordningAndelRepository(): OvergangsordningAndelRepository =
        mockk<OvergangsordningAndelRepository>().apply {
            every { hentOvergangsordningAndelerForBehandling(any()) } returns emptyList()
        }

    private fun mockPraksisendring2024Service() =
        mockk<Praksisendring2024Service>().apply {
            every { genererAndelerForPraksisendring2024(any(), any(), any()) } returns emptyList()
        }
}
