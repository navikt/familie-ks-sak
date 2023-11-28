package no.nav.familie.ks.sak.kjerne.util

import no.nav.familie.ks.sak.common.tidslinje.filtrerIkkeNull
import no.nav.familie.ks.sak.common.tidslinje.util.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaEntitet
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import java.time.YearMonth

abstract class SkjemaBuilder<S, B>(
    private val startMåned: YearMonth,
    private val behandlingId: Long,
) where S : EøsSkjemaEntitet<S>, B : SkjemaBuilder<S, B> {
    private val skjemaer: MutableList<S> = mutableListOf()

    protected fun medSkjema(
        charTidslinje: String,
        barn: List<Person>,
        mapChar: (Char?) -> S?,
    ): B {
        val tidslinje =
            charTidslinje.tilTidslinje(startMåned, mapChar)

        tidslinje.tilPerioder()
            .filtrerIkkeNull()
            .map {
                it.verdi.kopier(
                    fom = it.fom?.tilYearMonth(),
                    tom = it.tom?.tilYearMonth(),
                    barnAktører = barn.map { person -> person.aktør }.toSet(),
                )
            }
            .all { skjemaer.add(it) }

        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    protected fun medTransformasjon(transformasjon: (S) -> S): B {
        val transformerteSkjemaer = skjemaer.map { skjema -> transformasjon(skjema) }
        skjemaer.clear()
        skjemaer.addAll(transformerteSkjemaer)

        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    fun bygg(): Collection<S> =
        skjemaer
            .map { skjema -> skjema.also { it.behandlingId = behandlingId } }

    fun lagreTil(repository: EøsSkjemaRepository<S>): List<S> {
        return repository.saveAll(bygg())
    }
}
