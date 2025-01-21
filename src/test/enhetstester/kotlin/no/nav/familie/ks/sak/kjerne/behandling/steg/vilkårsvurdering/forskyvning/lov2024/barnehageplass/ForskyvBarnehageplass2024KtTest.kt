package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lov2024.barnehageplass

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.lov2024.barnehageplass.forskyvBarnehageplassVilkår2024
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ForskyvBarnehageplass2024KtTest {
    @Test
    fun `skal forskyve barnehageplass vilkår resultater`() {
        // Arrange
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                antallTimer = BigDecimal(0),
                periodeFom = LocalDate.of(2024, 8, 1),
                periodeTom = LocalDate.of(2024, 9, 15),
            )

        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                antallTimer = BigDecimal(15),
                periodeFom = LocalDate.of(2024, 9, 16),
                periodeTom = LocalDate.of(2024, 10, 16),
            )

        val vilkårResultat3 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                antallTimer = BigDecimal(30),
                periodeFom = LocalDate.of(2024, 10, 17),
                periodeTom = LocalDate.of(2024, 10, 31),
            )

        val vilkårResultat4 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                antallTimer = BigDecimal(0),
                periodeFom = LocalDate.of(2024, 11, 1),
                periodeTom = null,
            )

        val vilkårresultater =
            listOf(
                vilkårResultat1,
                vilkårResultat2,
                vilkårResultat3,
                vilkårResultat4,
            )

        // Act
        val forskyvBarnehageplassVilkår2024 = forskyvBarnehageplassVilkår2024(vilkårresultater)

        // Assert
        assertThat(forskyvBarnehageplassVilkår2024).hasSize(3)

        assertThat(forskyvBarnehageplassVilkår2024[0].verdi).isEqualTo(vilkårResultat1)
        assertThat(forskyvBarnehageplassVilkår2024[0].fom).isEqualTo(LocalDate.of(2024, 8, 1))
        assertThat(forskyvBarnehageplassVilkår2024[0].tom).isEqualTo(LocalDate.of(2024, 9, 30))

        assertThat(forskyvBarnehageplassVilkår2024[1].verdi).isEqualTo(vilkårResultat2)
        assertThat(forskyvBarnehageplassVilkår2024[1].fom).isEqualTo(LocalDate.of(2024, 10, 1))
        assertThat(forskyvBarnehageplassVilkår2024[1].tom).isEqualTo(LocalDate.of(2024, 10, 31))

        assertThat(forskyvBarnehageplassVilkår2024[2].verdi).isEqualTo(vilkårResultat4)
        assertThat(forskyvBarnehageplassVilkår2024[2].fom).isEqualTo(LocalDate.of(2024, 11, 1))
        assertThat(forskyvBarnehageplassVilkår2024[2].tom).isNull()
    }

    @Test
    fun `når man går fra ingen barnehageplass til gradert barnehageplass skal reduksjon skje i samme måned`() {
        // Arrange
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                antallTimer = BigDecimal(0),
                periodeFom = null,
                periodeTom = LocalDate.of(2024, 9, 15),
            )

        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                antallTimer = BigDecimal(20),
                periodeFom = LocalDate.of(2024, 9, 16),
                periodeTom = null,
            )

        val vilkårresultater =
            listOf(
                vilkårResultat1,
                vilkårResultat2,
            )

        // Act
        val forskyvBarnehageplassVilkår2024 = forskyvBarnehageplassVilkår2024(vilkårresultater)

        // Assert
        assertThat(forskyvBarnehageplassVilkår2024).hasSize(2)

        assertThat(forskyvBarnehageplassVilkår2024[0].verdi).isEqualTo(vilkårResultat1)
        assertThat(forskyvBarnehageplassVilkår2024[0].fom).isNull()
        assertThat(forskyvBarnehageplassVilkår2024[0].tom).isEqualTo(LocalDate.of(2024, 9, 30))

        assertThat(forskyvBarnehageplassVilkår2024[1].verdi).isEqualTo(vilkårResultat2)
        assertThat(forskyvBarnehageplassVilkår2024[1].fom).isEqualTo(LocalDate.of(2024, 10, 1))
        assertThat(forskyvBarnehageplassVilkår2024[1].tom).isNull()
    }

    @Test
    fun `når man går fra gradert barnehageplass til gradert barnehageplass skal reduksjon skje i samme måned`() {
        // Arrange
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                antallTimer = BigDecimal(10),
                periodeFom = null,
                periodeTom = LocalDate.of(2024, 9, 15),
            )

        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                antallTimer = BigDecimal(20),
                periodeFom = LocalDate.of(2024, 9, 16),
                periodeTom = null,
            )

        val vilkårresultater =
            listOf(
                vilkårResultat1,
                vilkårResultat2,
            )

        // Act
        val forskyvBarnehageplassVilkår2024 = forskyvBarnehageplassVilkår2024(vilkårresultater)

        // Assert
        assertThat(forskyvBarnehageplassVilkår2024).hasSize(2)

        assertThat(forskyvBarnehageplassVilkår2024[0].verdi).isEqualTo(vilkårResultat1)
        assertThat(forskyvBarnehageplassVilkår2024[0].fom).isNull()
        assertThat(forskyvBarnehageplassVilkår2024[0].tom).isEqualTo(LocalDate.of(2024, 9, 30))

        assertThat(forskyvBarnehageplassVilkår2024[1].verdi).isEqualTo(vilkårResultat2)
        assertThat(forskyvBarnehageplassVilkår2024[1].fom).isEqualTo(LocalDate.of(2024, 10, 1))
        assertThat(forskyvBarnehageplassVilkår2024[1].tom).isNull()
    }

    @Test
    fun `når man går fra ingen barnehageplass til full barnehageplass skal reduksjon til full barnehageplass skje i neste måned`() {
        // Arrange
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                antallTimer = BigDecimal(0),
                periodeFom = null,
                periodeTom = LocalDate.of(2024, 9, 15),
            )

        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                antallTimer = BigDecimal(40),
                periodeFom = LocalDate.of(2024, 9, 16),
                periodeTom = null,
            )

        val vilkårresultater =
            listOf(
                vilkårResultat1,
                vilkårResultat2,
            )

        // Act
        val forskyvBarnehageplassVilkår2024 = forskyvBarnehageplassVilkår2024(vilkårresultater)

        // Assert
        assertThat(forskyvBarnehageplassVilkår2024).hasSize(2)

        assertThat(forskyvBarnehageplassVilkår2024[0].verdi).isEqualTo(vilkårResultat1)
        assertThat(forskyvBarnehageplassVilkår2024[0].fom).isNull()
        assertThat(forskyvBarnehageplassVilkår2024[0].tom).isEqualTo(LocalDate.of(2024, 9, 30))

        assertThat(forskyvBarnehageplassVilkår2024[1].verdi).isEqualTo(vilkårResultat2)
        assertThat(forskyvBarnehageplassVilkår2024[1].fom).isEqualTo(LocalDate.of(2024, 10, 1))
        assertThat(forskyvBarnehageplassVilkår2024[1].tom).isNull()
    }

    @Test
    fun `når man går fra gradert barnehageplass til full barnehageplass skal reduksjon til full barnehageplass skje i neste måned`() {
        // Arrange
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                antallTimer = BigDecimal(20),
                periodeFom = null,
                periodeTom = LocalDate.of(2024, 9, 15),
            )

        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                antallTimer = BigDecimal(40),
                periodeFom = LocalDate.of(2024, 9, 16),
                periodeTom = null,
            )

        val vilkårresultater =
            listOf(
                vilkårResultat1,
                vilkårResultat2,
            )

        // Act
        val forskyvBarnehageplassVilkår2024 = forskyvBarnehageplassVilkår2024(vilkårresultater)

        // Assert
        assertThat(forskyvBarnehageplassVilkår2024).hasSize(2)

        assertThat(forskyvBarnehageplassVilkår2024[0].verdi).isEqualTo(vilkårResultat1)
        assertThat(forskyvBarnehageplassVilkår2024[0].fom).isNull()
        assertThat(forskyvBarnehageplassVilkår2024[0].tom).isEqualTo(LocalDate.of(2024, 9, 30))

        assertThat(forskyvBarnehageplassVilkår2024[1].verdi).isEqualTo(vilkårResultat2)
        assertThat(forskyvBarnehageplassVilkår2024[1].fom).isEqualTo(LocalDate.of(2024, 10, 1))
        assertThat(forskyvBarnehageplassVilkår2024[1].tom).isNull()
    }
}
