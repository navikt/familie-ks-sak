package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPerson
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBrevPerson
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
) : Vedtaksperiode

data class UtbetalingsperiodeDetalj(
    val brevPerson: BrevPerson,
    val ytelseType: YtelseType,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean,
    val prosent: BigDecimal
)

fun mapTilUtbetalingsperioder(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
): List<Utbetalingsperiode> {

    //TODO: HELP
    // return andelerTilkjentYtelse.lagVertikaleSegmenter().map { (segment, andelerForSegment) ->
    //     Utbetalingsperiode(
    //         periodeFom = segment.fom,
    //         periodeTom = segment.tom,
    //         ytelseTyper = andelerForSegment.map(AndelTilkjentYtelseMedEndreteUtbetalinger::type),
    //         utbetaltPerMnd = segment.value,
    //         antallBarn = andelerForSegment.count { andel ->
    //             personopplysningGrunnlag.barna.any { barn -> barn.aktør == andel.aktør }
    //         },
    //         utbetalingsperiodeDetaljer = andelerForSegment.lagUtbetalingsperiodeDetaljer(personopplysningGrunnlag)
    //     )
    // }

    return emptyList()
}

internal fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.lagUtbetalingsperiodeDetaljer(
    personopplysningGrunnlag: PersonopplysningGrunnlag
): List<UtbetalingsperiodeDetalj> =
    this.map { andel ->
        val personForAndel =
            personopplysningGrunnlag.personer.find { person -> andel.aktør == person.aktør }
                ?: throw IllegalStateException("Fant ikke personopplysningsgrunnlag for andel")

        UtbetalingsperiodeDetalj(
            brevPerson = personForAndel.tilBrevPerson(),
            ytelseType = andel.type,
            utbetaltPerMnd = andel.kalkulertUtbetalingsbeløp,
            erPåvirketAvEndring = andel.endreteUtbetalinger.isNotEmpty(),
            prosent = andel.prosent
        )
    }
