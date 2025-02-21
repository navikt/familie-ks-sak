package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverkInformasjonForBarn
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.lovverk.LovverkUtleder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.tidslinje.IkkeNullbarPeriode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BarnetsAlderVilkårValidatorTest {
    val barnetsAlderVilkårValidator2021: BarnetsAlderVilkårValidator2021 = mockk()
    val barnetsAlderVilkårValidator2024: BarnetsAlderVilkårValidator2024 = mockk()
    val barnetsAlderVilkårValidator2021og2024: BarnetsAlderVilkårValidator2021og2024 = mockk()
    val barnetsAlderVilkårValidator2025: BarnetsAlderVilkårValidator2025 = mockk()
    val barnetsAlderVilkårValidator =
        BarnetsAlderVilkårValidator(
            barnetsAlderVilkårValidator2021,
            barnetsAlderVilkårValidator2024,
            barnetsAlderVilkårValidator2021og2024,
            barnetsAlderVilkårValidator2025,
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
                perioder = vilkårResultatPerioder,
                barn = person,
                adopsjonsdato = null,
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
                perioder = vilkårResultatPerioder,
                barn = person,
                adopsjonsdato = null,
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
                perioder = vilkårResultatPerioder,
                barn = person,
                adopsjonsdato = null,
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
                perioder = vilkårResultatPerioder,
                barn = person,
                adopsjonsdato = null,
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
                fødselsdato = LocalDate.of(2023, 8, 1),
            )

        val vilkårResultatPeriode =
            IkkeNullbarPeriode(
                lagVilkårResultat(),
                person.fødselsdato.plusMonths(13),
                person.fødselsdato.plusMonths(19),
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
                perioder = vilkårResultatPerioder,
                barn = person,
                adopsjonsdato = null,
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
                fødselsdato = LocalDate.of(2023, 8, 1),
            )

        val vilkårResultatPeriode =
            IkkeNullbarPeriode(
                lagVilkårResultat(),
                person.fødselsdato.plusMonths(13),
                person.fødselsdato.plusMonths(19),
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
                perioder = vilkårResultatPerioder,
                barn = person,
                adopsjonsdato = null,
            )

        // Assert
        assertThat(feil).hasSize(1)
        assertThat(feil).contains("Feilmelding")
        verify(exactly = 0) { barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 1) { barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 0) { barnetsAlderVilkårValidator2021og2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<VilkårLovverkInformasjonForBarn>()) }
    }

    @Test
    fun `skal kalle på riktig valideringsfunksjon for adopsjonsbarn født så det fra fødselsdato ville ha vært truffet av både lovverk 2021 og 2024`() {
        // Arrange
        val fødselsedato = DATO_LOVENDRING_2024.minusMonths(19)
        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsedato,
                personType = PersonType.BARN,
            )

        val vilkårResultatPeriode =
            IkkeNullbarPeriode(
                lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER, utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON)),
                DATO_LOVENDRING_2024.plusMonths(2),
                DATO_LOVENDRING_2024.plusMonths(8),
            )

        val vilkårResultatPerioder = listOf(vilkårResultatPeriode)

        every {
            barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(
                perioder = vilkårResultatPerioder,
                barn = person,
                periodeFomBarnetsAlderLov2024 = fødselsedato.plusMonths(13),
                periodeTomBarnetsAlderLov2024 = fødselsedato.plusMonths(19),
            )
        }.returns(listOf("feilmelding"))

        // Act
        barnetsAlderVilkårValidator.validerVilkårBarnetsAlder(
            perioder = vilkårResultatPerioder,
            barn = person,
            adopsjonsdato = fødselsedato.plusMonths(2),
        )

        // Assert
        verify(exactly = 1) {
            barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(
                perioder = vilkårResultatPerioder,
                barn = person,
                periodeFomBarnetsAlderLov2024 = fødselsedato.plusMonths(13),
                periodeTomBarnetsAlderLov2024 = fødselsedato.plusMonths(19),
            )
        }
    }

    @Test
    fun `skal kalle på riktig valideringsfunksjon for adopsjonsbarn født før 'lovendring februar 2025'-grense, men adoptert etter`() {
        // Arrange
        val fødselsedato = LovverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025.minusMonths(2)
        val adopsjonsdato = LovverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025.plusMonths(2)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsedato,
                personType = PersonType.BARN,
            )

        val vilkårResultatPeriode =
            IkkeNullbarPeriode(
                lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER, utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON)),
                adopsjonsdato.plusMonths(2),
                adopsjonsdato.plusMonths(8),
            )

        val vilkårResultatPerioder = listOf(vilkårResultatPeriode)

        every {
            barnetsAlderVilkårValidator2025.validerBarnetsAlderVilkår(
                perioder = vilkårResultatPerioder,
                barn = person,
                periodeFomBarnetsAlderLov2025 = fødselsedato.plusMonths(12),
                periodeTomBarnetsAlderLov2025 = fødselsedato.plusMonths(20),
            )
        }.returns(listOf("feilmelding"))

        // Act
        barnetsAlderVilkårValidator.validerVilkårBarnetsAlder(
            perioder = vilkårResultatPerioder,
            barn = person,
            adopsjonsdato = adopsjonsdato,
        )

        // Assert
        verify(exactly = 1) {
            barnetsAlderVilkårValidator2025.validerBarnetsAlderVilkår(
                perioder = vilkårResultatPerioder,
                barn = person,
                periodeFomBarnetsAlderLov2025 = fødselsedato.plusMonths(12),
                periodeTomBarnetsAlderLov2025 = fødselsedato.plusMonths(20),
            )
        }
    }

    @Test
    fun `skal returnere feil når validering for 2025 returnerer feil`() {
        // Arrange
        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = LovverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025,
            )

        val vilkårResultatPeriode =
            IkkeNullbarPeriode(
                lagVilkårResultat(),
                person.fødselsdato.plusMonths(12),
                person.fødselsdato.plusMonths(20),
            )

        val vilkårResultatPerioder = listOf(vilkårResultatPeriode)

        every {
            barnetsAlderVilkårValidator2025.validerBarnetsAlderVilkår(
                vilkårResultatPerioder,
                person,
                any<LocalDate>(),
                any<LocalDate>(),
            )
        }.returns(listOf("Feilmelding"))

        // Act
        val feil =
            barnetsAlderVilkårValidator.validerVilkårBarnetsAlder(
                perioder = vilkårResultatPerioder,
                barn = person,
                adopsjonsdato = null,
            )

        // Assert
        assertThat(feil).hasSize(1)
        assertThat(feil).contains("Feilmelding")
        verify(exactly = 0) { barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 0) { barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 0) { barnetsAlderVilkårValidator2021og2024.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<VilkårLovverkInformasjonForBarn>()) }
        verify(exactly = 1) { barnetsAlderVilkårValidator2025.validerBarnetsAlderVilkår(vilkårResultatPerioder, person, any<LocalDate>(), any<LocalDate>()) }
    }
}
