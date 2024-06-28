package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import java.time.LocalDate
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.erSammeEllerEtter

data class VilkårLovverkInformasjonForBarn(
    val fødselsdato: LocalDate,
) {
    val periodeFomBarnetsAlderLov2021: LocalDate
    val periodeTomBarnetsAlderLov2021: LocalDate
    val periodeFomBarnetsAlderLov2024: LocalDate
    val periodeTomBarnetsAlderLov2024: LocalDate
    val lovverk: VilkårLovverk

    init {
        this.periodeFomBarnetsAlderLov2021 = fødselsdato.plusYears(1)
        this.periodeTomBarnetsAlderLov2021 = fødselsdato.plusYears(2)
        this.periodeFomBarnetsAlderLov2024 = fødselsdato.plusMonths(13)
        this.periodeTomBarnetsAlderLov2024 = fødselsdato.plusMonths(19)
        val erTruffetAvLovverk2021 = periodeFomBarnetsAlderLov2021.isBefore(DATO_LOVENDRING_2024)
        val erTruffetAvLovverk2024 = periodeTomBarnetsAlderLov2024.erSammeEllerEtter(DATO_LOVENDRING_2024)
        this.lovverk = when {
            erTruffetAvLovverk2021 && erTruffetAvLovverk2024 -> VilkårLovverk._2021_OG_2024
            erTruffetAvLovverk2021 -> VilkårLovverk._2021
            erTruffetAvLovverk2024 -> VilkårLovverk._2024
            else -> throw Feil("Forventer at barnet blir truffet av minst et lovverk")
        }
    }
}

