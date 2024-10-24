package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.barnehageplass

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class AndreVilkårOppfyltTidspunktUtlederKtTest {
    @Test
    fun `skal kaste exception om vilkårresultatene inneholder vilkår for barnehageplass`() {
        // Arrange
        val barnehageplassVilkårresultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = LocalDate.of(2024, 5, 31),
                periodeTom = LocalDate.of(2024, 9, 15),
                resultat = Resultat.OPPFYLT,
            )

        val vilkårResultater = listOf(barnehageplassVilkårresultat)

        // Act & assert
        val exception =
            assertThrows<IllegalArgumentException> {
                utledTidligsteÅrMånedAlleAndreVilkårErOppfylt(
                    vilkårResultater,
                )
            }
        assertThat(exception.message).isEqualTo("Fant vilkår barnehageplass men forventent at det ikke skulle bli sendt inn")
    }

    @Test
    fun `skal finne tidligste tidspunkt for hvor alle vilkår er oppfylt`() {
        // Arrange
        val borMedSøker1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BOR_MED_SØKER,
                periodeFom = LocalDate.of(2024, 5, 31),
                periodeTom = LocalDate.of(2024, 9, 15),
                resultat = Resultat.OPPFYLT,
            )

        val borMedSøker2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BOR_MED_SØKER,
                periodeFom = LocalDate.of(2024, 10, 15),
                periodeTom = null,
                resultat = Resultat.OPPFYLT,
            )

        val medlemskap =
            lagVilkårResultat(
                vilkårType = Vilkår.MEDLEMSKAP,
                periodeFom = LocalDate.of(1995, 1, 1),
                periodeTom = null,
                resultat = Resultat.OPPFYLT,
            )

        val medlemskapAnnenForelder =
            lagVilkårResultat(
                vilkårType = Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                periodeFom = LocalDate.of(1995, 1, 1),
                periodeTom = null,
                resultat = Resultat.OPPFYLT,
            )

        val bosattIRiket =
            lagVilkårResultat(
                vilkårType = Vilkår.BOSATT_I_RIKET,
                periodeFom = LocalDate.of(2024, 10, 1),
                periodeTom = null,
                resultat = Resultat.OPPFYLT,
            )

        val barnetsAlder =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = LocalDate.of(2024, 5, 31),
                periodeTom = LocalDate.of(2024, 12, 31),
                resultat = Resultat.OPPFYLT,
            )

        val vilkårResultater =
            listOf(
                borMedSøker1,
                borMedSøker2,
                medlemskap,
                medlemskapAnnenForelder,
                bosattIRiket,
                barnetsAlder,
            )

        // Act
        val tidligsteÅrMånedAlleAndreVilkårresultaterErOppfylt =
            utledTidligsteÅrMånedAlleAndreVilkårErOppfylt(
                vilkårResultater,
            )

        // Assert
        assertThat(tidligsteÅrMånedAlleAndreVilkårresultaterErOppfylt).isEqualTo(
            YearMonth.of(2024, 10),
        )
    }

    @Test
    fun `skal returnere null da det ikke finnes et tidspunkt hvor alle vilkår er oppfylt`() {
        // Arrange
        val borMedSøker =
            lagVilkårResultat(
                vilkårType = Vilkår.BOR_MED_SØKER,
                periodeFom = LocalDate.of(2024, 5, 31),
                periodeTom = LocalDate.of(2024, 9, 15),
                resultat = Resultat.OPPFYLT,
            )

        val medlemskap =
            lagVilkårResultat(
                vilkårType = Vilkår.MEDLEMSKAP,
                periodeFom = LocalDate.of(1995, 1, 1),
                periodeTom = null,
                resultat = Resultat.OPPFYLT,
            )

        val medlemskapAnnenForelder =
            lagVilkårResultat(
                vilkårType = Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                periodeFom = LocalDate.of(1995, 1, 1),
                periodeTom = null,
                resultat = Resultat.OPPFYLT,
            )

        val bosattIRiket =
            lagVilkårResultat(
                vilkårType = Vilkår.BOSATT_I_RIKET,
                periodeFom = LocalDate.of(2024, 10, 1),
                periodeTom = null,
                resultat = Resultat.OPPFYLT,
            )

        val barnetsAlder =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = LocalDate.of(2024, 5, 31),
                periodeTom = LocalDate.of(2024, 12, 31),
                resultat = Resultat.OPPFYLT,
            )

        val vilkårResultater =
            listOf(
                borMedSøker,
                medlemskap,
                medlemskapAnnenForelder,
                bosattIRiket,
                barnetsAlder,
            )

        // Act
        val tidligsteÅrMånedAlleAndreVilkårresultaterErOppfylt =
            utledTidligsteÅrMånedAlleAndreVilkårErOppfylt(
                vilkårResultater,
            )

        // Assert
        assertThat(tidligsteÅrMånedAlleAndreVilkårresultaterErOppfylt).isNull()
    }
}
