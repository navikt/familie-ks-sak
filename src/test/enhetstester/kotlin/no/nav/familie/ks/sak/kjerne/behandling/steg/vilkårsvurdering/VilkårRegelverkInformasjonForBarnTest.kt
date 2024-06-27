package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VilkårRegelverkInformasjonForBarnTest {
    @Test
    fun `skal ha korrekt informasjon om barn som kun er påvirket av regelverk for 2021`() {
        // Arrange
        val fødselsdato: LocalDate = LocalDate.of(2022, 12, 31)

        // Act
        val vilkårRegelverkInformasjonForBarn = VilkårRegelverkInformasjonForBarn(fødselsdato)

        // Assert
        assertThat(vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2021).isTrue()
        assertThat(vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2024).isFalse()

        assertThat(vilkårRegelverkInformasjonForBarn.periodeFomBarnetsAlderLov2021).isEqualTo(fødselsdato.plusYears(1))
        assertThat(vilkårRegelverkInformasjonForBarn.periodeTomBarnetsAlderLov2021).isEqualTo(fødselsdato.plusYears(2))

        assertThat(vilkårRegelverkInformasjonForBarn.periodeFomBarnetsAlderLov2024).isEqualTo(fødselsdato.plusMonths(13))
        assertThat(vilkårRegelverkInformasjonForBarn.periodeTomBarnetsAlderLov2024).isEqualTo(fødselsdato.plusMonths(19))
    }

    @Test
    fun `skal ha korrekt informasjon om barn som kun er påvirket av regelverk for 2024`() {
        // Arrange
        val fødselsdato: LocalDate = LocalDate.of(2023, 8, 1)

        // Act
        val vilkårRegelverkInformasjonForBarn = VilkårRegelverkInformasjonForBarn(fødselsdato)

        // Assert
        assertThat(vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2021).isFalse()
        assertThat(vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2024).isTrue()

        assertThat(vilkårRegelverkInformasjonForBarn.periodeFomBarnetsAlderLov2021).isEqualTo(fødselsdato.plusYears(1))
        assertThat(vilkårRegelverkInformasjonForBarn.periodeTomBarnetsAlderLov2021).isEqualTo(fødselsdato.plusYears(2))

        assertThat(vilkårRegelverkInformasjonForBarn.periodeFomBarnetsAlderLov2024).isEqualTo(fødselsdato.plusMonths(13))
        assertThat(vilkårRegelverkInformasjonForBarn.periodeTomBarnetsAlderLov2024).isEqualTo(fødselsdato.plusMonths(19))
    }

    @Test
    fun `skal ha korrekt informasjon om barn som er påvirket av regelverk for både 2021 og 2024`() {
        // Arrange
        val fødselsdato: LocalDate = LocalDate.of(2023, 7, 31)

        // Act
        val vilkårRegelverkInformasjonForBarn = VilkårRegelverkInformasjonForBarn(fødselsdato)

        // Assert
        assertThat(vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2021).isTrue()
        assertThat(vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2024).isTrue()

        assertThat(vilkårRegelverkInformasjonForBarn.periodeFomBarnetsAlderLov2021).isEqualTo(fødselsdato.plusYears(1))
        assertThat(vilkårRegelverkInformasjonForBarn.periodeTomBarnetsAlderLov2021).isEqualTo(fødselsdato.plusYears(2))

        assertThat(vilkårRegelverkInformasjonForBarn.periodeFomBarnetsAlderLov2024).isEqualTo(fødselsdato.plusMonths(13))
        assertThat(vilkårRegelverkInformasjonForBarn.periodeTomBarnetsAlderLov2024).isEqualTo(fødselsdato.plusMonths(19))
    }
}
