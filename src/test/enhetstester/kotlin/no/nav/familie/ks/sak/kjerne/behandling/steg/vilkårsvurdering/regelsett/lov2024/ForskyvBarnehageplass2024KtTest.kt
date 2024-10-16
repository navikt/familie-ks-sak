package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024

import java.math.BigDecimal
import java.time.LocalDate
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ForskyvBarnehageplass2024KtTest {
    @Test
    fun `asdf`() {
        // Arrange
        val vilkårresultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    antallTimer = BigDecimal(0),
                    periodeFom = LocalDate.of(2024, 8, 1),
                    periodeTom = LocalDate.of(2024, 9, 15),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    antallTimer = BigDecimal(15),
                    periodeFom = LocalDate.of(2024, 9, 16),
                    periodeTom = LocalDate.of(2024, 10, 16),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    antallTimer = BigDecimal(30),
                    periodeFom = LocalDate.of(2024, 10, 17),
                    periodeTom = LocalDate.of(2024, 10, 31),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    antallTimer = BigDecimal(0),
                    periodeFom = LocalDate.of(2024, 11, 1),
                    periodeTom = null,
                ),
            )

        // Act
        val forskyvBarnehageplassVilkår2024 = vilkårresultater.forskyvBarnehageplassVilkår2024()

        // Assert
        assertThat(forskyvBarnehageplassVilkår2024).hasSize(4)

        assertThat(forskyvBarnehageplassVilkår2024[0].fom).isEqualTo(LocalDate.of(2024, 8, 1))
        assertThat(forskyvBarnehageplassVilkår2024[0].tom).isEqualTo(LocalDate.of(2024, 8, 31))

        assertThat(forskyvBarnehageplassVilkår2024[1].fom).isEqualTo(LocalDate.of(2024, 9, 1))
        assertThat(forskyvBarnehageplassVilkår2024[1].tom).isEqualTo(LocalDate.of(2024, 9, 30))

        assertThat(forskyvBarnehageplassVilkår2024[2].fom).isEqualTo(LocalDate.of(2024, 10, 1))
        assertThat(forskyvBarnehageplassVilkår2024[2].tom).isEqualTo(LocalDate.of(2024, 10, 31))

        assertThat(forskyvBarnehageplassVilkår2024[3].fom).isEqualTo(LocalDate.of(2024, 11, 1))
        assertThat(forskyvBarnehageplassVilkår2024[3].tom).isNull()
    }
}
