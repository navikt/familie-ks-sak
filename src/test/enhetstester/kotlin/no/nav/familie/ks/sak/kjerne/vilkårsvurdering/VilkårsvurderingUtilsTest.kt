package no.nav.familie.ks.sak.kjerne.vilkårsvurdering

import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagVilkårResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårsvurderingUtilsTest {

    @Test
    fun `tilpassVilkårForEndretVilkår - skal splitte gammelt vilkår og oppdatere behandling`() {
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

        Assertions.assertEquals(4, resultat.size)

        Assertions.assertEquals(LocalDate.now().minusMonths(3), resultat[0].periodeFom)
        Assertions.assertEquals(LocalDate.now().minusMonths(2), resultat[0].periodeTom)
        Assertions.assertEquals(LocalDate.now().minusMonths(2), resultat[1].periodeFom)
        Assertions.assertEquals(LocalDate.now().minusMonths(1), resultat[1].periodeTom)
        Assertions.assertEquals(LocalDate.now().minusMonths(1), resultat[2].periodeFom)
        Assertions.assertEquals(LocalDate.now().minusMonths(0), resultat[2].periodeTom)

        Assertions.assertEquals(2, resultat[0].behandlingId)
        Assertions.assertEquals(2, resultat[0].behandlingId)
        Assertions.assertEquals(2, resultat[0].behandlingId)
    }
}
