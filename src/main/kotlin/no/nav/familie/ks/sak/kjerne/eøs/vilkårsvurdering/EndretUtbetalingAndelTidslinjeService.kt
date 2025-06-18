package no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import java.math.BigDecimal
import java.time.LocalDate

internal fun Iterable<EndretUtbetalingAndel>.tilBarnasSkalIkkeUtbetalesTidslinjer(): Map<Aktør, Tidslinje<Boolean>> =
    this
        .filter { it.årsak in listOf(Årsak.ETTERBETALING_3MND, Årsak.ALLEREDE_UTBETALT) && it.prosent == BigDecimal.ZERO }
        .flatMap { andel ->
            andel.personer
                .filter { it.type == PersonType.BARN }
                .map { it.aktør to andel }
        }.groupBy({ it.first }, { it.second })
        .mapValues { (_, endringer) ->
            endringer
                .map { it.tilPeriode() }
                .tilTidslinje()
        }

private fun EndretUtbetalingAndel.tilPeriode() =
    Periode(
        fom = this.fom?.førsteDagIInneværendeMåned() ?: LocalDate.now(),
        tom = this.tom?.sisteDagIInneværendeMåned() ?: LocalDate.now(),
        verdi = true,
    )
