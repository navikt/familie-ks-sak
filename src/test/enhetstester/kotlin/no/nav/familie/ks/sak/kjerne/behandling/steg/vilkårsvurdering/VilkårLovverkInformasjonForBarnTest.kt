package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårLovverkInformasjonForBarnTest {
    @Test
    fun `skal ha korrekt informasjon om barn som kun er påvirket av lovverk for 2021`() {
        // Arrange
        val fødselsdato: LocalDate = LocalDate.of(2022, 12, 31)

        // Act
        val vilkårLovverkInformasjonForBarn =
            VilkårLovverkInformasjonForBarn(
                fødselsdato = fødselsdato,
                adopsjonsdato = null,
            )

        // Assert
        assertThat(vilkårLovverkInformasjonForBarn.vilkårLovverk).isEqualTo(VilkårLovverk.LOVVERK_2021)

        assertThat(vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2021).isEqualTo(fødselsdato.plusYears(1))
        assertThat(vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2021).isEqualTo(fødselsdato.plusYears(2))

        assertThat(vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2024).isEqualTo(fødselsdato.plusMonths(13))
        assertThat(vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2024).isEqualTo(fødselsdato.plusMonths(19))
    }

    @Test
    fun `skal ha korrekt informasjon om barn som kun er påvirket av lovverk for 2024`() {
        // Arrange
        val fødselsdato: LocalDate = LocalDate.of(2023, 8, 1)

        // Act
        val vilkårLovverkInformasjonForBarn =
            VilkårLovverkInformasjonForBarn(
                fødselsdato = fødselsdato,
                adopsjonsdato = null,
            )

        // Assert
        assertThat(vilkårLovverkInformasjonForBarn.vilkårLovverk).isEqualTo(VilkårLovverk.LOVVERK_2024)

        assertThat(vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2021).isEqualTo(fødselsdato.plusYears(1))
        assertThat(vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2021).isEqualTo(fødselsdato.plusYears(2))

        assertThat(vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2024).isEqualTo(fødselsdato.plusMonths(13))
        assertThat(vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2024).isEqualTo(fødselsdato.plusMonths(19))
    }

    @Test
    fun `skal ha korrekt informasjon om barn som er påvirket av lovverk for både 2021 og 2024`() {
        // Arrange
        val fødselsdato: LocalDate = LocalDate.of(2023, 7, 31)

        // Act
        val vilkårLovverkInformasjonForBarn =
            VilkårLovverkInformasjonForBarn(
                fødselsdato = fødselsdato,
                adopsjonsdato = null,
            )

        // Assert
        assertThat(vilkårLovverkInformasjonForBarn.vilkårLovverk).isEqualTo(VilkårLovverk.LOVVERK_2021_OG_2024)

        assertThat(vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2021).isEqualTo(fødselsdato.plusYears(1))
        assertThat(vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2021).isEqualTo(fødselsdato.plusYears(2))

        assertThat(vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2024).isEqualTo(fødselsdato.plusMonths(13))
        assertThat(vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2024).isEqualTo(fødselsdato.plusMonths(19))
    }
}
