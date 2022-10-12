package no.nav.familie.ks.sak.kjerne.tidslinje.util

import no.nav.familie.ks.sak.kjerne.tidslinje.Periode
import no.nav.familie.ks.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ks.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ks.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ks.sak.kjerne.tidslinje.tid.rangeTo

class StringTidslinje(
    val start: Tidspunkt<Måned>,
    val s: List<String>
) : Tidslinje<String, Måned>() {

    val fraOgMed = if (s.firstOrNull() == "<") start.somUendeligLengeSiden() else start

    val tilOgMed: Tidspunkt<Måned>
        get() {
            val slutt = start.flytt(s.size.toLong() - 1)
            return if (s.lastOrNull() == ">") slutt.somUendeligLengeTil() else slutt
        }

    override fun lagPerioder(): Collection<Periode<String, Måned>> {
        val tidspunkter = fraOgMed..tilOgMed
        return tidspunkter.mapIndexed { index, tidspunkt ->
            val c = when (index) {
                0 -> if (s[index] == "<") s[index + 1] else s[index]
                s.size - 1 -> if (s[index] == ">") s[index - 1] else s[index]
                else -> s[index]
            }
            Periode(tidspunkt.somFraOgMed(), tidspunkt.somTilOgMed(), c)
        }
    }
}
