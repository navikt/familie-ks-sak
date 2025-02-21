package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning

import jan
import mai
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.førsteDagINesteMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toLocalDate
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonResultatFraVilkårResultater
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.lovverk.Lovverk
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class ForskyvVilkårKtTest {
    @Nested
    inner class ForskyvVilkårResultaterTest2022 {
        val august = YearMonth.of(2022, 8)
        val september = YearMonth.of(2022, 9)
        val oktober = YearMonth.of(2022, 10)
        val november = YearMonth.of(2022, 11)
        val desember = YearMonth.of(2022, 12)

        @Test
        fun `skal ikke lage opphold i vilkår som ligger back to back`() {
            // Arrange
            val vilkårResultater =
                setOf(
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = august.atDay(15),
                        periodeTom = oktober.atDay(14),
                    ),
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = oktober.atDay(15),
                        periodeTom = desember.atDay(1),
                    ),
                )

            val personResultat = lagPersonResultatFraVilkårResultater(vilkårResultater, randomAktør())

            // Act
            val forskjøvedeVilkårResultater =
                personResultat
                    .forskyvVilkårResultater()
                    .values
                    .flatten()

            // Arrange
            assertThat(forskjøvedeVilkårResultater).hasSize(2)
            assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(september.atDay(1))
            assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(oktober.atEndOfMonth())
            assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(november.atDay(1))
            assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(november.atEndOfMonth())
        }

        @Test
        fun `skal lage opphold i vilkårene ved perioder som ikke er back to back`() {
            // Arrange
            val vilkårResultater =
                setOf(
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = august.atDay(15),
                        periodeTom = oktober.atDay(13),
                        resultat = Resultat.OPPFYLT,
                    ),
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = oktober.atDay(14),
                        periodeTom = oktober.atDay(14),
                        resultat = Resultat.IKKE_OPPFYLT,
                    ),
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = oktober.atDay(15),
                        periodeTom = desember.atDay(1),
                        resultat = Resultat.OPPFYLT,
                    ),
                )

            val personResultat = lagPersonResultatFraVilkårResultater(vilkårResultater, randomAktør())

            // Act
            val forskjøvedeVilkårResultater =
                personResultat
                    .forskyvVilkårResultater()
                    .values
                    .flatten()

            // Assert
            assertThat(forskjøvedeVilkårResultater).hasSize(2)
            assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(september.atDay(1))
            assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(september.atEndOfMonth())
            assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(november.atDay(1))
            assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(november.atEndOfMonth())
        }

        @Test
        fun `skal ikke lage opphold i vilkår som ligger back to back i månedsskifte`() {
            // Arrange
            val vilkårResultater =
                setOf(
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = august.atDay(15),
                        periodeTom = august.atEndOfMonth(),
                    ),
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = september.atDay(1),
                        periodeTom = desember.atDay(1),
                    ),
                )

            val personResultat = lagPersonResultatFraVilkårResultater(vilkårResultater, randomAktør())

            // Act
            val forskjøvedeVilkårResultater =
                personResultat
                    .forskyvVilkårResultater()
                    .values
                    .flatten()

            // Assert
            assertThat(forskjøvedeVilkårResultater).hasSize(2)
            assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(september.atDay(1))
            assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(september.atEndOfMonth())
            assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(oktober.atDay(1))
            assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(november.atEndOfMonth())
        }

        @Test
        fun `skal bare lage opphold i vilkår som varer lengre enn en hel måned`() {
            // Arrange
            val vilkårResultater =
                setOf(
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = august.atDay(15),
                        periodeTom = september.atEndOfMonth(),
                    ),
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = oktober.atDay(2),
                        periodeTom = desember.atDay(1),
                    ),
                )

            val personResultat = lagPersonResultatFraVilkårResultater(vilkårResultater, randomAktør())

            // Act
            val forskjøvedeVilkårResultater =
                personResultat
                    .forskyvVilkårResultater()
                    .values
                    .flatten()

            // Assert
            assertThat(forskjøvedeVilkårResultater).hasSize(1)
            assertThat(forskjøvedeVilkårResultater).allSatisfy {
                assertThat(it.fom).isEqualTo(november.atDay(1))
                assertThat(it.tom).isEqualTo(november.atEndOfMonth())
            }
        }

        @Test
        fun `skal filtrere bort peroder som ikke gjelder for noen måneder`() {
            // Arrange
            val vilkårResultater =
                setOf(
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = august.atDay(15),
                        periodeTom = september.atEndOfMonth(),
                    ),
                )

            val personResultat = lagPersonResultatFraVilkårResultater(vilkårResultater, randomAktør())

            // Act
            val forskjøvedeVilkårResultater =
                personResultat
                    .forskyvVilkårResultater()
                    .values
                    .flatten()

            // Assert
            assertThat(forskjøvedeVilkårResultater).isEmpty()
        }
    }

    @Nested
    inner class ForskyvVilkårResultaterTest2025 {
        val august = YearMonth.of(2025, 8)
        val september = YearMonth.of(2025, 9)
        val oktober = YearMonth.of(2025, 10)
        val november = YearMonth.of(2025, 11)
        val desember = YearMonth.of(2025, 12)

        @Test
        fun `skal ikke lage opphold i vilkår som ligger back to back`() {
            // Arrange
            val vilkårResultater =
                setOf(
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = august.atDay(15),
                        periodeTom = oktober.atDay(14),
                    ),
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = oktober.atDay(15),
                        periodeTom = desember.atDay(1),
                    ),
                )

            val personResultat = lagPersonResultatFraVilkårResultater(vilkårResultater, randomAktør())

            // Act
            val forskjøvedeVilkårResultater =
                personResultat
                    .forskyvVilkårResultater(Lovverk.LOVENDRING_FEBRUAR_2025)
                    .values
                    .flatten()

            // Arrange
            assertThat(forskjøvedeVilkårResultater).hasSize(2)
            assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(september.atDay(1))
            assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(oktober.atEndOfMonth())
            assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(november.atDay(1))
            assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(november.atEndOfMonth())
        }

        @Test
        fun `skal lage opphold i vilkårene ved perioder som ikke er back to back`() {
            // Arrange
            val vilkårResultater =
                setOf(
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = august.atDay(15),
                        periodeTom = oktober.atDay(13),
                        resultat = Resultat.OPPFYLT,
                    ),
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = oktober.atDay(14),
                        periodeTom = oktober.atDay(14),
                        resultat = Resultat.IKKE_OPPFYLT,
                    ),
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = oktober.atDay(15),
                        periodeTom = desember.atDay(1),
                        resultat = Resultat.OPPFYLT,
                    ),
                )

            val personResultat = lagPersonResultatFraVilkårResultater(vilkårResultater, randomAktør())

            // Act
            val forskjøvedeVilkårResultater =
                personResultat
                    .forskyvVilkårResultater(Lovverk.LOVENDRING_FEBRUAR_2025)
                    .values
                    .flatten()

            // Assert
            assertThat(forskjøvedeVilkårResultater).hasSize(2)
            assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(september.atDay(1))
            assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(september.atEndOfMonth())
            assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(november.atDay(1))
            assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(november.atEndOfMonth())
        }

        @Test
        fun `skal ikke lage opphold i vilkår som ligger back to back i månedsskifte`() {
            // Arrange
            val vilkårResultater =
                setOf(
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = august.atDay(15),
                        periodeTom = august.atEndOfMonth(),
                    ),
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = september.atDay(1),
                        periodeTom = desember.atDay(1),
                    ),
                )

            val personResultat = lagPersonResultatFraVilkårResultater(vilkårResultater, randomAktør())

            // Act
            val forskjøvedeVilkårResultater =
                personResultat
                    .forskyvVilkårResultater(Lovverk.LOVENDRING_FEBRUAR_2025)
                    .values
                    .flatten()

            // Assert
            assertThat(forskjøvedeVilkårResultater).hasSize(2)
            assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(september.atDay(1))
            assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(september.atEndOfMonth())
            assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(oktober.atDay(1))
            assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(november.atEndOfMonth())
        }

        @Test
        fun `skal bare lage opphold i vilkår som varer lengre enn en hel måned`() {
            // Arrange
            val vilkårResultater =
                setOf(
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = august.atDay(15),
                        periodeTom = september.atEndOfMonth(),
                    ),
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = oktober.atDay(2),
                        periodeTom = desember.atDay(1),
                    ),
                )

            val personResultat = lagPersonResultatFraVilkårResultater(vilkårResultater, randomAktør())

            // Act
            val forskjøvedeVilkårResultater =
                personResultat
                    .forskyvVilkårResultater(Lovverk.LOVENDRING_FEBRUAR_2025)
                    .values
                    .flatten()

            // Assert
            assertThat(forskjøvedeVilkårResultater).hasSize(1)
            assertThat(forskjøvedeVilkårResultater).allSatisfy {
                assertThat(it.fom).isEqualTo(november.atDay(1))
                assertThat(it.tom).isEqualTo(november.atEndOfMonth())
            }
        }

        @Test
        fun `skal filtrere bort peroder som ikke gjelder for noen måneder`() {
            // Arrange
            val vilkårResultater =
                setOf(
                    lagVilkårResultat(
                        vilkårType = Vilkår.BARNETS_ALDER,
                        periodeFom = august.atDay(15),
                        periodeTom = september.atEndOfMonth(),
                    ),
                )

            val personResultat = lagPersonResultatFraVilkårResultater(vilkårResultater, randomAktør())

            // Act
            val forskjøvedeVilkårResultater =
                personResultat
                    .forskyvVilkårResultater(Lovverk.LOVENDRING_FEBRUAR_2025)
                    .values
                    .flatten()

            // Assert
            assertThat(forskjøvedeVilkårResultater).isEmpty()
        }
    }

    @Nested
    inner class PersonResultaterForskyvVilkårResultater {
        @Test
        fun `skal forskyve barn og søker hver for seg og søker skal forskyves basert på barnas lovverk tidslinje`() {
            // Arrange
            val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
            val barn1 = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = jan(2020).toLocalDate())
            val barn2 = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = mai(2023).toLocalDate())
            val barn3 = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = jan(2024).toLocalDate())

            val personopplysningGrunnlag = lagPersonopplysningGrunnlag(søkerAktør = søker.aktør, barnAktør = listOf(barn1.aktør, barn2.aktør, barn3.aktør), barnasFødselsdatoer = listOf(barn1.fødselsdato, barn2.fødselsdato, barn3.fødselsdato))

            val vilkårResultaterBarn = setOf(lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER, periodeFom = barn1.fødselsdato.plusYears(1), periodeTom = barn1.fødselsdato.plusYears(2)))
            val vilkårResultaterBarn2 = setOf(lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER, periodeFom = barn2.fødselsdato.plusYears(1), periodeTom = barn2.fødselsdato.plusMonths(19)))
            val vilkårResultaterBarn3 = setOf(lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER, periodeFom = barn3.fødselsdato.plusMonths(12), periodeTom = barn3.fødselsdato.plusMonths(20)))

            val søkerFlyttetFraNorge = LocalDate.of(2023, 10, 5)
            val søkerFlyttetTilbakeTilNorge = LocalDate.of(2024, 10, 3)
            val søkerFlyttetFraNorgeIgjen = LocalDate.of(2025, 6, 4)
            val vilkårResultaterSøker =
                setOf(
                    lagVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET, periodeFom = søker.fødselsdato, periodeTom = søkerFlyttetFraNorge),
                    lagVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET, periodeFom = søkerFlyttetTilbakeTilNorge, periodeTom = søkerFlyttetFraNorgeIgjen),
                )

            val personResultater =
                listOf(
                    lagPersonResultatFraVilkårResultater(
                        vilkårResultaterBarn,
                        barn1.aktør,
                    ),
                    lagPersonResultatFraVilkårResultater(
                        vilkårResultaterBarn2,
                        barn2.aktør,
                    ),
                    lagPersonResultatFraVilkårResultater(
                        vilkårResultaterBarn3,
                        barn3.aktør,
                    ),
                    lagPersonResultatFraVilkårResultater(
                        vilkårResultaterSøker,
                        søker.aktør,
                    ),
                )

            // Act
            val forskjøvedeVilkårResultater =
                personResultater.forskyvVilkårResultater(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    adopsjonerIBehandling = emptyList(),
                )

            // Assert
            assertThat(forskjøvedeVilkårResultater).hasSize(4)

            val barn1AlderVilkår = forskjøvedeVilkårResultater[barn1.aktør]!![Vilkår.BARNETS_ALDER]!!.single()
            assertThat(barn1AlderVilkår.verdi.periodeFom).isEqualTo(barn1.fødselsdato.plusMonths(12))
            assertThat(barn1AlderVilkår.verdi.periodeTom).isEqualTo(barn1.fødselsdato.plusMonths(24))
            // Fom og tom forskjøvet etter Lov 2021
            assertThat(barn1AlderVilkår.fom).isEqualTo(barn1.fødselsdato.plusMonths(13).førsteDagIInneværendeMåned())
            assertThat(barn1AlderVilkår.tom).isEqualTo(barn1.fødselsdato.plusMonths(23).sisteDagIMåned())

            val barn2AlderVilkår = forskjøvedeVilkårResultater[barn2.aktør]!![Vilkår.BARNETS_ALDER]!!.single()
            assertThat(barn2AlderVilkår.verdi.periodeFom).isEqualTo(barn2.fødselsdato.plusMonths(12))
            assertThat(barn2AlderVilkår.verdi.periodeTom).isEqualTo(barn2.fødselsdato.plusMonths(19))
            // Fom forskjøvet etter Lov 2021
            assertThat(barn2AlderVilkår.fom).isEqualTo(barn2.fødselsdato.plusMonths(13).førsteDagIInneværendeMåned())
            // Tom forskjøvet etter Lov 2024
            assertThat(barn2AlderVilkår.tom).isEqualTo(barn2.fødselsdato.plusMonths(19).sisteDagIMåned())

            val barn3AlderVilkår = forskjøvedeVilkårResultater[barn3.aktør]!![Vilkår.BARNETS_ALDER]!!.single()
            assertThat(barn3AlderVilkår.verdi.periodeFom).isEqualTo(barn3.fødselsdato.plusMonths(12))
            assertThat(barn3AlderVilkår.verdi.periodeTom).isEqualTo(barn3.fødselsdato.plusMonths(20))
            // Fom og Tom forskjøvet etter Lov 2025
            assertThat(barn3AlderVilkår.fom).isEqualTo(barn3.fødselsdato.plusMonths(13).førsteDagIInneværendeMåned())
            assertThat(barn3AlderVilkår.tom).isEqualTo(barn3.fødselsdato.plusMonths(19).sisteDagIMåned())

            val søkerBosattIRiketVilkår = forskjøvedeVilkårResultater[søker.aktør]!![Vilkår.BOSATT_I_RIKET]!!
            assertThat(søkerBosattIRiketVilkår).hasSize(2)
            // Søkers første periode
            assertThat(søkerBosattIRiketVilkår.first().verdi.periodeFom).isEqualTo(søker.fødselsdato)
            assertThat(søkerBosattIRiketVilkår.first().verdi.periodeTom).isEqualTo(søkerFlyttetFraNorge)
            // Fom forskjøvet etter Lov 2021
            assertThat(søkerBosattIRiketVilkår.first().fom).isEqualTo(søker.fødselsdato.førsteDagINesteMåned())
            // Tom forskjøvet etter Lov 2021
            assertThat(søkerBosattIRiketVilkår.first().tom).isEqualTo(søkerFlyttetFraNorge.minusMonths(1).sisteDagIMåned())

            // Søkers andre periode
            assertThat(søkerBosattIRiketVilkår.last().verdi.periodeFom).isEqualTo(søkerFlyttetTilbakeTilNorge)
            assertThat(søkerBosattIRiketVilkår.last().verdi.periodeTom).isEqualTo(søkerFlyttetFraNorgeIgjen)
            // Fom forskjøvet etter Lov 2024
            assertThat(søkerBosattIRiketVilkår.last().fom).isEqualTo(søkerFlyttetTilbakeTilNorge.førsteDagIInneværendeMåned())
            // Tom forskjøvet etter Lov 2025
            assertThat(søkerBosattIRiketVilkår.last().tom).isEqualTo(søkerFlyttetFraNorgeIgjen.minusMonths(1).sisteDagIMåned())
        }
    }
}
