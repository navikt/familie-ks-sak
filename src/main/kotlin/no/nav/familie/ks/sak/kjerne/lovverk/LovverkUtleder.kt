package no.nav.familie.ks.sak.kjerne.lovverk

import java.time.LocalDate

object LovverkUtleder {
    val FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025 = LocalDate.of(2024, 1, 1)

    fun utledLovverkForBarn(
        fødselsdato: LocalDate,
        adopsjonsdato: LocalDate?,
    ): Lovverk {
        val fødselsdatoEllerAdopsjonsdato = adopsjonsdato ?: fødselsdato

        return when {
            fødselsdatoEllerAdopsjonsdato.isBefore(FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025) -> Lovverk.FØR_LOVENDRING_2025
            else -> Lovverk.LOVENDRING_FEBRUAR_2025
        }
    }
}
