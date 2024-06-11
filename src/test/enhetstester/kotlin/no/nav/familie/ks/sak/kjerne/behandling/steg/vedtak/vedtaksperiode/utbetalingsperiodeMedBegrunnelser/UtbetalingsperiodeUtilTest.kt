package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser

import apr
import mars
import no.nav.familie.ks.sak.common.tidslinje.Periode
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
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårRegelsett
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.tilForskjøvetOppfylteVilkårResultatTidslinjeMap
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseUtils
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
                vilkårsvurdering.lagGodkjentPersonResultatForBarn(barn1),
                vilkårsvurdering.lagGodkjentPersonResultatForBarn(barn2),
            )

        val faktiskResultat =
            hentPerioderMedUtbetaling(
                andelerTilkjentYtelse = listOf(andelPerson1MarsTilApril, andelPerson1MaiTilJuli, andelPerson2MarsTilJuli),
                vedtak = vedtak,
                forskjøvetVilkårResultatTidslinjeMap = personResultater.tilForskjøvetOppfylteVilkårResultatTidslinjeMap(personopplysningGrunnlag),
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
                vilkårsvurdering.lagGodkjentPersonResultatForBarn(barn1),
                vilkårsvurdering.lagGodkjentPersonResultatForBarn(barn2),
            )

        val faktiskResultat =
            hentPerioderMedUtbetaling(
                andelerTilkjentYtelse = listOf(andelPerson1MarsTilMai, andelPerson2MaiTilJuli),
                vedtak = vedtak,
                forskjøvetVilkårResultatTidslinjeMap = personResultater.tilForskjøvetOppfylteVilkårResultatTidslinjeMap(personopplysningGrunnlag),
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
            val forskjøvetVilkårResultatTidslinjeMap = vilkårsvurdering.personResultater.tilForskjøvetOppfylteVilkårResultatTidslinjeMap(personopplysningGrunnlag)

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
                        tom = tilkjentYtelse.andelerTilkjentYtelse.first().stønadFom.plusMonths(1),
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
            val forskjøvetVilkårResultatTidslinjeMap = vilkårsvurdering.personResultater.tilForskjøvetOppfylteVilkårResultatTidslinjeMap(personopplysningGrunnlag)

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
                        fom = tilkjentYtelse.andelerTilkjentYtelse.first().stønadFom.plusMonths(1),
                        tom = tilkjentYtelse.andelerTilkjentYtelse.first().stønadFom.plusMonths(1),
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

        val tilkjentYtelse =
            TilkjentYtelseUtils.beregnTilkjentYtelse(
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
            regelsett = VilkårRegelsett.LOV_AUGUST_2021,
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
            regelsett = VilkårRegelsett.LOV_AUGUST_2021,
        )
}
