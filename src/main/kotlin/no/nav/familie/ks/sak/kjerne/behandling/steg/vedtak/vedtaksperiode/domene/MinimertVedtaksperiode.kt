package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene

import no.nav.familie.ks.sak.common.util.NullableMånedPeriode
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import java.time.LocalDate

class MinimertVedtaksperiode(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val ytelseTyperForPeriode: Set<YtelseType>,
    val type: Vedtaksperiodetype,
    val utbetalingsperioder: List<UtbetalingsperiodeDetalj>
) {
    fun finnEndredeAndelerISammePeriode(
        endretUtbetalingAndeler: List<MinimertEndretAndel>
    ) = endretUtbetalingAndeler.filter {
        it.erOverlappendeMed(
            NullableMånedPeriode(
                this.fom?.toYearMonth(),
                this.tom?.toYearMonth()
            )
        )
    }
}

fun UtvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(): MinimertVedtaksperiode {
    return MinimertVedtaksperiode(
        fom = this.fom,
        tom = this.tom,
        ytelseTyperForPeriode = this.utbetalingsperiodeDetaljer.map { it.ytelseType }.toSet(),
        type = this.type,
        utbetalingsperioder = this.utbetalingsperiodeDetaljer
    )
}
