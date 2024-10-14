package no.nav.familie.ks.sak.kjerne.regelverk

import java.time.LocalDate

object RegelverkUtleder {
    // TODO: Korriger dato når endelig lovendring er vedtatt
    val FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025 = LocalDate.of(2024, 1, 1)

    // TODO: Legg inn støtte for adopsjon
    fun utledRegelverkForBarn(
        fødselsdato: LocalDate,
        skalBestemmeRegelverkBasertPåFødselsdato: Boolean,
    ): Regelverk {
        if (!skalBestemmeRegelverkBasertPåFødselsdato) {
            return Regelverk.FØR_LOVENDRING_2025
        }
        return when {
            fødselsdato.isBefore(FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025) -> Regelverk.FØR_LOVENDRING_2025
            else -> Regelverk.LOVENDRING_FEBRUAR_2025
        }
    }
}
