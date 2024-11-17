package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.standard

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.TilknyttetVilkårResultater
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TomForskyverKtTest {
    private val dagensDato = LocalDate.of(2024, 11, 17)

    @Test
    fun `skal returnere null når gjeldende periode tom er null`() {
        // Arrange
        val gjeldendeVilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = dagensDato.minusMonths(3),
                periodeTom = null,
            )

        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                gjeldende = gjeldendeVilkårResultat,
                neste = null,
            )

        // Act
        val forskjøvetTom = forskyvTom(tilknyttetVilkårResultater)

        // Assert
        assertThat(forskjøvetTom).isNull()
    }

    @Test
    fun `skal returnere gjeldende vilkårresultat periode tom når gjeldene vilkårresultat periode slutter dagen før neste vilkårresultat`() {
        // Arrange
        val gjeldendeVilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BOR_MED_SØKER,
                periodeFom = dagensDato.minusMonths(3),
                periodeTom = dagensDato.minusDays(1),
            )

        val nesteVilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BOR_MED_SØKER,
                periodeFom = dagensDato,
                periodeTom = dagensDato.plusMonths(1),
            )

        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                gjeldende = gjeldendeVilkårResultat,
                neste = nesteVilkårResultat,
            )

        // Act
        val forskjøvetTom = forskyvTom(tilknyttetVilkårResultater)

        // Assert
        assertThat(forskjøvetTom).isEqualTo(gjeldendeVilkårResultat.periodeTom)
    }

    @Test
    fun `skal returnere siste dag i måneden basert på gjeldende vilkårresutlat periode tom når gjeldende vilkårresultat periode tom ikke er dagen før neste vilkårresultat periode`() {
        // Arrange
        val gjeldendeVilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BOR_MED_SØKER,
                periodeFom = dagensDato.minusMonths(3),
                periodeTom = dagensDato.minusDays(1),
            )

        val nesteVilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BOR_MED_SØKER,
                periodeFom = dagensDato.plusDays(1),
                periodeTom = dagensDato.plusMonths(1),
            )

        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                gjeldende = gjeldendeVilkårResultat,
                neste = nesteVilkårResultat,
            )

        // Act
        val forskjøvetTom = forskyvTom(tilknyttetVilkårResultater)

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 11, 30))
    }
}
