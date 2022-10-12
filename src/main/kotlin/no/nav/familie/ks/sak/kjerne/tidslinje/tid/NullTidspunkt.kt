package no.nav.familie.ks.sak.kjerne.tidslinje.tid

import java.time.LocalDate
import java.time.YearMonth

data class NullTidspunkt<T : Tidsenhet> internal constructor(private val uendelighet: Uendelighet) :
    Tidspunkt<T>(uendelighet) {

    companion object {
        // Vi plasserer fra-og-med uendelig langt inn i fremtiden og til-og-med uendelig langt bak i fortiden
        // Dermed er tidslinjen "maksimalt tom"
        fun <T : Tidsenhet> fraOgMed() = NullTidspunkt<T>(uendelighet = Uendelighet.FREMTID)
        fun <T : Tidsenhet> tilOgMed() = NullTidspunkt<T>(uendelighet = Uendelighet.FORTID)
    }

    val nullTidspunktException = IllegalStateException("Dette er et NULL-tidspunkt")

    override fun tilFørsteDagIMåneden(): Tidspunkt<Dag> = NullTidspunkt(uendelighet)

    override fun tilSisteDagIMåneden(): Tidspunkt<Dag> = NullTidspunkt(uendelighet)

    override fun tilInneværendeMåned(): Tidspunkt<Måned> = NullTidspunkt(uendelighet)

    override fun tilLocalDateEllerNull(): LocalDate? = null

    override fun tilLocalDate(): LocalDate = throw nullTidspunktException

    override fun tilYearMonthEllerNull(): YearMonth? = null

    override fun tilYearMonth(): YearMonth = throw nullTidspunktException

    override fun flytt(tidsenheter: Long): Tidspunkt<T> = this // Er uendelig, så enhver flytting forblir uendelig

    override fun somEndelig(): Tidspunkt<T> = this // Forblir uendelig

    override fun somUendeligLengeSiden(): Tidspunkt<T> = fraOgMed()

    override fun somUendeligLengeTil(): Tidspunkt<T> = tilOgMed()

    override fun somFraOgMed(): Tidspunkt<T> = fraOgMed()

    override fun somTilOgMed(): Tidspunkt<T> = tilOgMed()

    override fun sammenlignMed(tidspunkt: Tidspunkt<T>): Int {
        return if (tidspunkt is NullTidspunkt) {
            val nullTidspunkt: NullTidspunkt<T> = tidspunkt // Burde kunne smart cast'es, men det funker ikke
            if (this.uendelighet == nullTidspunkt.uendelighet) {
                0
            } else if (this.uendelighet == Uendelighet.FORTID) {
                -1
            } else {
                1
            }
        } else {
            when (uendelighet) {
                Uendelighet.FREMTID -> 1 // Dette skal alltid være det seneste tidspunktet i alle sammenlikninger
                Uendelighet.FORTID -> -1 // Dette skal alltid være det tidligste tidspunktet i alle sammenlikninger
                else -> throw nullTidspunktException // Skal ikke inntreffe
            }
        }
    }
}
