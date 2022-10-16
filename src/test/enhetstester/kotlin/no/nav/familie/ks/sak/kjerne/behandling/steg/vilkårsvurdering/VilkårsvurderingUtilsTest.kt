package no.nav.familie.ks.sak.kjerne.vilkårsvurdering

import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårsvurderingUtilsTest {

    private val januar = LocalDate.of(2022, 1, 1)
    private val april = LocalDate.of(2022, 4, 1)
    private val august = LocalDate.of(2022, 8, 1)
    private val desember = LocalDate.of(2022, 12, 1)

    @Test
    fun `tilpassVilkårForEndretVilkår - skal splitte gammelt vilkår og oppdatere behandling ved ny periode i midten`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                id = 50,
                personResultat = mockk(relaxed = true),
                behandlingId = 1,
                periodeFom = januar,
                periodeTom = desember.minusDays(1)
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                id = 51,
                personResultat = mockk(relaxed = true),
                behandlingId = 2,
                periodeFom = april,
                periodeTom = august.minusDays(1)
            )

        val resultat = tilpassVilkårForEndretVilkår(
            endretVilkårResultatId = vilkårResultat2.id,
            eksisterendeVilkårResultat = vilkårResultat1,
            endretVilkårResultat = vilkårResultat2
        )

        Assertions.assertEquals(januar, resultat[0].periodeFom)
        Assertions.assertEquals(april.minusDays(1), resultat[0].periodeTom)
        Assertions.assertEquals(august, resultat[1].periodeFom)
        Assertions.assertEquals(desember.minusDays(1), resultat[1].periodeTom)

        Assertions.assertEquals(2, resultat[0].behandlingId)
        Assertions.assertEquals(2, resultat[1].behandlingId)

        Assertions.assertEquals(0, resultat[0].id)
        Assertions.assertEquals(0, resultat[1].id)
    }

    @Test
    fun `tilpassVilkårForEndretVilkår - skal forskyve gammelt vilkår og oppdatere behandling ved ny periode i slutten`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                id = 50,
                personResultat = mockk(relaxed = true),
                behandlingId = 1,
                periodeFom = januar,
                periodeTom = desember.minusDays(1)
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                id = 51,
                personResultat = mockk(relaxed = true),
                behandlingId = 2,
                periodeFom = april,
                periodeTom = desember.minusDays(1)
            )

        val resultat = tilpassVilkårForEndretVilkår(
            endretVilkårResultatId = vilkårResultat2.id,
            eksisterendeVilkårResultat = vilkårResultat1,
            endretVilkårResultat = vilkårResultat2
        )

        Assertions.assertEquals(1, resultat.size)

        Assertions.assertEquals(januar, resultat[0].periodeFom)
        Assertions.assertEquals(april.minusDays(1), resultat[0].periodeTom)

        Assertions.assertEquals(2, resultat[0].behandlingId)

        Assertions.assertEquals(0, resultat[0].id)
    }

    @Test
    fun `tilpassVilkårForEndretVilkår - skal forskyve gammelt vilkår og oppdatere behandling ved ny periode i starten`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                id = 50,
                personResultat = mockk(relaxed = true),
                behandlingId = 1,
                periodeFom = januar,
                periodeTom = desember.minusDays(1)
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                id = 51,
                personResultat = mockk(relaxed = true),
                behandlingId = 2,
                periodeFom = januar,
                periodeTom = august.minusDays(1)
            )

        val resultat = tilpassVilkårForEndretVilkår(
            endretVilkårResultatId = vilkårResultat2.id,
            eksisterendeVilkårResultat = vilkårResultat1,
            endretVilkårResultat = vilkårResultat2
        )

        Assertions.assertEquals(1, resultat.size)

        Assertions.assertEquals(august, resultat[0].periodeFom)
        Assertions.assertEquals(desember.minusDays(1), resultat[0].periodeTom)

        Assertions.assertEquals(2, resultat[0].behandlingId)

        Assertions.assertEquals(0, resultat[0].id)
    }

    @Test
    fun `tilpassVilkårForEndretVilkår - skal ikke endre gamle vilkår som ikke blir overlappet`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                id = 50,
                personResultat = mockk(relaxed = true),
                behandlingId = 1,
                periodeFom = januar,
                periodeTom = april.minusDays(1)
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                id = 51,
                personResultat = mockk(relaxed = true),
                behandlingId = 2,
                periodeFom = august,
                periodeTom = desember.minusDays(1)
            )

        val resultat = tilpassVilkårForEndretVilkår(
            endretVilkårResultatId = vilkårResultat2.id,
            eksisterendeVilkårResultat = vilkårResultat1,
            endretVilkårResultat = vilkårResultat2
        )

        Assertions.assertEquals(1, resultat.size)

        Assertions.assertEquals(januar, resultat[0].periodeFom)
        Assertions.assertEquals(april.minusDays(1), resultat[0].periodeTom)

        Assertions.assertEquals(1, resultat[0].behandlingId)

        Assertions.assertEquals(50, resultat[0].id)
    }

    @Test
    fun `tilpassVilkårForEndretVilkår - skal fjerne gammelt vilkårresultat som blir helt overlappet`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                id = 50,
                personResultat = mockk(relaxed = true),
                behandlingId = 1,
                periodeFom = april,
                periodeTom = august.minusDays(1)
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                id = 51,
                personResultat = mockk(relaxed = true),
                behandlingId = 2,
                periodeFom = januar,
                periodeTom = desember.minusDays(1)
            )

        val resultat = tilpassVilkårForEndretVilkår(
            endretVilkårResultatId = vilkårResultat2.id,
            eksisterendeVilkårResultat = vilkårResultat1,
            endretVilkårResultat = vilkårResultat2
        )

        Assertions.assertEquals(0, resultat.size)
    }

    @Test
    fun `tilpassVilkårForEndretVilkår - skal bytte ut gamle vilkår som har lik id som det nye vilkåret`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                id = 50,
                personResultat = mockk(relaxed = true),
                behandlingId = 1,
                periodeFom = januar,
                periodeTom = april.minusDays(1)
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                id = 50,
                personResultat = mockk(relaxed = true),
                behandlingId = 2,
                periodeFom = august,
                periodeTom = desember.minusDays(1)
            )

        val resultat = tilpassVilkårForEndretVilkår(
            endretVilkårResultatId = vilkårResultat2.id,
            eksisterendeVilkårResultat = vilkårResultat1,
            endretVilkårResultat = vilkårResultat2
        )

        Assertions.assertEquals(1, resultat.size)

        Assertions.assertEquals(august, resultat[0].periodeFom)
        Assertions.assertEquals(desember.minusDays(1), resultat[0].periodeTom)

        Assertions.assertEquals(2, resultat[0].behandlingId)

        Assertions.assertEquals(50, resultat[0].id)
    }

    @Test
    fun `tilpassVilkårForEndretVilkår - skal ikke endre gamle vilkår som når det nye er av en annen type`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                id = 50,
                personResultat = mockk(relaxed = true),
                behandlingId = 1,
                periodeFom = januar,
                periodeTom = august.minusDays(1),
                vilkårType = Vilkår.BOR_MED_SØKER
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                id = 51,
                personResultat = mockk(relaxed = true),
                behandlingId = 2,
                periodeFom = april,
                periodeTom = desember.minusDays(1),
                vilkårType = Vilkår.BOSATT_I_RIKET
            )

        val resultat = tilpassVilkårForEndretVilkår(
            endretVilkårResultatId = vilkårResultat2.id,
            eksisterendeVilkårResultat = vilkårResultat1,
            endretVilkårResultat = vilkårResultat2
        )

        Assertions.assertEquals(1, resultat.size)

        Assertions.assertEquals(januar, resultat[0].periodeFom)
        Assertions.assertEquals(august.minusDays(1), resultat[0].periodeTom)

        Assertions.assertEquals(1, resultat[0].behandlingId)

        Assertions.assertEquals(50, resultat[0].id)
    }
}
