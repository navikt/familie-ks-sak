package no.nav.familie.ks.sak.common.tidslinje

const val inf = 1_000_000_000

sealed class PeriodeVerdi<T>(protected val _verdi: T?) {

    override operator fun equals(other: Any?): Boolean {
        if (other !is PeriodeVerdi<*>) return false
        if (other._verdi == this._verdi) return true
        return false
    }

    override fun hashCode(): Int {
        return this._verdi.hashCode()
    }

    abstract val verdi: T?
}

class Verdi<T>(override val verdi: T & Any) : PeriodeVerdi<T>(verdi)

class Udefinert<T> : PeriodeVerdi<T>(null) {

    override fun equals(other: Any?): Boolean {
        return other is Udefinert<*>
    }

    override fun hashCode(): Int {
        return this._verdi.hashCode()
    }

    override val verdi: T? = this._verdi
}

class Null<T> : PeriodeVerdi<T>(null) {

    override fun equals(other: Any?): Boolean {
        return other is Null<*>
    }

    override fun hashCode(): Int {
        return this._verdi.hashCode()
    }

    override val verdi: T? = this._verdi
}

/**
 * En periode representerer et tidsintervall hvor en tidslinje har en konstant verdi.
 * En periode varer en tid [lengde], og kan være uendelig.
 * Om [lengde] > [inf] eller [erUendelig] er satt til true, behandles perioden som at den har uendelig lengde.
 * En tidslinje støtter verdier av typen [Udefinert], [Null] og [PeriodeVerdi]. En verdi er udefinert når vi ikke vet
 * hva verdien skal være (et hull i tidslinja). En verdi er no.nav.familie.ks.sak.common.tidslinje.Null når vi vet at det ikke finnes en verdi i dette tidsrommet.
 */
data class TidslinjePeriode<T>(val periodeVerdi: PeriodeVerdi<T>, var lengde: Int, var erUendelig: Boolean = false) {

    init {
        if (lengde >= inf) {
            erUendelig = true
        }
        if (erUendelig && lengde < inf) {
            lengde = inf
        }
        if (lengde <= 0) {
            throw java.lang.IllegalArgumentException("lengde må være større enn null.")
        }
    }

    constructor(periodeVerdi: T?, lengde: Int, erUendelig: Boolean = false) : this(
        if (periodeVerdi == null) Null() else Verdi(
            periodeVerdi
        ),
        lengde,
        erUendelig
    )

    override fun toString(): String {
        return "Verdi: " + periodeVerdi.verdi.toString() + ", Lengde: " + lengde
    }
}

fun <T> T?.tilPeriodeVerdi(): PeriodeVerdi<T> {
    return this?.let { Verdi(it) } ?: Null()
}
