package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lov2025

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.forskyvEtterLovgivning2025
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class ForskyvVilkår2025Test {
    @Test
    fun `skal forskyve barnehageplass vilkår`() {
        // Arrange
        val vilkårResultat =
            lagVilkårResultat(
                periodeFom = LocalDate.of(2025, 1, 1),
                periodeTom = LocalDate.of(2025, 12, 31),
                vilkårType = Vilkår.BARNEHAGEPLASS,
            )

        val vilkårResultater = listOf(vilkårResultat)

        // Act
        val forskjøvetVilkårResultat = forskyvEtterLovgivning2025(Vilkår.BARNEHAGEPLASS, vilkårResultater)

        // Assert
        assertThat(forskjøvetVilkårResultat).hasSize(1)
        assertThat(forskjøvetVilkårResultat[0].fom).isEqualTo(LocalDate.of(2025, 2, 1))
        assertThat(forskjøvetVilkårResultat[0].tom).isEqualTo(LocalDate.of(2025, 12, 31))
        assertThat(forskjøvetVilkårResultat[0].verdi).isEqualTo(vilkårResultat)
    }

    @ParameterizedTest
    @EnumSource(
        value = Vilkår::class,
        names = ["BARNEHAGEPLASS"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `skal forskyve standard vilkår`(vilkår: Vilkår) {
        // Arrange
        val vilkårResultat =
            lagVilkårResultat(
                periodeFom = LocalDate.of(2025, 1, 1),
                periodeTom = LocalDate.of(2025, 6, 30),
                vilkårType = vilkår,
            )

        val vilkårResultater = listOf(vilkårResultat)

        // Act
        val forskjøvetVilkårResultat = forskyvEtterLovgivning2025(vilkår, vilkårResultater)

        // Assert
        assertThat(forskjøvetVilkårResultat).hasSize(1)
        assertThat(forskjøvetVilkårResultat[0].fom).isEqualTo(LocalDate.of(2025, 2, 1))
        assertThat(forskjøvetVilkårResultat[0].tom).isEqualTo(LocalDate.of(2025, 5, 31))
        assertThat(forskjøvetVilkårResultat[0].verdi).isEqualTo(vilkårResultat)
    }
}
