package no.nav.familie.ks.sak.common.tidslinje.utvidelser

import no.nav.familie.ks.sak.common.tidslinje.PeriodeVerdi
import no.nav.familie.ks.sak.common.tidslinje.TidslinjePeriode

/**
 * Beregner en ny periode ved bruk av [operator] basert på periodene this og [operand].
 * Den nye perioden har lengde [lengde] og verdien blir beregnet ut ifra å benytte seg av
 * funksjonen [operator].
 * [operator] vil få verdiene som de to input periodene består av som input.
 */
fun <T, R, RESULTAT> TidslinjePeriode<T>.biFunksjon(
    operand: TidslinjePeriode<R>,
    lengde: Int,
    erUendelig: Boolean,
    operator: (elem1: PeriodeVerdi<T>, elem2: PeriodeVerdi<R>) -> PeriodeVerdi<RESULTAT>,
): TidslinjePeriode<RESULTAT> {
    return TidslinjePeriode(operator(this.periodeVerdi, operand.periodeVerdi), lengde, erUendelig)
}

fun <T> List<TidslinjePeriode<T>>.slåSammenLike(): List<TidslinjePeriode<T>> {
    return this.fold(emptyList()) { acc, tidslinjePeriode ->
        val sisteElementIAcc = acc.lastOrNull()

        if (sisteElementIAcc == null) {
            listOf(tidslinjePeriode)
        } else if (sisteElementIAcc.periodeVerdi == tidslinjePeriode.periodeVerdi) {
            acc.dropLast(1) + TidslinjePeriode(
                periodeVerdi = sisteElementIAcc.periodeVerdi,
                lengde = sisteElementIAcc.lengde + tidslinjePeriode.lengde,
                erUendelig = sisteElementIAcc.erUendelig || tidslinjePeriode.erUendelig,
            )
        } else {
            acc + tidslinjePeriode
        }
    }
}
