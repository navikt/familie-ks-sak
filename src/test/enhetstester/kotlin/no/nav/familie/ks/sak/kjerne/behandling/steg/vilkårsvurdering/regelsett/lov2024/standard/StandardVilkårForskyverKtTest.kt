package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.standard

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class StandardVilkårForskyverKtTest {
    @Test
    fun `skal kaste exception om man prøver å forskyve barnehageplass`() {
        // Arrange
        val alleVilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                ),
            )

        // Act & assert
        val exception =
            assertThrows<IllegalArgumentException> {
                forskyvStandardVilkår2024(
                    Vilkår.BARNEHAGEPLASS,
                    alleVilkårResultater,
                )
            }
        assertThat(exception.message).isEqualTo("BARNEHAGEPLASS skal ikke behandles etter standard logikk")
    }

    @Test
    fun `skal filtrer bort vilkårresultat som ikke er riktig vilkårtype`() {
        // Arrange
        val alleVilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                ),
            )

        // Ac
        val forskjøvetVilkårResultater =
            forskyvStandardVilkår2024(
                Vilkår.MEDLEMSKAP,
                alleVilkårResultater,
            )

        // Assert
        assertThat(forskjøvetVilkårResultater).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(
        value = Resultat::class,
        names = ["OPPFYLT", "IKKE_AKTUELT"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `skal filtrer bort vilkårresultat som ikke har riktig resultat`(
        resultat: Resultat,
    ) {
        // Arrange
        val alleVilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = resultat,
                ),
            )

        // Ac
        val forskjøvetVilkårResultater =
            forskyvStandardVilkår2024(
                Vilkår.BOR_MED_SØKER,
                alleVilkårResultater,
            )

        // Assert
        assertThat(forskjøvetVilkårResultater).isEmpty()
    }

    @Test
    fun `skal standard forskyve ett vilkårresultat`() {
        // Arrange
        val dagensDato = LocalDate.of(2024, 11, 17)

        val vilkårResultatBorMedSøker =
            lagVilkårResultat(
                vilkårType = Vilkår.BOR_MED_SØKER,
                resultat = Resultat.OPPFYLT,
                periodeFom = dagensDato.minusMonths(1),
                periodeTom = dagensDato.plusMonths(1),
            )

        val alleVilkårResultater = listOf(vilkårResultatBorMedSøker)

        // Ac
        val forskjøvetVilkårResultater =
            forskyvStandardVilkår2024(
                Vilkår.BOR_MED_SØKER,
                alleVilkårResultater,
            )

        // Assert
        assertThat(forskjøvetVilkårResultater).hasSize(1)
        assertThat(forskjøvetVilkårResultater).anySatisfy {
            assertThat(it.fom).isEqualTo(LocalDate.of(2024, 10, 1))
            assertThat(it.tom).isEqualTo(LocalDate.of(2024, 12, 31))
            assertThat(it.verdi).isEqualTo(vilkårResultatBorMedSøker)
        }
    }

    @Test
    fun `skal standard forskyve flere vilkårresultater`() {
        // Arrange
        val dagensDato = LocalDate.of(2024, 11, 17)

        val vilkårResultatBorMedSøker1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BOR_MED_SØKER,
                resultat = Resultat.OPPFYLT,
                periodeFom = dagensDato.minusMonths(1),
                periodeTom = dagensDato.plusMonths(1),
            )

        val vilkårResultatBorMedSøker2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BOR_MED_SØKER,
                resultat = Resultat.OPPFYLT,
                periodeFom = dagensDato.plusMonths(1).plusDays(1),
                periodeTom = dagensDato.plusMonths(3),
            )

        val alleVilkårResultater = listOf(vilkårResultatBorMedSøker1, vilkårResultatBorMedSøker2)

        // Ac
        val forskjøvetVilkårResultater =
            forskyvStandardVilkår2024(
                Vilkår.BOR_MED_SØKER,
                alleVilkårResultater,
            )

        // Assert
        assertThat(forskjøvetVilkårResultater).hasSize(2)
        assertThat(forskjøvetVilkårResultater).anySatisfy {
            assertThat(it.fom).isEqualTo(LocalDate.of(2024, 10, 1))
            assertThat(it.tom).isEqualTo(LocalDate.of(2024, 12, 17))
            assertThat(it.verdi).isEqualTo(vilkårResultatBorMedSøker1)
        }
        assertThat(forskjøvetVilkårResultater).anySatisfy {
            assertThat(it.fom).isEqualTo(LocalDate.of(2024, 12, 18))
            assertThat(it.tom).isEqualTo(LocalDate.of(2025, 2, 28))
            assertThat(it.verdi).isEqualTo(vilkårResultatBorMedSøker2)
        }
    }
}
