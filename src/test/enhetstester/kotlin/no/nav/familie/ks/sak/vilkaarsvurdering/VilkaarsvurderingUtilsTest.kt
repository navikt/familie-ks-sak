package no.nav.familie.ks.sak.vilkaarsvurdering

import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.tilpassVilkårForEndretVilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkaarsvurderingUtilsTest {

    @Test
    fun `hmm`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                id = 0,
                personResultat = mockk(relaxed = true),
                behandlingId = 1,
                periodeFom = LocalDate.now().minusMonths(3),
                periodeTom = LocalDate.now()
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                id = 1,
                personResultat = mockk(relaxed = true),
                behandlingId = 2,
                periodeFom = LocalDate.now().minusMonths(2),
                periodeTom = LocalDate.now().minusMonths(1)
            )

        val resultat = tilpassVilkårForEndretVilkår(
            eksisterendeVilkårResultat = vilkårResultat1,
            endretVilkårResultat = vilkårResultat2
        )

        Assertions.assertEquals(3, resultat.size)
        Assertions.assertEquals(LocalDate.now().minusMonths(3), resultat[0].periodeFom)
    }
}
