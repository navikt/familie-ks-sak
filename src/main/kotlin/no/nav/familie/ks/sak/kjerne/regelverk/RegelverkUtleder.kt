package no.nav.familie.ks.sak.kjerne.regelverk

import java.time.LocalDate

object RegelverkUtleder {
    val FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025 = LocalDate.of(2024, 1, 1)

    fun utledRegelverkForBarn(fødselsdato: LocalDate): Regelverk =
        when {
            fødselsdato.isBefore(FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025) -> Regelverk.FØR_LOVENDRING_2025
            else -> Regelverk.LOVENDRING_FEBRUAR_2025
        }
}
