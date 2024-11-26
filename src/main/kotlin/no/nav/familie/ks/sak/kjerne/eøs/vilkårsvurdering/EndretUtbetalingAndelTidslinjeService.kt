package no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class EndretUtbetalingAndelTidslinjeService(
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
) {
    fun hentBarnasSkalIkkeUtbetalesTidslinjer(behandlingId: Long) =
        endretUtbetalingAndelService
            .hentEndredeUtbetalingAndeler(behandlingId)
            .tilBarnasSkalIkkeUtbetalesTidslinjer()
}

internal fun Iterable<EndretUtbetalingAndel>.tilBarnasSkalIkkeUtbetalesTidslinjer(): Map<Aktør, Tidslinje<Boolean>> =
    this
        .filter { it.årsak in listOf(Årsak.ETTERBETALING_3MND, Årsak.ALLEREDE_UTBETALT, Årsak.ENDRE_MOTTAKER) && it.prosent == BigDecimal.ZERO }
        .filter { it.person?.type == PersonType.BARN }
        .filter { it.person?.aktør != null }
        .groupBy { checkNotNull(it.person?.aktør) }
        .mapValues { (_, endringer) -> endringer.map { it.tilPeriode { true } }.tilTidslinje() }

private fun <T> EndretUtbetalingAndel.tilPeriode(mapper: (EndretUtbetalingAndel) -> T) =
    Periode(
        fom = this.fom?.førsteDagIInneværendeMåned() ?: LocalDate.now(),
        tom = this.tom?.sisteDagIInneværendeMåned() ?: LocalDate.now(),
        verdi = mapper(this),
    )
