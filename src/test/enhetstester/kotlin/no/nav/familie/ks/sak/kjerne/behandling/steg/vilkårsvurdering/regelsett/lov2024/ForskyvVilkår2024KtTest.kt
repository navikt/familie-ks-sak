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
    val januar = YearMonth.of(2025, 1)

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
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = desember.atDay(2),
                    periodeTom = null,
                    antallTimer = BigDecimal.valueOf(40),
                    resultat = Resultat.IKKE_OPPFYLT,
                ),
            )

        // Act
        val forskjøvedeVilkårResultater =
            forskyvEtterLovgivning2024(
                Vilkår.BARNEHAGEPLASS,
                vilkårResultater,
            )

        // Arrange
        assertThat(forskjøvedeVilkårResultater).hasSize(3)
        assertThat(forskjøvedeVilkårResultater[0].fom).isEqualTo(august.atDay(1))
        assertThat(forskjøvedeVilkårResultater[0].tom).isEqualTo(september.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater[1].fom).isEqualTo(oktober.atDay(1))
        assertThat(forskjøvedeVilkårResultater[1].tom).isEqualTo(desember.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater[2].fom).isEqualTo(januar.atDay(1))
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
}
