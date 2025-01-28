package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lov2025.standard

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.TilknyttetVilkårResultater
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.standard.forskyvTom
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TomForskyverTest {
    @Test
    fun `skal returnere null når gjeldene vilkårresultat tom dato er null`() {
        // Arrange
        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                gjeldende =
                    lagVilkårResultat(
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        periodeFom = LocalDate.of(2025, 1, 31),
                        periodeTom = null,
                    ),
                neste = null,
            )

        // Act
        val forskjøvetTom = forskyvTom(tilknyttetVilkårResultater)

        // Assert
        assertThat(forskjøvetTom).isNull()
    }

    @Test
    fun `skal returnere siste dag i måneden når gjeldende vilkårresultat slutter dagen først neste vilkårresultat begynner og gjeldende slutter på siste dag i måneden`() {
        // Arrange
        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                gjeldende =
                    lagVilkårResultat(
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        periodeFom = LocalDate.of(2025, 1, 31),
                        periodeTom = LocalDate.of(2025, 2, 1),
                    ),
                neste =
                    lagVilkårResultat(
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        periodeFom = LocalDate.of(2024, 11, 1),
                        periodeTom = LocalDate.of(2024, 12, 31),
                    ),
            )

        // Act
        val forskjøvetTom = forskyvTom(tilknyttetVilkårResultater)

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2025, 1, 31))
    }

    @Test
    fun `skal returnere siste dag i forrige måneden når gjeldende vilkårresultat slutter dagen først neste vilkårresultat begynner og gjeldende slutter på første dag i måneden`() {
        // Arrange
        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                gjeldende =
                    lagVilkårResultat(
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        periodeFom = LocalDate.of(2024, 3, 1),
                        periodeTom = LocalDate.of(2025, 1, 1),
                    ),
                neste =
                    lagVilkårResultat(
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        periodeFom = LocalDate.of(2025, 1, 2),
                        periodeTom = LocalDate.of(2024, 12, 31),
                    ),
            )

        // Act
        val forskjøvetTom = forskyvTom(tilknyttetVilkårResultater)

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2025, 1, 31))
    }
}
