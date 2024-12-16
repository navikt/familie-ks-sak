package no.nav.familie.tidslinje.util

import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import java.time.YearMonth

fun <T> String.tilTidslinje(
    startTidspunkt: YearMonth,
    mapper: (Char) -> T,
): Tidslinje<T> {
    val charListe = this.toCharArray().toList()
    val førsteTegnBeskriverUendelighet = charListe.first() == '<'
    val sisteTegnBeskriverUendelighet = charListe.last() == '>'

    val charListeUtenUendelighet =
        when {
            førsteTegnBeskriverUendelighet && sisteTegnBeskriverUendelighet -> charListe.drop(1).dropLast(1)
            førsteTegnBeskriverUendelighet -> charListe.drop(1)
            sisteTegnBeskriverUendelighet -> charListe.dropLast(1)
            else -> charListe
        }

    val sisteIndex = charListeUtenUendelighet.size - 1
    return charListeUtenUendelighet
        .mapIndexed { index, char ->
            val erUendeligStartOgFørstePeriode = index == 0 && førsteTegnBeskriverUendelighet
            val erUendeligSluttOgSistePeriode = index == sisteIndex && sisteTegnBeskriverUendelighet

            val fom = if (erUendeligStartOgFørstePeriode) null else startTidspunkt.plusMonths(index.toLong()).førsteDagIInneværendeMåned()
            val tom = if (erUendeligSluttOgSistePeriode) null else startTidspunkt.plusMonths(index.toLong()).sisteDagIInneværendeMåned()
            val verdi = mapper(char)

            Periode(verdi, fom, tom)
        }.tilTidslinje()
        .slåSammenLikePerioder()
}
