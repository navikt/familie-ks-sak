package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode


import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.util.inneværendeMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.MinimertPerson
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.tilMinimertPerson
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
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
) : Vedtaksperiode {
    fun tilTomPeriode() = Periode<Utbetalingsperiode>(
        verdi = null,
        fom = this.periodeFom,
        tom = this.periodeTom,
    )
}

data class UtbetalingsperiodeDetalj(
    val minimertPerson: MinimertPerson,
    val ytelseType: YtelseType,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean,
    val prosent: BigDecimal
)

fun List<UtbetalingsperiodeDetalj>.totaltUtbetalt(): Int =
    this.sumOf { it.utbetaltPerMnd }

fun hentUtbetalingsperiodeForVedtaksperiode(
    utbetalingsperioder: List<Utbetalingsperiode>,
    fom: LocalDate?
): Utbetalingsperiode {
    if (utbetalingsperioder.isEmpty()) {
        throw Feil("Det finnes ingen utbetalingsperioder ved utledning av utbetalingsperiode.")
    }
    val fomDato = fom?.toYearMonth() ?: inneværendeMåned()

    val sorterteUtbetalingsperioder = utbetalingsperioder.sortedBy { it.periodeFom }

    return sorterteUtbetalingsperioder.lastOrNull { it.periodeFom.toYearMonth() <= fomDato }
        ?: sorterteUtbetalingsperioder.firstOrNull()
        ?: throw Feil("Finner ikke gjeldende utbetalingsperiode ved fortsatt innvilget")
}

fun mapTilUtbetalingsperioder(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
): List<Utbetalingsperiode> {

    //TODO: HELP
    return andelerTilkjentYtelse.lagVertikaleSegmenter().map { (segment, andelerForSegment) ->
        Utbetalingsperiode(
            periodeFom = segment.fom,
            periodeTom = segment.tom,
            ytelseTyper = andelerForSegment.map(AndelTilkjentYtelseMedEndreteUtbetalinger::type),
            utbetaltPerMnd = segment.value,
            antallBarn = andelerForSegment.count { andel ->
                personopplysningGrunnlag.barna.any { barn -> barn.aktør == andel.aktør }
            },
            utbetalingsperiodeDetaljer = andelerForSegment.lagUtbetalingsperiodeDetaljer(personopplysningGrunnlag)
        )
    }
}

internal fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.lagUtbetalingsperiodeDetaljer(
    personopplysningGrunnlag: PersonopplysningGrunnlag
): List<UtbetalingsperiodeDetalj> =
    this.map { andel ->
        val personForAndel =
            personopplysningGrunnlag.personer.find { person -> andel.aktør == person.aktør }
                ?: throw IllegalStateException("Fant ikke personopplysningsgrunnlag for andel")

        UtbetalingsperiodeDetalj(
            minimertPerson = personForAndel.tilMinimertPerson(),
            ytelseType = andel.type,
            utbetaltPerMnd = andel.kalkulertUtbetalingsbeløp,
            erPåvirketAvEndring = andel.endreteUtbetalinger.isNotEmpty(),
            prosent = andel.prosent
        )
    }
