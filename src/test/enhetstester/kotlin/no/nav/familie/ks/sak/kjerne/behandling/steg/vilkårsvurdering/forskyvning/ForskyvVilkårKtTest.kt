package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning

import no.nav.familie.ks.sak.data.lagPersonResultatFraVilkårResultater
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.lovverk.Lovverk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
}
