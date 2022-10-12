package no.nav.familie.ks.sak.kjerne.tidslinje.tid

import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import java.time.LocalDate
import java.time.YearMonth

data class DagTidspunkt internal constructor(
    private val dato: LocalDate,
    private val uendelighet: Uendelighet
) : Tidspunkt<Dag>(uendelighet) {

    init {
        if (dato < PRAKTISK_TIDLIGSTE_DAG) {
            throw IllegalArgumentException("Kan ikke håndtere så tidlig tidspunkt. Bruk uendeligLengeSiden()")
        } else if (dato > PRAKTISK_SENESTE_DAG) {
            throw IllegalArgumentException("Kan ikke håndtere så sent tidspunkt. Bruk uendeligLengeTil()")
        }
    }

    override fun tilFørsteDagIMåneden(): DagTidspunkt = this.copy(dato = dato.withDayOfMonth(1))

    override fun tilSisteDagIMåneden(): DagTidspunkt = this.copy(dato = dato.sisteDagIMåned())

    override fun tilInneværendeMåned(): MånedTidspunkt = MånedTidspunkt(dagTilMånedKonverterer(this.dato), uendelighet)

    override fun tilLocalDateEllerNull(): LocalDate? =
        if (uendelighet != Uendelighet.INGEN) {
            null
        } else {
            dato
        }

    override fun tilLocalDate(): LocalDate =
        tilLocalDateEllerNull() ?: throw IllegalStateException("Tidspunkt er uendelig")

    override fun tilYearMonthEllerNull(): YearMonth? = tilLocalDateEllerNull()?.let { dagTilMånedKonverterer(it) }

    override fun tilYearMonth(): YearMonth = dagTilMånedKonverterer(tilLocalDate())

    override fun flytt(tidsenheter: Long): DagTidspunkt = this.copy(dato = dato.plusDays(tidsenheter))

    override fun somEndelig(): DagTidspunkt = copy(uendelighet = Uendelighet.INGEN)

    override fun somUendeligLengeSiden(): DagTidspunkt = copy(uendelighet = Uendelighet.FORTID)

    override fun somUendeligLengeTil(): DagTidspunkt = copy(uendelighet = Uendelighet.FREMTID)

    override fun somFraOgMed(): DagTidspunkt =
        if (uendelighet == Uendelighet.FREMTID) {
            somEndelig()
        } else {
            this
        }

    override fun somTilOgMed(): DagTidspunkt =
        if (uendelighet == Uendelighet.FORTID) {
            somEndelig()
        } else {
            this
        }

    override fun toString(): String =
        when (uendelighet) {
            Uendelighet.FORTID -> "<--"
            else -> ""
        } + dato + when (uendelighet) {
            Uendelighet.FREMTID -> "-->"
            else -> ""
        }

    override fun sammenlignMed(tidspunkt: Tidspunkt<Dag>): Int {
        return dato.compareTo(tidspunkt.tilLocalDate())
    }

    override fun equals(other: Any?): Boolean =
        when (other) {
            is DagTidspunkt -> compareTo(other) == 0
            else -> super.equals(other)
        }

    companion object {
        fun nå() = DagTidspunkt(LocalDate.now(), Uendelighet.INGEN)

        internal fun LocalDate?.tilTidspunktEllerTidligereEnn(tidspunkt: LocalDate?) =
            tilTidspunktEllerUendelig(tidspunkt ?: LocalDate.now(), Uendelighet.FORTID)

        internal fun LocalDate?.tilTidspunktEllerSenereEnn(tidspunkt: LocalDate?) =
            tilTidspunktEllerUendelig(tidspunkt ?: LocalDate.now(), Uendelighet.FREMTID)

        internal fun LocalDate?.tilTidspunktEllerUendeligLengeSiden() =
            this.tilTidspunktEllerUendelig(PRAKTISK_TIDLIGSTE_DAG.plusYears(1), Uendelighet.FORTID)

        internal fun LocalDate?.tilTidspunktEllerUendeligLengeTil() =
            this.tilTidspunktEllerUendelig(PRAKTISK_SENESTE_DAG.minusYears(1), Uendelighet.FREMTID)

        private fun LocalDate?.tilTidspunktEllerUendelig(default: LocalDate?, uendelighet: Uendelighet) =
            this?.let { DagTidspunkt(it, Uendelighet.INGEN) } ?: DagTidspunkt(
                default ?: LocalDate.now(),
                uendelighet
            )

        fun dagForUendeligLengeSiden(dato: LocalDate = LocalDate.now()) =
            DagTidspunkt(dato, uendelighet = Uendelighet.FORTID)

        fun dagMedUendeligLengeTil(dato: LocalDate = LocalDate.now()) =
            DagTidspunkt(dato, uendelighet = Uendelighet.FREMTID)
    }
}
