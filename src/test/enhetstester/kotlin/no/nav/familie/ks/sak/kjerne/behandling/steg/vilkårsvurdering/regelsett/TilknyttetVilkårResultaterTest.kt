package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårRegelsett
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class TilknyttetVilkårResultaterTest {
    val august = YearMonth.of(2022, 8)
    val september = YearMonth.of(2022, 9)
    val oktober = YearMonth.of(2022, 10)

    @Test
    fun `gjeldende slutter ikke dagen før neste om neste er null`() {
        // Arrange
        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNETS_ALDER,
                    periodeFom = august.atDay(1),
                    periodeTom = september.atDay(14),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                null,
            )

        // Act
        val gjeldendeSlutterDagenFørNeste = tilknyttetVilkårResultater.gjeldendeSlutterDagenFørNeste()

        // Assert
        assertThat(gjeldendeSlutterDagenFørNeste).isFalse()
    }

    @Test
    fun `gjeldende slutter ikke dagen før neste om gjeldende tom er null`() {
        // Arrange
        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNETS_ALDER,
                    periodeFom = august.atDay(1),
                    periodeTom = null,
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                // Perioden her er lagt inn kun for å teste gjeldende sin periodeTom
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNETS_ALDER,
                    periodeFom = september.atDay(15),
                    periodeTom = oktober.atDay(1),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2024,
                ),
            )

        // Act
        val gjeldendeSlutterDagenFørNeste = tilknyttetVilkårResultater.gjeldendeSlutterDagenFørNeste()

        // Assert
        assertThat(gjeldendeSlutterDagenFørNeste).isFalse()
    }

    @Test
    fun `gjeldende slutter dagen før neste`() {
        // Arrange
        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNETS_ALDER,
                    periodeFom = august.atDay(1),
                    periodeTom = september.atDay(14),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2024,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNETS_ALDER,
                    periodeFom = september.atDay(15),
                    periodeTom = oktober.atDay(1),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2024,
                ),
            )

        // Act
        val gjeldendeSlutterDagenFørNeste = tilknyttetVilkårResultater.gjeldendeSlutterDagenFørNeste()

        // Assert
        assertThat(gjeldendeSlutterDagenFørNeste).isTrue()
    }
}
