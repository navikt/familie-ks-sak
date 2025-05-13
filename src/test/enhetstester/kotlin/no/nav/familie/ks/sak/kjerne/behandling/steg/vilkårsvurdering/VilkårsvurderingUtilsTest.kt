package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import io.mockk.mockk
import no.nav.familie.ks.sak.api.dto.VilkårResultatDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårsvurderingOppfylt
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

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
        personResultaterBarn1.vilkårResultater.forEach { assertEquals(fødselsDatoBarn1.plusMonths(19), it.periodeTom) }

        val personResultaterBarn2 = vilkårsvurdering.personResultater.single { it.aktør.aktivFødselsnummer() == personIdentBarn2 }
        personResultaterBarn2.vilkårResultater.forEach { assertEquals(dødsdatoBarn2, it.periodeTom) }
    }

    @Test
    fun `forkortHvisSkalForkortesEtterRegelverkEndring forkorter hvis periode krysser lovendringsdato`() {
        val vilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = LocalDate.of(2024, 5, 1),
                periodeTom = LocalDate.of(2025, 5, 1),
                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
            )

        val resultat = listOf(vilkårResultat).forkortTomTilGyldigLengde()

        assertEquals(LocalDate.of(2024, 12, 1), resultat.first { it.vilkårType == Vilkår.BARNETS_ALDER }.periodeTom)
    }

    @Test
    fun `forkortHvisSkalForkortesEtterRegelverkEndring forkorter ikke hvis periode ikke krysser lovendringsdato`() {
        val vilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = LocalDate.of(2023, 7, 1),
                periodeTom = LocalDate.of(2024, 7, 1),
                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
            )

        val resultat = listOf(vilkårResultat).forkortTomTilGyldigLengde()

        assertEquals(LocalDate.of(2024, 7, 1), resultat.first { it.vilkårType == Vilkår.BARNETS_ALDER }.periodeTom)
    }

    @Test
    fun `forkortHvisSkalForkortesEtterRegelverkEndring forkorter hvis periode er laget etter nytt lovverk og er lengre enn 7 mnd`() {
        val vilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = LocalDate.of(2024, 8, 1),
                periodeTom = LocalDate.of(2025, 7, 1),
                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
            )

        val resultat = listOf(vilkårResultat).forkortTomTilGyldigLengde()

        assertEquals(LocalDate.of(2025, 3, 1), resultat.first { it.vilkårType == Vilkår.BARNETS_ALDER }.periodeTom)
    }

    @Test
    fun `forkortHvisSkalForkortesEtterRegelverkEndring forkorter til lovendringsdato hvis den forkortes under 7 mnder`() {
        val vilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = LocalDate.of(2023, 10, 15),
                periodeTom = LocalDate.of(2024, 9, 15),
                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
            )

        val resultat = listOf(vilkårResultat).forkortTomTilGyldigLengde()

        assertEquals(LocalDate.of(2024, 7, 31), resultat.first { it.vilkårType == Vilkår.BARNETS_ALDER }.periodeTom)
    }

    @Test
    fun `endreVilkårResultat - Skal kastes funksjonell feil hvis det forsøkes å lage en oppfylt periode mens det finnes en avslagsperiode uten periode`() {
        val person = mockk<PersonResultat>(relaxed = true)

        val eksisterendeVilkårResultat =
            listOf(
                lagVilkårResultat(
                    id = 1,
                    personResultat = person,
                    behandlingId = 1,
                    periodeFom = null,
                    periodeTom = null,
                    resultat = Resultat.IKKE_OPPFYLT,
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    erEksplisittAvslagPåSøknad = true,
                ),
                lagVilkårResultat(
                    id = 2,
                    personResultat = person,
                    behandlingId = 1,
                    periodeFom = null,
                    periodeTom = null,
                    resultat = Resultat.IKKE_VURDERT,
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                ),
            )

        val vilkårDto =
            VilkårResultatDto(
                id = 2,
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = januar,
                periodeTom = desember,
                resultat = Resultat.OPPFYLT,
                endretTidspunkt = LocalDateTime.now(),
                behandlingId = 1,
                begrunnelse = "test",
                endretAv = "VL",
            )

        val frontendFeilmelding =
            assertThrows<FunksjonellFeil> {
                endreVilkårResultat(eksisterendeVilkårResultat, vilkårDto)
            }.frontendFeilmelding

        assertThat(frontendFeilmelding).isEqualTo("Du kan ikke legge til perioden fordi det er vurdert avslag uten datoer på vilkåret. Denne må fjernes først.")
    }

    @Test
    fun `endreVilkårResultat - Skal kastes funksjonell feil hvis det forsøkes å lage en avslagsperiode uten dato mens det finnes en oppfylt periode`() {
        val person = mockk<PersonResultat>(relaxed = true)

        val eksisterendeVilkårResultat =
            listOf(
                lagVilkårResultat(
                    id = 1,
                    personResultat = person,
                    behandlingId = 1,
                    periodeFom = januar,
                    periodeTom = desember,
                    resultat = Resultat.OPPFYLT,
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                ),
                lagVilkårResultat(
                    id = 2,
                    personResultat = person,
                    behandlingId = 1,
                    periodeFom = null,
                    periodeTom = null,
                    resultat = Resultat.IKKE_VURDERT,
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                ),
            )

        val vilkårDto =
            VilkårResultatDto(
                id = 2,
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = null,
                periodeTom = null,
                resultat = Resultat.IKKE_OPPFYLT,
                endretTidspunkt = LocalDateTime.now(),
                behandlingId = 1,
                begrunnelse = "test",
                endretAv = "VL",
                erEksplisittAvslagPåSøknad = true,
            )

        val frontendFeilmelding =
            assertThrows<FunksjonellFeil> {
                endreVilkårResultat(eksisterendeVilkårResultat, vilkårDto)
            }.frontendFeilmelding

        assertThat(frontendFeilmelding).isEqualTo("Du kan ikke legge til avslagperiode uten datoer fordi det finnes oppfylte perioder på vilkåret. Disse må fjernes først.")
    }
}
