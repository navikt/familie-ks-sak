package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StandardVilkårForskyverKtTest {
    @Test
    fun `skal kaste exception om man prøver å forskyve barnehageplass`() {
        // Arrange
        val alleVilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                ),
            )

        // Act & assert
        val exception = assertThrows<IllegalArgumentException> {
            forskyvStandardVilkår2024(
                Vilkår.BARNEHAGEPLASS,
                alleVilkårResultater,
            )
        }

        // Assert
        assertThat(exception.message).isEqualTo("BARNEHAGEPLASS skal ikke behandles etter standard logikk")
    }
}
