package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.erSammeEllerEtter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VilkårLovverkInformasjonForBarnTest {
    @Test
    fun `skal ha korrekt informasjon om barn som kun er påvirket av lovverk for 2021`() {
        // Arrange
        val fødselsdato: LocalDate = LocalDate.of(2022, 12, 31)

        // Act
        val vilkårLovverkInformasjonForBarn = VilkårLovverkInformasjonForBarn(fødselsdato)

        // Assert
        assertThat(vilkårLovverkInformasjonForBarn.lovverk).isEqualTo(VilkårLovverk._2021)

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
        val vilkårLovverkInformasjonForBarn = VilkårLovverkInformasjonForBarn(fødselsdato)

        // Assert
        assertThat(vilkårLovverkInformasjonForBarn.lovverk).isEqualTo(VilkårLovverk._2024)

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
        val vilkårLovverkInformasjonForBarn = VilkårLovverkInformasjonForBarn(fødselsdato)

        // Assert
        assertThat(vilkårLovverkInformasjonForBarn.lovverk).isEqualTo(VilkårLovverk._2021_OG_2024)

        assertThat(vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2021).isEqualTo(fødselsdato.plusYears(1))
        assertThat(vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2021).isEqualTo(fødselsdato.plusYears(2))

        assertThat(vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2024).isEqualTo(fødselsdato.plusMonths(13))
        assertThat(vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2024).isEqualTo(fødselsdato.plusMonths(19))
    }

    @Test
    fun `skal kaste feil om barnet ikke er truffet av noen lovverk`() {

        // Arrange
        val mockDato: LocalDate = mockk()

        every { mockDato.plusYears(any()) } returns mockDato
        every { mockDato.plusMonths(any()) } returns mockDato
        every { mockDato.isBefore(any()) } returns false
        every { mockDato.isAfter(any()) } returns false
        every { mockDato.erSammeEllerEtter(any()) } returns false

        // Act & assert
        val exception = assertThrows<Feil> {
            VilkårLovverkInformasjonForBarn(mockDato)
        }
        assertThat(exception.message).isEqualTo(
            "Forventer at barnet blir truffet av minst et lovverk"
        )

    }
}
