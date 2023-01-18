package no.nav.familie.ks.sak.common.tidslinje.utvidelser

import no.nav.familie.ks.sak.common.tidslinje.Null
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.Udefinert
import no.nav.familie.ks.sak.common.tidslinje.Verdi

fun <T> Tidslinje<T>.filtrer(predicate: (T?) -> Boolean) =
    this.map {
        when (it) {
            is Verdi, is Null -> if (predicate(it.verdi)) it else Udefinert()
            is Udefinert -> Udefinert()
        }
    }

fun <T> Tidslinje<T>.filtrerIkkeNull(): Tidslinje<T> = filtrer { it != null }
