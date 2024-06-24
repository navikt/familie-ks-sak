package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårRegelsett
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class MapTilTilknyttetVilkårResultaterKtTest {
    @Test
    fun `skal returnere en tom liste når input er en tom liste`() {
        // Arrange
        val list: List<VilkårResultat> = emptyList()

        // Act
        val tilknyttetVilkårResultaterList = list.mapTilTilknyttetVilkårResultater()

        // Assert
        assertThat(tilknyttetVilkårResultaterList).isEmpty()
    }

    @Test
    fun `skal mappe to VilkårResultat til to TilknyttetVilkårResultat`() {
        // Arrange
        val august = YearMonth.of(2022, 8)
        val september = YearMonth.of(2022, 9)
        val oktober = YearMonth.of(2022, 10)

        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = august.atDay(1),
                periodeTom = september.atDay(14),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )

        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = september.atDay(15),
                periodeTom = oktober.atDay(1),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )

        val list: List<VilkårResultat> =
            listOf(
                vilkårResultat1,
                vilkårResultat2,
            )

        // Act
        val tilknyttetVilkårResultater = list.mapTilTilknyttetVilkårResultater()

        // Assert
        assertThat(tilknyttetVilkårResultater).hasSize(2)
        assertThat(tilknyttetVilkårResultater.first()).satisfies({
            assertThat(it.gjeldende).isEqualTo(vilkårResultat1)
            assertThat(it.neste).isEqualTo(vilkårResultat2)
        })
        assertThat(tilknyttetVilkårResultater.last()).satisfies({
            assertThat(it.gjeldende).isEqualTo(vilkårResultat2)
            assertThat(it.neste).isNull()
        })
    }

    @Test
    fun `skal mappe tre VilkårResultat til tre TilknyttetVilkårResultat`() {
        // Arrange
        val august = YearMonth.of(2022, 8)
        val september = YearMonth.of(2022, 9)
        val oktober = YearMonth.of(2022, 10)

        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = august.atDay(1),
                periodeTom = september.atDay(14),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )

        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = september.atDay(15),
                periodeTom = oktober.atDay(1),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )

        val vilkårResultat3 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = oktober.atDay(1),
                periodeTom = oktober.atDay(31),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )

        val list: List<VilkårResultat> =
            listOf(
                vilkårResultat1,
                vilkårResultat2,
                vilkårResultat3,
            )

        // Act
        val tilknyttetVilkårResultater = list.mapTilTilknyttetVilkårResultater()

        // Assert
        assertThat(tilknyttetVilkårResultater).hasSize(3)
        assertThat(tilknyttetVilkårResultater[0]).satisfies({
            assertThat(it.gjeldende).isEqualTo(vilkårResultat1)
            assertThat(it.neste).isEqualTo(vilkårResultat2)
        })
        assertThat(tilknyttetVilkårResultater[1]).satisfies({
            assertThat(it.gjeldende).isEqualTo(vilkårResultat2)
            assertThat(it.neste).isEqualTo(vilkårResultat3)
        })
        assertThat(tilknyttetVilkårResultater[2]).satisfies({
            assertThat(it.gjeldende).isEqualTo(vilkårResultat3)
            assertThat(it.neste).isNull()
        })
    }

    @Test
    fun `skal returnere sorterete TilknyttetVilkårResulateter bastert på gjeldende periode fom når VilkårResultat listen ikke er sortert på forhånd`() {
        // Arrange
        val august = YearMonth.of(2022, 8)
        val september = YearMonth.of(2022, 9)
        val oktober = YearMonth.of(2022, 10)

        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = september.atDay(15),
                periodeTom = oktober.atDay(1),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )

        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = august.atDay(1),
                periodeTom = september.atDay(14),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )

        val list: List<VilkårResultat> =
            listOf(
                vilkårResultat1,
                vilkårResultat2,
            )

        // Act
        val tilknyttetVilkårResultater = list.mapTilTilknyttetVilkårResultater()

        // Assert
        assertThat(tilknyttetVilkårResultater).hasSize(2)
        assertThat(tilknyttetVilkårResultater.first()).satisfies({
            assertThat(it.gjeldende).isEqualTo(vilkårResultat1)
            assertThat(it.neste).isEqualTo(vilkårResultat2)
        })
        assertThat(tilknyttetVilkårResultater.last()).satisfies({
            assertThat(it.gjeldende).isEqualTo(vilkårResultat2)
            assertThat(it.neste).isNull()
        })
    }

}
