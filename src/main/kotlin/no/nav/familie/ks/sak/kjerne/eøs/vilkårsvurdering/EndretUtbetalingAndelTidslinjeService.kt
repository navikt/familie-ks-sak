package no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EndretUtbetalingAndelTidslinjeService(private val endretUtbetalingAndelService: EndretUtbetalingAndelService) {
    fun hentBarnasHarEtterbetaling3MånedTidslinjer(behandlingId: Long) =
        endretUtbetalingAndelService
            .hentEndredeUtbetalingAndeler(behandlingId)
            .tilBarnasHarEtterbetaling3MånedTidslinjer()
}

internal fun Iterable<EndretUtbetalingAndel>.tilBarnasHarEtterbetaling3MånedTidslinjer(): Map<Aktør, Tidslinje<Boolean>> {
    return this.filter { it.årsak == Årsak.ETTERBETALING_3MND }
        .filter { it.person?.type == PersonType.BARN }
        .filter { it.person?.aktør != null }
        .groupBy { checkNotNull(it.person?.aktør) }
        .mapValues { (_, endringer) -> endringer.map { it.tilPeriode { true } }.tilTidslinje() }
}

private fun <T> EndretUtbetalingAndel.tilPeriode(mapper: (EndretUtbetalingAndel) -> T) =
    Periode(
        fom = this.fom?.førsteDagIInneværendeMåned() ?: LocalDate.now(),
        tom = this.tom?.sisteDagIInneværendeMåned() ?: LocalDate.now(),
        verdi = mapper(this),
    )
