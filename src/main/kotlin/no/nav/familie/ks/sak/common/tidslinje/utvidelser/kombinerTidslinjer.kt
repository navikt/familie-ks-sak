package no.nav.familie.ks.sak.common.tidslinje.utvidelser

import no.nav.familie.ks.sak.common.tidslinje.Null
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.Udefinert
import no.nav.familie.ks.sak.common.tidslinje.Verdi
import no.nav.familie.ks.sak.common.tidslinje.tomTidslinje

fun <T> Collection<Tidslinje<T>>.slåSammen(): Tidslinje<Collection<T>> {
    val minsteTidspunkt = this.minOf { it.startsTidspunkt }
    return this.fold(tomTidslinje(startsTidspunkt = minsteTidspunkt)) { sammenlagt, neste ->
        sammenlagt.biFunksjon(neste) { periodeVerdiFraSammenlagt, periodeVerdiFraNeste ->
            when (periodeVerdiFraSammenlagt) {
                is Verdi -> when (periodeVerdiFraNeste) {
                    is Verdi -> Verdi(periodeVerdiFraSammenlagt.verdi + periodeVerdiFraNeste.verdi)
                    else -> periodeVerdiFraSammenlagt
                }

                is Null -> when (periodeVerdiFraNeste) {
                    is Verdi -> Verdi(listOf(periodeVerdiFraNeste.verdi))
                    else -> Null()
                }

                is Udefinert -> when (periodeVerdiFraNeste) {
                    is Verdi -> Verdi(listOf(periodeVerdiFraNeste.verdi))
                    is Null -> Null()
                    is Udefinert -> Udefinert()
                }
            }
        }
    }
}

fun <I, R> Collection<Tidslinje<I>>.kombiner(
    listeKombinator: (Iterable<I>) -> R
): Tidslinje<R> = this.slåSammen().map {
    when (it) {
        is Verdi -> Verdi(listeKombinator(it.verdi)!!)
        is Null -> Null()
        is Udefinert -> Udefinert()
    }
}