package no.nav.familie.ks.sak.kjerne.brev.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import java.math.BigDecimal

data class BrevUtbetalingsperiodeDetalj(
    val person: BrevPerson,
    val ytelseType: YtelseType,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean,
    val prosent: BigDecimal
)

fun UtbetalingsperiodeDetalj.tilBrevUtbetalingsperiodeDetalj() = BrevUtbetalingsperiodeDetalj(
    person = this.person.tilBrevPerson(),
    ytelseType = this.ytelseType,
    utbetaltPerMnd = this.utbetaltPerMnd,
    erPåvirketAvEndring = this.erPåvirketAvEndring,
    prosent = this.prosent
)

fun List<BrevUtbetalingsperiodeDetalj>.antallBarn(): Int =
    this.filter { it.person.type == PersonType.BARN }.size

fun List<BrevUtbetalingsperiodeDetalj>.totaltUtbetalt(): Int =
    this.sumOf { it.utbetaltPerMnd }

fun List<BrevUtbetalingsperiodeDetalj>.beløpUtbetaltFor(
    personIdenter: List<String>
) = this
    .filter { utbetalingsperiodeDetalj -> personIdenter.contains(utbetalingsperiodeDetalj.person.aktivPersonIdent) }
    .totaltUtbetalt()
