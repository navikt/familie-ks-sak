package no.nav.familie.ks.sak.kjerne.eøs.felles.domene

import no.nav.familie.ks.sak.common.util.MAX_MÅNED
import no.nav.familie.ks.sak.common.util.MIN_MÅNED
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import java.time.YearMonth

interface EøsSkjema<T : EøsSkjema<T>> {
    val fom: YearMonth?
    val tom: YearMonth?
    val barnAktører: Set<Aktør>

    fun kopier(
        fom: YearMonth? = this.fom,
        tom: YearMonth? = this.tom,
        barnAktører: Set<Aktør> = this.barnAktører.map { it.copy() }.toSet(),
    ): T

    fun utenInnhold(): T
}

fun <T : EøsSkjema<T>> T.utenInnholdTom(tom: YearMonth?) = this.kopier(fom = this.tom?.plusMonths(1), tom = tom).utenInnhold()

fun <T : EøsSkjema<T>> T.bareInnhold(): T = this.kopier(fom = null, tom = null, barnAktører = emptySet())

fun <T : EøsSkjema<T>> T.utenBarn(): T = this.kopier(fom = this.fom, tom = this.tom, barnAktører = emptySet())

fun <T : EøsSkjema<T>> T.utenPeriode(): T = this.kopier(fom = null, tom = null, barnAktører = this.barnAktører)

// Metoden kalles med eksisterende skjema og oppdatering skjema som kommer i argument
// Sjekker om oppdatering skjema tom dato er ikke null og
// enten eksisterende skjema tom dato var null
// eller oppdatering skjema tom dato var satt til tidligere dato enn eksisterende skjema tom dato
fun <T : EøsSkjema<T>> T.tomBlirForkortetEllerLukketAv(skjema: T): Boolean = skjema.tom != null && (this.tom == null || checkNotNull(this.tom) > skjema.tom)

fun <T : EøsSkjema<T>> T.erLikBortsettFraTom(skjema: T): Boolean = this.kopier(tom = skjema.tom) == skjema

fun <T : EøsSkjema<T>> T.erLikBortsettFraBarn(skjema: T): Boolean = this.kopier(barnAktører = skjema.barnAktører) == skjema

fun <T : EøsSkjema<T>> T.erLikBortsettFraBarnOgTom(skjema: T): Boolean = this.kopier(barnAktører = skjema.barnAktører, tom = skjema.tom) == skjema

fun <T : EøsSkjema<T>> T.manglerBarn(skjema: T): Boolean = this.barnAktører.size < skjema.barnAktører.size && skjema.barnAktører.containsAll(this.barnAktører)

fun <T : EøsSkjema<T>> T.medFjernetBarn(skjema: T): T = this.kopier(barnAktører = skjema.barnAktører.minus(this.barnAktører))

fun <T : EøsSkjema<T>> T.harBarnOgPeriode(): Boolean {
    val harPeriode = fom == null || tom == null || checkNotNull(fom) <= tom
    return harPeriode && barnAktører.isNotEmpty()
}

fun <T : EøsSkjema<T>> T.medOverlappendeBarnOgPeriode(skjema: T): T? {
    val fom = maxOf(this.fom ?: MIN_MÅNED, skjema.fom ?: MIN_MÅNED)
    val tom = minOf(this.tom ?: MAX_MÅNED, skjema.tom ?: MAX_MÅNED)

    val snitt =
        this.kopier(
            fom = if (fom == MIN_MÅNED) null else fom,
            tom = if (tom == MAX_MÅNED) null else tom,
            barnAktører = this.barnAktører.intersect(skjema.barnAktører),
        )

    return if (snitt.harBarnOgPeriode()) snitt else null
}

fun <T : EøsSkjema<T>> T.trekkFra(skjema: T): List<T> {
    val gammeltSkjema = this

    val skjemaForFjernetBarn =
        gammeltSkjema
            .kopier(
                barnAktører = gammeltSkjema.barnAktører.minus(skjema.barnAktører),
            ).takeIf { it.barnAktører.isNotEmpty() }

    val skjemaForTidligerePerioder =
        gammeltSkjema
            .kopier(
                fom = gammeltSkjema.fom,
                tom = skjema.fom?.minusMonths(1),
                barnAktører = skjema.barnAktører,
            ).takeIf { it.fom != null && checkNotNull(it.fom) <= it.tom }

    val skjemaForEtterfølgendePerioder =
        gammeltSkjema
            .kopier(
                fom = skjema.tom?.plusMonths(1),
                tom = gammeltSkjema.tom,
                barnAktører = skjema.barnAktører,
            ).takeIf { it.fom != null && it.fom!! <= (it.tom ?: MAX_MÅNED) }

    return listOfNotNull(skjemaForFjernetBarn, skjemaForTidligerePerioder, skjemaForEtterfølgendePerioder)
}
