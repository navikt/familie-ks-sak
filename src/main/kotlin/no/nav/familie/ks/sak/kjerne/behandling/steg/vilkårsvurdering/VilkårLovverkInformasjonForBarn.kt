package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.erSammeEllerEtter
import no.nav.familie.ks.sak.common.util.toLocalDate
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.lovverk.Lovverk
import no.nav.familie.ks.sak.kjerne.lovverk.LovverkUtleder
import java.time.LocalDate
import java.time.YearMonth

data class VilkårLovverkInformasjonForBarn(
    val fødselsdato: LocalDate,
    val adopsjonsdato: LocalDate?,
    val periodeFomForAdoptertBarn: YearMonth? = null,
    val periodeTomForAdoptertBarn: YearMonth? = null,
) {
    val periodeFomBarnetsAlderLov2021: LocalDate
    val periodeTomBarnetsAlderLov2021: LocalDate
    val periodeFomBarnetsAlderLov2024: LocalDate
    val periodeTomBarnetsAlderLov2024: LocalDate
    val periodeFomBarnetsAlderLov2025: LocalDate
    val periodeTomBarnetsAlderLov2025: LocalDate
    val vilkårLovverk: VilkårLovverk

    init {
        this.periodeFomBarnetsAlderLov2021 = fødselsdato.plusYears(1)
        this.periodeTomBarnetsAlderLov2021 = fødselsdato.plusYears(2)
        this.periodeFomBarnetsAlderLov2024 = fødselsdato.plusMonths(13)
        this.periodeTomBarnetsAlderLov2024 = fødselsdato.plusMonths(19)
        this.periodeFomBarnetsAlderLov2025 = fødselsdato.plusMonths(12)
        this.periodeTomBarnetsAlderLov2025 = fødselsdato.plusMonths(20)

        val lovverk = LovverkUtleder.utledLovverkForBarn(fødselsdato = fødselsdato, adopsjonsdato = adopsjonsdato)

        this.vilkårLovverk =
            when (lovverk) {
                Lovverk.LOVENDRING_FEBRUAR_2025 -> VilkårLovverk.LOVVERK_2025
                Lovverk.FØR_LOVENDRING_2025 -> utledVilkårLovverkFørLovendring2025()
            }
    }

    private fun utledVilkårLovverkFørLovendring2025(): VilkårLovverk {
        // Lovverk før 2025 trenger å se på de faktiske periodene man har oppfylt adopsjon i vilkårsvurderingen og kan ikke erstattes av adopsjondato
        val erTruffetAvLovverk2021 = periodeFomForAdoptertBarn?.isBefore(DATO_LOVENDRING_2024.toYearMonth()) ?: periodeFomBarnetsAlderLov2021.isBefore(DATO_LOVENDRING_2024)
        val erTruffetAvLovverk2024 = periodeTomForAdoptertBarn?.toLocalDate()?.erSammeEllerEtter(DATO_LOVENDRING_2024) ?: periodeTomBarnetsAlderLov2024.erSammeEllerEtter(DATO_LOVENDRING_2024)
        return when {
            erTruffetAvLovverk2021 && erTruffetAvLovverk2024 -> VilkårLovverk.LOVVERK_2021_OG_2024
            erTruffetAvLovverk2021 -> VilkårLovverk.LOVVERK_2021
            else -> VilkårLovverk.LOVVERK_2024
        }
    }
}
