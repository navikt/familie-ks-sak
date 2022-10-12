package no.nav.familie.ks.sak.kjerne.tidslinje.util

import no.nav.familie.ks.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ks.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ks.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ks.sak.kjerne.tidslinje.tilOgMed

fun <T : Tidsenhet> Iterable<Tidslinje<*, T>>.print() = this.forEach { it.print() }
fun <T : Tidsenhet> Tidslinje<*, T>.print() {
    println("${this.fraOgMed()..this.tilOgMed()} ${this.javaClass.name}")
    this.perioder().forEach { println(it) }
}
