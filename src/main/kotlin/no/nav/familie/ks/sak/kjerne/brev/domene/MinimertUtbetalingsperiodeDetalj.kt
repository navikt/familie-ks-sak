package no.nav.familie.ks.sak.kjerne.brev.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.MinimertRestPerson
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import java.math.BigDecimal

data class MinimertUtbetalingsperiodeDetalj(
    val person: MinimertRestPerson,
    val ytelseType: YtelseType,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean,
    val prosent: BigDecimal
)

fun UtbetalingsperiodeDetalj.tilMinimertUtbetalingsperiodeDetalj() = MinimertUtbetalingsperiodeDetalj(
    person = this.person.tilMinimertPerson(),
    ytelseType = this.ytelseType,
    utbetaltPerMnd = this.utbetaltPerMnd,
    erPåvirketAvEndring = this.erPåvirketAvEndring,
    prosent = this.prosent
)

fun List<MinimertUtbetalingsperiodeDetalj>.antallBarn(): Int =
    this.filter { it.person.type == PersonType.BARN }.size

fun List<MinimertUtbetalingsperiodeDetalj>.totaltUtbetalt(): Int =
    this.sumOf { it.utbetaltPerMnd }

fun List<MinimertUtbetalingsperiodeDetalj>.beløpUtbetaltFor(
    personIdenter: List<String>
) = this
    .filter { utbetalingsperiodeDetalj -> personIdenter.contains(utbetalingsperiodeDetalj.person.personIdent) }
    .totaltUtbetalt()
