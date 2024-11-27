package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.YearMonth

class ForskyvVilkår2024KtTest {
    val august = YearMonth.of(2024, 8)
    val september = YearMonth.of(2024, 9)
    val oktober = YearMonth.of(2024, 10)
    val desember = YearMonth.of(2024, 12)

    @Test
    fun `skal forskyve en liste av VilkårResultat med VilkårType BARNEHAGEPLASS`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = august.atDay(15),
                    periodeTom = oktober.atDay(14),
                    antallTimer = BigDecimal.valueOf(20L),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(15),
                    periodeTom = desember.atDay(1),
                    antallTimer = BigDecimal.valueOf(20L),
                ),
            )

        // Act
        val forskjøvedeVilkårResultater =
            forskyvEtterLovgivning2024(
                Vilkår.BARNEHAGEPLASS,
                vilkårResultater,
            )

        // Arrange
        assertThat(forskjøvedeVilkårResultater).hasSize(2)
        assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(august.atDay(1))
        assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(september.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(oktober.atDay(1))
        assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(desember.atEndOfMonth())
    }

    @ParameterizedTest
    @EnumSource(value = Vilkår::class, names = ["BARNEHAGEPLASS"], mode = EnumSource.Mode.EXCLUDE)
    fun `skal filtrere bort VilkårResultat hvor resultatet er hverken OPPFYLT eller IKKE_AKTULET hvor VilkårType ikke er BARNEHAGEPLASS`(vilkår: Vilkår) {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = vilkår,
                    periodeFom = august.atDay(15),
                    periodeTom = september.atDay(14),
                    resultat = Resultat.IKKE_OPPFYLT,
                ),
                lagVilkårResultat(
                    vilkårType = vilkår,
                    periodeFom = oktober.atDay(15),
                    periodeTom = desember.atDay(1),
                    resultat = Resultat.IKKE_VURDERT,
                ),
            )

        // Act
        val forskjøvedeVilkårResultater =
            forskyvEtterLovgivning2024(
                vilkår,
                vilkårResultater,
            )

        // Assert
        assertThat(forskjøvedeVilkårResultater).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(value = Vilkår::class, names = ["BARNEHAGEPLASS"], mode = EnumSource.Mode.EXCLUDE)
    fun `skal forskyve en liste av VilkårResultat hvor VilkårType ikke er BARNEHAGEPLASS`(vilkår: Vilkår) {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = vilkår,
                    periodeFom = august.atDay(15),
                    periodeTom = oktober.atDay(14),
                    resultat = Resultat.IKKE_VURDERT,
                ),
                lagVilkårResultat(
                    vilkårType = vilkår,
                    periodeFom = oktober.atDay(15),
                    periodeTom = desember.atDay(1),
                    resultat = Resultat.OPPFYLT,
                ),
            )

        // Act
        val forskjøvedeVilkårResultater =
            forskyvEtterLovgivning2024(
                vilkår,
                vilkårResultater,
            )

        // Arrange
        assertThat(forskjøvedeVilkårResultater).hasSize(1)
        assertThat(forskjøvedeVilkårResultater).allSatisfy {
            assertThat(it.fom).isEqualTo(oktober.atDay(1))
            assertThat(it.tom).isEqualTo(desember.atEndOfMonth())
        }
    }

    @Test
    fun `Ved to oppfylte vilkårresultat i samme måned så skal det seneste vilkåret være gjeldende for måneden`() {
        // Arrange
        val førstePeriodeBorMedSøker =
            lagVilkårResultat(
                vilkårType = Vilkår.BOR_MED_SØKER,
                periodeFom = august.atDay(15),
                periodeTom = oktober.atDay(14),
                resultat = Resultat.OPPFYLT,
            )
        val andrePeriodeBorMedSøker =
            lagVilkårResultat(
                vilkårType = Vilkår.BOR_MED_SØKER,
                periodeFom = oktober.atDay(15),
                periodeTom = desember.atDay(1),
                resultat = Resultat.OPPFYLT,
            )

        val vilkårResultater =
            listOf(
                førstePeriodeBorMedSøker,
                andrePeriodeBorMedSøker,
            )

        // Act
        val forskjøvedeVilkårResultater =
            forskyvEtterLovgivning2024(
                Vilkår.BOR_MED_SØKER,
                vilkårResultater,
            )

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(2)

        assertThat(forskjøvedeVilkårResultater[0].fom).isEqualTo(august.atDay(1))
        assertThat(forskjøvedeVilkårResultater[0].tom).isEqualTo(september.atDay(30))
        assertThat(forskjøvedeVilkårResultater[0].verdi).isEqualTo(førstePeriodeBorMedSøker)

        assertThat(forskjøvedeVilkårResultater[1].fom).isEqualTo(oktober.atDay(1))
        assertThat(forskjøvedeVilkårResultater[1].tom).isEqualTo(desember.atDay(31))
        assertThat(forskjøvedeVilkårResultater[1].verdi).isEqualTo(andrePeriodeBorMedSøker)
    }
}
