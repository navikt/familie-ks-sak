package no.nav.familie.ks.sak.api.dto

import jakarta.validation.constraints.AssertTrue
import no.nav.familie.kontrakter.felles.Fødselsnummer
import java.math.BigDecimal
import java.time.YearMonth

data class OvergangsordningAndelDto(
    val id: Long,
    val personIdent: String?,
    val antallTimer: BigDecimal,
    val deltBosted: Boolean,
    val fom: YearMonth?,
    val tom: YearMonth?,
) {
    // Valideringsfunksjoner må start med `is`
    @AssertTrue(message = "Til og med-dato kan ikke være før fra og med-dato")
    fun isTomSameOrAfterFom(): Boolean = fom == null || tom == null || !tom.isBefore(fom)

    @AssertTrue(message = "Personident må være et gyldig fødselsnummer")
    fun isPersonidentValid(): Boolean =
        personIdent == null ||
            runCatching { Fødselsnummer(personIdent!!) }.fold(
                onSuccess = { true },
                onFailure = { false },
            )

    @AssertTrue(message = "Hvis antall timer er større enn 0, kan ikke delt bosted være avhuket")
    fun isAntallTimerAndDeltBostedValid(): Boolean = !deltBosted || antallTimer == BigDecimal.ZERO
}
