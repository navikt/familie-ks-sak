package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import io.mockk.mockk
import java.time.LocalDate
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårsvurderingOppfylt
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VilkårsvurderingUtilsTest {
    private val januar = LocalDate.of(2022, 1, 1)
    private val april = LocalDate.of(2022, 4, 1)
    private val august = LocalDate.of(2022, 8, 1)
    private val desember = LocalDate.of(2022, 12, 1)

    private val søker = randomAktør()
    private val barn1 = randomAktør()

    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

    @Test
    fun `tilpassVilkårForEndretVilkår - skal splitte gammelt vilkår og oppdatere behandling ved ny periode i midten`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                id = 50,
                personResultat = mockk(relaxed = true),
                behandlingId = 1,
                periodeFom = januar,
                periodeTom = desember.minusDays(1),
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                id = 51,
                personResultat = mockk(relaxed = true),
                behandlingId = 2,
                periodeFom = april,
                periodeTom = august.minusDays(1),
            )

        val resultat =
            tilpassVilkårForEndretVilkår(
                endretVilkårResultatId = vilkårResultat2.id,
                eksisterendeVilkårResultat = vilkårResultat1,
                endretVilkårResultat = vilkårResultat2,
            )

        assertEquals(januar, resultat[0].periodeFom)
        assertEquals(april.minusDays(1), resultat[0].periodeTom)
        assertEquals(august, resultat[1].periodeFom)
        assertEquals(desember.minusDays(1), resultat[1].periodeTom)

        assertEquals(2, resultat[0].behandlingId)
        assertEquals(2, resultat[1].behandlingId)

        assertEquals(0, resultat[0].id)
        assertEquals(0, resultat[1].id)
    }

    @Test
    fun `tilpassVilkårForEndretVilkår - skal forskyve gammelt vilkår og oppdatere behandling ved ny periode i slutten`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                id = 50,
                personResultat = mockk(relaxed = true),
                behandlingId = 1,
                periodeFom = januar,
                periodeTom = desember.minusDays(1),
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                id = 51,
                personResultat = mockk(relaxed = true),
                behandlingId = 2,
                periodeFom = april,
                periodeTom = desember.minusDays(1),
            )

        val resultat =
            tilpassVilkårForEndretVilkår(
                endretVilkårResultatId = vilkårResultat2.id,
                eksisterendeVilkårResultat = vilkårResultat1,
                endretVilkårResultat = vilkårResultat2,
            )

        assertEquals(1, resultat.size)

        assertEquals(januar, resultat[0].periodeFom)
        assertEquals(april.minusDays(1), resultat[0].periodeTom)

        assertEquals(2, resultat[0].behandlingId)

        assertEquals(0, resultat[0].id)
    }

    @Test
    fun `tilpassVilkårForEndretVilkår - skal forskyve gammelt vilkår og oppdatere behandling ved ny periode i starten`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                id = 50,
                personResultat = mockk(relaxed = true),
                behandlingId = 1,
                periodeFom = januar,
                periodeTom = desember.minusDays(1),
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                id = 51,
                personResultat = mockk(relaxed = true),
                behandlingId = 2,
                periodeFom = januar,
                periodeTom = august.minusDays(1),
            )

        val resultat =
            tilpassVilkårForEndretVilkår(
                endretVilkårResultatId = vilkårResultat2.id,
                eksisterendeVilkårResultat = vilkårResultat1,
                endretVilkårResultat = vilkårResultat2,
            )

        assertEquals(1, resultat.size)

        assertEquals(august, resultat[0].periodeFom)
        assertEquals(desember.minusDays(1), resultat[0].periodeTom)

        assertEquals(2, resultat[0].behandlingId)

        assertEquals(0, resultat[0].id)
    }

    @Test
    fun `tilpassVilkårForEndretVilkår - skal ikke endre gamle vilkår som ikke blir overlappet`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                id = 50,
                personResultat = mockk(relaxed = true),
                behandlingId = 1,
                periodeFom = januar,
                periodeTom = april.minusDays(1),
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                id = 51,
                personResultat = mockk(relaxed = true),
                behandlingId = 2,
                periodeFom = august,
                periodeTom = desember.minusDays(1),
            )

        val resultat =
            tilpassVilkårForEndretVilkår(
                endretVilkårResultatId = vilkårResultat2.id,
                eksisterendeVilkårResultat = vilkårResultat1,
                endretVilkårResultat = vilkårResultat2,
            )

        assertEquals(1, resultat.size)

        assertEquals(januar, resultat[0].periodeFom)
        assertEquals(april.minusDays(1), resultat[0].periodeTom)

        assertEquals(1, resultat[0].behandlingId)

        assertEquals(50, resultat[0].id)
    }

    @Test
    fun `tilpassVilkårForEndretVilkår - skal fjerne gammelt vilkårresultat som blir helt overlappet`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                id = 50,
                personResultat = mockk(relaxed = true),
                behandlingId = 1,
                periodeFom = april,
                periodeTom = august.minusDays(1),
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                id = 51,
                personResultat = mockk(relaxed = true),
                behandlingId = 2,
                periodeFom = januar,
                periodeTom = desember.minusDays(1),
            )

        val resultat =
            tilpassVilkårForEndretVilkår(
                endretVilkårResultatId = vilkårResultat2.id,
                eksisterendeVilkårResultat = vilkårResultat1,
                endretVilkårResultat = vilkårResultat2,
            )

        assertEquals(0, resultat.size)
    }

    @Test
    fun `tilpassVilkårForEndretVilkår - skal bytte ut gamle vilkår som har lik id som det nye vilkåret`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                id = 50,
                personResultat = mockk(relaxed = true),
                behandlingId = 1,
                periodeFom = januar,
                periodeTom = april.minusDays(1),
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                id = 50,
                personResultat = mockk(relaxed = true),
                behandlingId = 2,
                periodeFom = august,
                periodeTom = desember.minusDays(1),
            )

        val resultat =
            tilpassVilkårForEndretVilkår(
                endretVilkårResultatId = vilkårResultat2.id,
                eksisterendeVilkårResultat = vilkårResultat1,
                endretVilkårResultat = vilkårResultat2,
            )

        assertEquals(1, resultat.size)

        assertEquals(august, resultat[0].periodeFom)
        assertEquals(desember.minusDays(1), resultat[0].periodeTom)

        assertEquals(2, resultat[0].behandlingId)

        assertEquals(50, resultat[0].id)
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
                vilkårType = Vilkår.BOR_MED_SØKER,
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                id = 51,
                personResultat = mockk(relaxed = true),
                behandlingId = 2,
                periodeFom = april,
                periodeTom = desember.minusDays(1),
                vilkårType = Vilkår.BOSATT_I_RIKET,
            )

        val resultat =
            tilpassVilkårForEndretVilkår(
                endretVilkårResultatId = vilkårResultat2.id,
                eksisterendeVilkårResultat = vilkårResultat1,
                endretVilkårResultat = vilkårResultat2,
            )

        assertEquals(1, resultat.size)

        assertEquals(januar, resultat[0].periodeFom)
        assertEquals(august.minusDays(1), resultat[0].periodeTom)

        assertEquals(1, resultat[0].behandlingId)

        assertEquals(50, resultat[0].id)
    }

    @Test
    fun `oppdaterMedDødsdatoer skal oppdatere vikårsvurdering slik at den avsluttes ved dødsdato dersom den starter før dødsdato`() {
        val søkerPersonIdent = randomFnr()
        val personIdentBarn1 = randomFnr()
        val personIdentBarn2 = randomFnr()

        val fødselsDatoBarn1 = LocalDate.of(2022, 2, 2)
        val fødselsDatoBarn2 = LocalDate.of(2022, 2, 2)

        val dødsdatoBarn1 = LocalDate.of(2023, 1, 2)
        val dødsdatoBarn2 = LocalDate.of(2023, 8, 2)

        val persongrunnlag =
            lagPersonopplysningGrunnlag(
                søkerPersonIdent = søkerPersonIdent,
                barnasIdenter = listOf(personIdentBarn1, personIdentBarn2),
                barnasFødselsdatoer = listOf(fødselsDatoBarn1, fødselsDatoBarn2),
                barnasDødsfallDatoer = listOf(dødsdatoBarn1, dødsdatoBarn2),
            )

        val vilkårsvurdering = lagVilkårsvurderingOppfylt(personer = persongrunnlag.personer)

        vilkårsvurdering.oppdaterMedDødsdatoer(persongrunnlag)

        // Siden barnet dør før vilkårResulatatene starter skal vi ikke gjøre noe med dem
        val personResultaterBarn1 = vilkårsvurdering.personResultater.single { it.aktør.aktivFødselsnummer() == personIdentBarn1 }
        personResultaterBarn1.vilkårResultater.forEach { assertEquals(fødselsDatoBarn1.plusYears(2), it.periodeTom) }

        val personResultaterBarn2 = vilkårsvurdering.personResultater.single { it.aktør.aktivFødselsnummer() == personIdentBarn2 }
        personResultaterBarn2.vilkårResultater.forEach { assertEquals(dødsdatoBarn2, it.periodeTom) }
    }
}
