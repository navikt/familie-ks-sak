package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import java.time.LocalDate
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.erSammeEllerEtter

data class VilkårRegelverkInformasjonForBarn(
    val fødselsdatoBarn: LocalDate,
) {
    // TODO : Endre fra Lov2021 til Regelverk2021 for periode variablene?
    val periodeFomBarnetsAlderLov2021 = fødselsdatoBarn.plusYears(1)
    val periodeTomBarnetsAlderLov2021 = fødselsdatoBarn.plusYears(2)
    val periodeFomBarnetsAlderLov2024 = fødselsdatoBarn.plusMonths(13)
    val periodeTomBarnetsAlderLov2024 = fødselsdatoBarn.plusMonths(19)
    val erTruffetAvRegelverk2021 = periodeFomBarnetsAlderLov2021.isBefore(DATO_LOVENDRING_2024)
    val erTruffetAvRegelverk2024 = periodeTomBarnetsAlderLov2024.erSammeEllerEtter(DATO_LOVENDRING_2024)
}
