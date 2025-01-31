package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lov2025.standard

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.standard.forskyvStandardVilkår
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class StandardVilkårForskyverTest {
    @Test
    fun `skal kaste exception om man prøver å forskyve vilkårresultat med type barnehageplass med standard logikk`() {
        // Arrange
        val vilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
            )

        val vilkårResultater = listOf(vilkårResultat)

        // Act & assert
        val exception =
            assertThrows<IllegalArgumentException> {
                forskyvStandardVilkår(vilkårResultater)
            }
        assertThat(exception.message).isEqualTo("Vilkårtype BARNEHAGEPLASS skal ikke forskyves etter standard logikk")
    }

    @ParameterizedTest
    @EnumSource(
        value = Resultat::class,
        names = ["OPPFYLT", "IKKE_AKTUELT"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `skal filtrer bort vilkårresultat som ikke er relevante for forskyvningen`(resultat: Resultat) {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.MEDLEMSKAP,
                    resultat = resultat,
                ),
            )

        // Act
        val forskjøvetVilkårResultater = forskyvStandardVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvetVilkårResultater).isEmpty()
    }

    @Test
    fun `skal forskyve standard vilkårresultat uten fom satt`() {
        // Arrange
        val vilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.MEDLEMSKAP,
                resultat = Resultat.OPPFYLT,
                periodeFom = null,
                periodeTom = LocalDate.of(2025, 2, 28),
            )

        val vilkårResultater = listOf(vilkårResultat)

        // Act
        val forskjøvetVilkårResultater = forskyvStandardVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvetVilkårResultater).hasSize(1)
        assertThat(forskjøvetVilkårResultater[0].fom).isNull()
        assertThat(forskjøvetVilkårResultater[0].tom).isEqualTo(LocalDate.of(2025, 1, 31))
        assertThat(forskjøvetVilkårResultater[0].verdi).isEqualTo(vilkårResultat)
    }

    @Test
    fun `skal forskyve standard vilkårresultat uten tom satt`() {
        // Arrange
        val vilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.MEDLEMSKAP,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.of(2025, 1, 31),
                periodeTom = null,
            )

        val vilkårResultater = listOf(vilkårResultat)

        // Act
        val forskjøvetVilkårResultater = forskyvStandardVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvetVilkårResultater).hasSize(1)
        assertThat(forskjøvetVilkårResultater[0].fom).isEqualTo(LocalDate.of(2025, 2, 1))
        assertThat(forskjøvetVilkårResultater[0].tom).isNull()
        assertThat(forskjøvetVilkårResultater[0].verdi).isEqualTo(vilkårResultat)
    }

    @Test
    fun `skal forskyve standard vilkårresultat med fom og tom satt`() {
        // Arrange
        val vilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.MEDLEMSKAP,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.of(2025, 1, 1),
                periodeTom = LocalDate.of(2025, 5, 31),
            )

        val vilkårResultater = listOf(vilkårResultat)

        // Act
        val forskjøvetVilkårResultater = forskyvStandardVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvetVilkårResultater).hasSize(1)
        assertThat(forskjøvetVilkårResultater[0].fom).isEqualTo(LocalDate.of(2025, 2, 1))
        assertThat(forskjøvetVilkårResultater[0].tom).isEqualTo(LocalDate.of(2025, 4, 30))
        assertThat(forskjøvetVilkårResultater[0].verdi).isEqualTo(vilkårResultat)
    }

    @Test
    fun `skal forskyve standard med flere vilkårresultat`() {
        // Arrange
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                resultat = Resultat.IKKE_AKTUELT,
                periodeFom = LocalDate.of(2025, 1, 1),
                periodeTom = LocalDate.of(2026, 1, 1),
            )

        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.MEDLEMSKAP,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.of(2025, 3, 1),
                periodeTom = LocalDate.of(2025, 7, 31),
            )

        val vilkårResultater = listOf(vilkårResultat1, vilkårResultat2)

        // Act
        val forskjøvetVilkårResultater = forskyvStandardVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvetVilkårResultater).hasSize(2)

        assertThat(forskjøvetVilkårResultater[0].fom).isEqualTo(LocalDate.of(2025, 2, 1))
        assertThat(forskjøvetVilkårResultater[0].tom).isEqualTo(LocalDate.of(2025, 12, 31))
        assertThat(forskjøvetVilkårResultater[0].verdi).isEqualTo(vilkårResultat1)

        assertThat(forskjøvetVilkårResultater[1].fom).isEqualTo(LocalDate.of(2025, 4, 1))
        assertThat(forskjøvetVilkårResultater[1].tom).isEqualTo(LocalDate.of(2025, 6, 30))
        assertThat(forskjøvetVilkårResultater[1].verdi).isEqualTo(vilkårResultat2)
    }
}
