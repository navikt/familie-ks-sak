package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning

import jan
import mai
import no.nav.familie.ks.sak.common.util.toLocalDate
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonResultatFraVilkårResultater
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

class ForskyvVilkårKtTest {
    val august = YearMonth.of(2022, 8)
    val september = YearMonth.of(2022, 9)
    val oktober = YearMonth.of(2022, 10)
    val november = YearMonth.of(2022, 11)
    val desember = YearMonth.of(2022, 12)

    @Nested
    inner class ForskyvVilkårResultaterTest {
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

        @Test
        fun `skal forskyve barn og søker hver for seg og søker skal forskyves basert på barnas lovverk tidslinje`() {
            // Arrange
            val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
            val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = jan(2020).toLocalDate())
            val barn2 = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = mai(2023).toLocalDate())
            val personopplysningGrunnlag = lagPersonopplysningGrunnlag(søkerAktør = søker.aktør, barnAktør = listOf(barn.aktør, barn2.aktør), barnasFødselsdatoer = listOf(barn.fødselsdato, barn2.fødselsdato))

            val vilkårResultaterBarn = setOf(lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER, periodeFom = barn.fødselsdato.plusYears(1), periodeTom = barn.fødselsdato.plusYears(2)))
            val vilkårResultaterBarn2 = setOf(lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER, periodeFom = barn2.fødselsdato.plusYears(1), periodeTom = barn2.fødselsdato.plusMonths(19)))
            val vilkårResultaterSøker =
                setOf(
                    lagVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET, periodeFom = søker.fødselsdato, periodeTom = barn.fødselsdato.plusMonths(15)),
                    lagVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET, periodeFom = barn.fødselsdato.plusMonths(20), periodeTom = null),
                )

            val personResultater =
                listOf(
                    lagPersonResultatFraVilkårResultater(
                        vilkårResultaterBarn,
                        barn.aktør,
                    ),
                    lagPersonResultatFraVilkårResultater(
                        vilkårResultaterBarn2,
                        barn2.aktør,
                    ),
                    lagPersonResultatFraVilkårResultater(
                        vilkårResultaterSøker,
                        søker.aktør,
                    ),
                )

            // Act
            val forskjøvedeVilkårResultater = personResultater.forskyvVilkårResultater(personopplysningGrunnlag = personopplysningGrunnlag, skalBestemmeLovverkBasertPåFødselsdato = true)

            // Assert
            assertThat(forskjøvedeVilkårResultater).hasSize(3)
        }
    }
}
