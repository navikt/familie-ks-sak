package no.nav.familie.ks.sak.kjerne.brev.domene

import no.nav.familie.ks.sak.common.util.NullableMånedPeriode
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import java.time.LocalDate

class BrevVedtaksPeriode(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val ytelseTyperForPeriode: Set<YtelseType>,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<BegrunnelseMedTriggere>,
    val brevUtbetalingsperiodeDetaljer: List<BrevUtbetalingsperiodeDetalj>
) {
    fun finnEndredeAndelerISammePeriode(
        endretUtbetalingAndeler: List<BrevEndretUtbetalingAndel>
    ) = endretUtbetalingAndeler.filter {
        it.erOverlappendeMed(
            NullableMånedPeriode(
                this.fom?.toYearMonth(),
                this.tom?.toYearMonth()
            )
        )
    }
}

fun UtvidetVedtaksperiodeMedBegrunnelser.tilBrevVedtaksPeriode(): BrevVedtaksPeriode {
    return BrevVedtaksPeriode(
        fom = this.fom,
        tom = this.tom,
        ytelseTyperForPeriode = this.utbetalingsperiodeDetaljer.map { it.ytelseType }.toSet(),
        type = this.type,
        brevUtbetalingsperiodeDetaljer = this.utbetalingsperiodeDetaljer
    )
}
