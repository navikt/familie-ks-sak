package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import no.nav.familie.ks.sak.common.tidslinje.IkkeNullbarPeriode
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverkInformasjonForBarn
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BarnetsAlderVilkårValidatorTest {
    val barnetsAlderVilkårValidator2021: BarnetsAlderVilkårValidator2021 = mockk()
    val barnetsAlderVilkårValidator2024: BarnetsAlderVilkårValidator2024 = mockk()
    val barnetsAlderVilkårValidator2021og2024: BarnetsAlderVilkårValidator2021og2024 = mockk()
    val barnetsAlderVilkårValidator =
        BarnetsAlderVilkårValidator(
            barnetsAlderVilkårValidator2021,
            barnetsAlderVilkårValidator2024,
            barnetsAlderVilkårValidator2021og2024,
        )

    @Test
    fun `skal returnere ingen feil når validering for 2021 og 2024 returnerer ingen feil`() {
        // Arrange
        val fødselsdato = DATO_LOVENDRING_2024.minusMonths(19)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )

        val vilkårResultatPeriode =
            IkkeNullbarPeriode(
                lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER),
                fødselsdato,
                DATO_LOVENDRING_2024.minusDays(1),
            )

        val vilkårResultatPerioder = listOf(vilkårResultatPeriode)

        every {
            barnetsAlderVilkårValidator2021og2024.validerBarnetsAlderVilkår(
                perioder = vilkårResultatPerioder,
                barn = person,
                any<VilkårLovverkInformasjonForBarn>(),
            )
        }.returns(listOf())

        // Act
        val feil =
            barnetsAlderVilkårValidator.validerVilkårBarnetsAlder(
                vilkårResultatPerioder,
                person,
            )

        // Assert
        assertThat(feil).isEmpty()
        verify(exactly = 0) { barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 0) { barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 1) { barnetsAlderVilkårValidator2021og2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<VilkårLovverkInformasjonForBarn>()) }
    }

    @Test
    fun `skal returnere feil når validering for 2021 og 2024 returnerer feil`() {
        // Arrange
        val fødselsdato = DATO_LOVENDRING_2024.minusMonths(19)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )

        val vilkårResultatPeriode =
            IkkeNullbarPeriode(
                lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER),
                fødselsdato,
                DATO_LOVENDRING_2024.minusDays(1),
            )

        val vilkårResultatPerioder = listOf(vilkårResultatPeriode)

        every {
            barnetsAlderVilkårValidator2021og2024.validerBarnetsAlderVilkår(
                perioder = vilkårResultatPerioder,
                barn = person,
                any<VilkårLovverkInformasjonForBarn>(),
            )
        }.returns(listOf("feilmelding"))

        // Act
        val feil =
            barnetsAlderVilkårValidator.validerVilkårBarnetsAlder(
                vilkårResultatPerioder,
                person,
            )

        // Assert
        assertThat(feil).hasSize(1)
        assertThat(feil).contains("feilmelding")
        verify(exactly = 0) { barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 0) { barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 1) { barnetsAlderVilkårValidator2021og2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<VilkårLovverkInformasjonForBarn>()) }
    }

    @Test
    fun `skal returnere ingen feil når validering for 2021 returnerer ingen feil`() {
        // Arrange
        val fødselsdato = DATO_LOVENDRING_2024.minusMonths(19).minusDays(1)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )

        val vilkårResultatPeriode =
            IkkeNullbarPeriode(
                lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER),
                fødselsdato,
                DATO_LOVENDRING_2024.minusDays(1),
            )

        val vilkårResultatPerioder = listOf(vilkårResultatPeriode)

        every {
            barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(
                perioder = vilkårResultatPerioder,
                barn = person,
                periodeFomBarnetsAlderLov2021 = any<LocalDate>(),
                periodeTomBarnetsAlderLov2021 = any<LocalDate>(),
            )
        }.returns(listOf())

        // Act
        val feil =
            barnetsAlderVilkårValidator.validerVilkårBarnetsAlder(
                vilkårResultatPerioder,
                person,
            )

        // Assert
        assertThat(feil).isEmpty()
        verify(exactly = 1) { barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 0) { barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 0) { barnetsAlderVilkårValidator2021og2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<VilkårLovverkInformasjonForBarn>()) }
    }

    @Test
    fun `skal returnere feil når validering for 2021 returnerer feil`() {
        // Arrange
        val fødselsdato = DATO_LOVENDRING_2024.minusMonths(19).minusDays(1)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )

        val vilkårResultatPeriode =
            IkkeNullbarPeriode(
                lagVilkårResultat(),
                fødselsdato,
                DATO_LOVENDRING_2024.minusDays(1),
            )

        val vilkårResultatPerioder = listOf(vilkårResultatPeriode)

        every {
            barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(
                vilkårResultatPerioder,
                person,
                any<LocalDate>(),
                any<LocalDate>(),
            )
        }.returns(listOf("Feilmelding"))

        // Act
        val feil =
            barnetsAlderVilkårValidator.validerVilkårBarnetsAlder(
                vilkårResultatPerioder,
                person,
            )

        // Assert
        assertThat(feil).hasSize(1)
        assertThat(feil).contains("Feilmelding")
        verify(exactly = 1) { barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 0) { barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 0) { barnetsAlderVilkårValidator2021og2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<VilkårLovverkInformasjonForBarn>()) }
    }

    @Test
    fun `skal returnere ingen feil når validering for 2024 returnerer ingen feil`() {
        // Arrange
        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = DATO_LOVENDRING_2024,
            )

        val vilkårResultatPeriode =
            IkkeNullbarPeriode(
                lagVilkårResultat(),
                DATO_LOVENDRING_2024,
                DATO_LOVENDRING_2024.plusYears(1),
            )

        val vilkårResultatPerioder = listOf(vilkårResultatPeriode)

        every {
            barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(
                vilkårResultatPerioder,
                person,
                any<LocalDate>(),
                any<LocalDate>(),
            )
        }.returns(listOf())

        // Act
        val feil =
            barnetsAlderVilkårValidator.validerVilkårBarnetsAlder(
                vilkårResultatPerioder,
                person,
            )

        // Assert
        assertThat(feil).isEmpty()
        verify(exactly = 0) { barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 1) { barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 0) { barnetsAlderVilkårValidator2021og2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<VilkårLovverkInformasjonForBarn>()) }
    }

    @Test
    fun `skal returnere feil når validering for 2024 returnerer feil`() {
        // Arrange
        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = DATO_LOVENDRING_2024,
            )

        val vilkårResultatPeriode =
            IkkeNullbarPeriode(
                lagVilkårResultat(),
                DATO_LOVENDRING_2024,
                DATO_LOVENDRING_2024.plusYears(1),
            )

        val vilkårResultatPerioder = listOf(vilkårResultatPeriode)

        every {
            barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(
                vilkårResultatPerioder,
                person,
                any<LocalDate>(),
                any<LocalDate>(),
            )
        }.returns(listOf("Feilmelding"))

        // Act
        val feil =
            barnetsAlderVilkårValidator.validerVilkårBarnetsAlder(
                vilkårResultatPerioder,
                person,
            )

        // Assert
        assertThat(feil).hasSize(1)
        assertThat(feil).contains("Feilmelding")
        verify(exactly = 0) { barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 1) { barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 0) { barnetsAlderVilkårValidator2021og2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<VilkårLovverkInformasjonForBarn>()) }
    }

}
