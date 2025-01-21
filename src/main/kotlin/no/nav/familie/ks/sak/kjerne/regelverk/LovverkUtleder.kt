package no.nav.familie.ks.sak.kjerne.regelverk

import java.time.LocalDate

object LovverkUtleder {
    val FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025 = LocalDate.of(2024, 1, 1)

    // TODO: Legg inn støtte for adopsjon
    fun utledLovverkForBarn(
        fødselsdato: LocalDate,
        skalBestemmeLovverkBasertPåFødselsdato: Boolean,
    ): Lovverk {
        if (!skalBestemmeLovverkBasertPåFødselsdato) {
            return Lovverk.FØR_LOVENDRING_2025
        }
        return when {
            fødselsdato.isBefore(FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025) -> Lovverk.FØR_LOVENDRING_2025
            else -> Lovverk.LOVENDRING_FEBRUAR_2025
        }
    }
}
