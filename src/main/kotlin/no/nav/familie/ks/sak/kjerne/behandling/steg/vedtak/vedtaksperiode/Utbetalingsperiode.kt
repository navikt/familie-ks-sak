package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.tilKombinertTidslinjePerAktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Dataklasser som brukes til frontend og backend når man jobber med vertikale utbetalingsperioder
 */

data class Utbetalingsperiode(
    override val periodeFom: LocalDate,
    override val periodeTom: LocalDate,
    override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj>,
    val ytelseTyper: List<YtelseType>,
    val antallBarn: Int,
    val utbetaltPerMnd: Int
) : Vedtaksperiode

data class UtbetalingsperiodeDetalj(
    val person: Person,
    val ytelseType: YtelseType,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean,
    val prosent: BigDecimal
)

fun mapTilUtbetalingsperioder(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
): List<Utbetalingsperiode> {
    val kombinertTidslinjePerAktør = andelerTilkjentYtelse.tilKombinertTidslinjePerAktør()

    val utbetalingsPerioder = kombinertTidslinjePerAktør.tilPerioderIkkeNull().map {
        Utbetalingsperiode(
            periodeFom = it.fom ?: TIDENES_MORGEN,
            periodeTom = it.tom ?: TIDENES_ENDE,
            ytelseTyper = it.verdi.map { andelTilkjentYtelse -> andelTilkjentYtelse.type },
            utbetaltPerMnd = it.verdi.sumOf { andelTilkjentYtelse -> andelTilkjentYtelse.kalkulertUtbetalingsbeløp },
            antallBarn = it.verdi.count { andel -> personopplysningGrunnlag.barna.any { barn -> barn.aktør == andel.aktør } },
            utbetalingsperiodeDetaljer = it.verdi.lagUtbetalingsperiodeDetaljer(personopplysningGrunnlag)
        )
    }

    return utbetalingsPerioder
}

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilTidslinje() = map {
    Periode(
        it,
        it.stønadFom.førsteDagIInneværendeMåned(),
        it.stønadTom.sisteDagIInneværendeMåned()
    )
}.tilTidslinje()

internal fun Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>.lagUtbetalingsperiodeDetaljer(
    personopplysningGrunnlag: PersonopplysningGrunnlag
): List<UtbetalingsperiodeDetalj> = this.map { andel ->
    val personForAndel =
        personopplysningGrunnlag.personer.find { person -> andel.aktør == person.aktør } ?: throw IllegalStateException(
            "Fant ikke personopplysningsgrunnlag for andel"
        )

    UtbetalingsperiodeDetalj(
        person = personForAndel,
        ytelseType = andel.type,
        utbetaltPerMnd = andel.kalkulertUtbetalingsbeløp,
        erPåvirketAvEndring = andel.endreteUtbetalinger.isNotEmpty(),
        prosent = andel.prosent
    )
}
