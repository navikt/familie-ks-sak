package no.nav.familie.ks.sak.common.tidslinje.utvidelser

import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjema
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaEntitet
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.utenBarn
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.utenPeriode
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull

fun <T : EøsSkjema<T>> List<T>.tilTidslinje() =
    this
        .map {
            Periode(
                it.utenPeriode(),
                it.fom?.førsteDagIInneværendeMåned(),
                it.tom?.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()

fun <T : EøsSkjema<T>> T.tilTidslinje() = listOf(this).tilTidslinje()

fun <T : EøsSkjema<T>> List<T>.slåSammen(): List<T> {
    if (this.isEmpty()) return this

    val eøsTidslinjer: Tidslinje<Set<T>> =
        this.map { it.tilTidslinje() }.kombiner { tidslinje ->
            tidslinje
                .groupingBy { it.utenBarn() }
                .reduce { _, acc, skjema -> acc.leggSammenBarn(skjema) }
                .values
                .toSet()
        }

    val eøsTidslinjerSlåttSammenVertikalt =
        eøsTidslinjer.tilPerioderIkkeNull().flatMap { periode ->
            periode.verdi.settFomOgTom(periode) ?: emptyList()
        }
    val eøsTidslinjerSlåttSammenHorizontalt =
        eøsTidslinjerSlåttSammenVertikalt
            .groupBy { it.utenPeriode() }
            .mapValues { (_, kompetanser) -> kompetanser.tilTidslinje().slåSammenLikePerioder() }
            .mapValues { (_, tidslinje) -> tidslinje.tilPerioder() }
            .values
            .flatten()
            .mapNotNull { periode -> periode.verdi?.settFomOgTom(periode) }

    return eøsTidslinjerSlåttSammenHorizontalt
}

private fun <T : EøsSkjema<T>> T.leggSammenBarn(skjema: T) =
    this.kopier(
        fom = this.fom,
        tom = this.tom,
        barnAktører = this.barnAktører + skjema.barnAktører,
    )

fun <T : EøsSkjema<T>> Iterable<T>?.settFomOgTom(periode: Periode<*>) = this?.map { skjema -> skjema.settFomOgTom(periode) }

fun <T : EøsSkjema<T>> T.settFomOgTom(periode: Periode<*>) =
    this.kopier(
        fom = periode.fom?.toYearMonth(),
        tom = periode.tom?.toYearMonth(),
        barnAktører = this.barnAktører,
    )

fun <T : EøsSkjema<T>> Iterable<T>.tilSeparateTidslinjerForBarna(): Map<Aktør, Tidslinje<T>> {
    val skjemaer = this
    if (skjemaer.toList().isEmpty()) return emptyMap()

    // Trekker ut alle barn aktørId som brukes i skjemaer
    val alleBarnAktørIder = skjemaer.map { it.barnAktører }.reduce { akk, neste -> akk + neste }

    return alleBarnAktørIder.associateWith { aktør ->
        skjemaer
            .filter { it.barnAktører.contains(aktør) }
            .map {
                Periode(
                    fom = it.fom?.førsteDagIInneværendeMåned(),
                    tom = it.tom?.sisteDagIInneværendeMåned(),
                    verdi = it.kopier(fom = null, tom = null, barnAktører = setOf(aktør)),
                )
            }.tilTidslinje()
    }
}

fun <T : EøsSkjemaEntitet<T>> Map<Aktør, Tidslinje<T>>.tilSkjemaer() = this.flatMap { (aktør, tidslinjer) -> tidslinjer.tilSkjemaer(aktør) }.slåSammen()

private fun <T : EøsSkjema<T>> Tidslinje<T>.tilSkjemaer(aktør: Aktør) =
    this.tilPerioder().mapNotNull { periode ->
        periode.verdi?.kopier(
            fom = periode.fom?.toYearMonth(),
            tom = periode.tom?.toYearMonth(),
            barnAktører = setOf(aktør),
        )
    }
