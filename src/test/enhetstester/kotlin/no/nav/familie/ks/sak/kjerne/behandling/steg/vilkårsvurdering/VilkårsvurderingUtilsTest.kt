package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import io.mockk.mockk
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårResultaterForBarn
import no.nav.familie.ks.sak.data.lagVilkårsvurderingMedSøkersVilkår
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Month

class VilkårsvurderingUtilsTest {

    private val januar = LocalDate.of(2022, 1, 1)
    private val april = LocalDate.of(2022, 4, 1)
    private val august = LocalDate.of(2022, 8, 1)
    private val desember = LocalDate.of(2022, 12, 1)

    private val søker = randomAktør()
    private val barn1 = randomAktør()

    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
        behandlingId = behandling.id,
        søkerPersonIdent = søker.aktivFødselsnummer(),
        barnasIdenter = listOf(barn1.aktivFødselsnummer()),
        søkerAktør = søker,
        barnAktør = listOf(barn1)
    )
    private val søkerPerson = lagPerson(personopplysningGrunnlag, søker, PersonType.SØKER)
    private val barnPerson = lagPerson(personopplysningGrunnlag, barn1, PersonType.BARN)
    private val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

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

        assertEquals(1, resultat.size)

        assertEquals(januar, resultat[0].periodeFom)
        assertEquals(august.minusDays(1), resultat[0].periodeTom)

        assertEquals(1, resultat[0].behandlingId)

        assertEquals(50, resultat[0].id)
    }

    @Test
    fun `validerBarnasVilkår skal kaste feil når vilkår resulat er oppfylt men mangler fom`() {
        val fom = null
        val tom = LocalDate.now().plusMonths(7)

        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn = setOf(
            VilkårResultat(
                id = 0,
                personResultat = personResultatForBarn,
                vilkårType = Vilkår.BOR_MED_SØKER,
                resultat = Resultat.OPPFYLT,
                periodeFom = fom,
                periodeTom = tom,
                begrunnelse = "begrunnelse",
                behandlingId = behandling.id
            )
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val exception = assertThrows<Feil> { validerBarnasVilkår(vilkårsvurdering, barna = listOf(barnPerson)) }
        assertEquals(
            "Vilkår ${Vilkår.BOR_MED_SØKER} " +
                "for barn med fødselsdato ${barnPerson.fødselsdato.tilDagMånedÅr()} mangler fom dato.",
            exception.message
        )
    }

    @Test
    fun `validerBarnasVilkår skal kaste feil når vilkår resulat fom er før barnets fødselsdato`() {
        val fom = barnPerson.fødselsdato.minusMonths(2)
        val tom = fom.plusMonths(5)

        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn = setOf(
            VilkårResultat(
                id = 0,
                personResultat = personResultatForBarn,
                vilkårType = Vilkår.BOR_MED_SØKER,
                resultat = Resultat.OPPFYLT,
                periodeFom = fom,
                periodeTom = tom,
                begrunnelse = "begrunnelse",
                behandlingId = behandling.id
            )
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val exception = assertThrows<Feil> { validerBarnasVilkår(vilkårsvurdering, barna = listOf(barnPerson)) }
        assertEquals(
            "Vilkår ${Vilkår.BOR_MED_SØKER} for barn med fødselsdato ${barnPerson.fødselsdato.tilDagMånedÅr()}" +
                " har fom dato før barnets fødselsdato.",
            exception.message
        )
    }

    @Test
    fun `validerBarnasVilkår skal ikke kaste feil når vilkår resulat mangler fom, tom med eksplisitt avslag`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn = setOf(
            VilkårResultat(
                id = 0,
                personResultat = personResultatForBarn,
                vilkårType = Vilkår.BOR_MED_SØKER,
                resultat = Resultat.IKKE_VURDERT,
                periodeFom = null,
                periodeTom = null,
                begrunnelse = "begrunnelse",
                behandlingId = behandling.id,
                erEksplisittAvslagPåSøknad = true
            )
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        assertDoesNotThrow { validerBarnasVilkår(vilkårsvurdering, barna = listOf(barnPerson)) }
    }

    @Test
    fun `validerBarnasVilkår skal kaste feil når MELLOM_1_OG_2_ELLER_ADOPTERT vilkår resulat har fom etter barnets 1 år`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn = setOf(
            VilkårResultat(
                id = 0,
                personResultat = personResultatForBarn,
                vilkårType = Vilkår.MELLOM_1_OG_2_ELLER_ADOPTERT,
                resultat = Resultat.OPPFYLT,
                periodeFom = barnPerson.fødselsdato.plusMonths(13),
                periodeTom = barnPerson.fødselsdato.plusYears(2),
                begrunnelse = "begrunnelse",
                behandlingId = behandling.id
            )
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val exception = assertThrows<Feil> { validerBarnasVilkår(vilkårsvurdering, barna = listOf(barnPerson)) }
        assertEquals("F.o.m datoen må være lik barnets 1 års dag.", exception.message)
    }

    @Test
    fun `validerBarnasVilkår skal kaste feil når MELLOM_1_OG_2_ELLER_ADOPTERT vilkår resulat har tom etter barnets 2 år`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn = setOf(
            VilkårResultat(
                id = 0,
                personResultat = personResultatForBarn,
                vilkårType = Vilkår.MELLOM_1_OG_2_ELLER_ADOPTERT,
                resultat = Resultat.OPPFYLT,
                periodeFom = barnPerson.fødselsdato.plusYears(1),
                periodeTom = barnPerson.fødselsdato.plusYears(2).plusMonths(2),
                begrunnelse = "begrunnelse",
                behandlingId = behandling.id
            )
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val exception = assertThrows<Feil> { validerBarnasVilkår(vilkårsvurdering, barna = listOf(barnPerson)) }
        assertEquals("T.o.m datoen må være lik barnets 2 års dag.", exception.message)
    }

    @Test
    fun `validerBarnasVilkår skal kaste feil når MELLOM_1_OG_2_ELLER_ADOPTERT med adopsjon vilkår resulat har tom etter barnets 6 år`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn = setOf(
            VilkårResultat(
                id = 0,
                personResultat = personResultatForBarn,
                vilkårType = Vilkår.MELLOM_1_OG_2_ELLER_ADOPTERT,
                resultat = Resultat.OPPFYLT,
                periodeFom = barnPerson.fødselsdato.plusYears(3),
                periodeTom = barnPerson.fødselsdato.plusYears(6).withMonth(Month.AUGUST.value).plusMonths(2),
                begrunnelse = "begrunnelse",
                behandlingId = behandling.id,
                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON)
            )
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val exception = assertThrows<Feil> { validerBarnasVilkår(vilkårsvurdering, barna = listOf(barnPerson)) }
        assertEquals("Du kan ikke sette en t.o.m dato som er etter august året barnet fyller 6 år.", exception.message)
    }

    @Test
    fun `validerBarnasVilkår skal kaste feil når MELLOM_1_OG_2_ELLER_ADOPTERT med adopsjon vilkår resulat har diff mellom fom,tom mer enn 1 år`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn = setOf(
            VilkårResultat(
                id = 0,
                personResultat = personResultatForBarn,
                vilkårType = Vilkår.MELLOM_1_OG_2_ELLER_ADOPTERT,
                resultat = Resultat.OPPFYLT,
                periodeFom = barnPerson.fødselsdato.plusYears(3),
                periodeTom = barnPerson.fødselsdato.plusYears(6).withMonth(Month.AUGUST.value),
                begrunnelse = "begrunnelse",
                behandlingId = behandling.id,
                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON)
            )
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val exception = assertThrows<Feil> { validerBarnasVilkår(vilkårsvurdering, barna = listOf(barnPerson)) }
        assertEquals("Differansen mellom f.o.m datoen og t.o.m datoen kan ikke være mer enn 1 år.", exception.message)
    }

    @Test
    fun `validerBarnasVilkår skal ikke kaste feil når MELLOM_1_OG_2_ELLER_ADOPTERT med adopsjon vilkår resulat har tom etter August barnet fyller 6 år`() {
        val tom = barnPerson.fødselsdato.plusYears(6).withMonth(Month.AUGUST.value)
        val fom = tom.minusMonths(5)
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn = setOf(
            VilkårResultat(
                id = 0,
                personResultat = personResultatForBarn,
                vilkårType = Vilkår.MELLOM_1_OG_2_ELLER_ADOPTERT,
                resultat = Resultat.OPPFYLT,
                periodeFom = fom,
                periodeTom = tom,
                begrunnelse = "begrunnelse",
                behandlingId = behandling.id,
                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON)
            )
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        assertDoesNotThrow { validerBarnasVilkår(vilkårsvurdering, barna = listOf(barnPerson)) }
    }

    @Test
    fun `hentInnvilgedePerioder skal hente innvilgede perioder for søker og barna`() {
        val vilkårsvurderingMedSøkersvilkår = lagVilkårsvurderingMedSøkersVilkår(
            søkerAktør = søker,
            behandling = behandling,
            resultat = Resultat.OPPFYLT,
            søkerPeriodeFom = LocalDate.of(1987, 1, 1),
            søkerPeriodeTom = null
        )
        val personResultaterForSøker = vilkårsvurderingMedSøkersvilkår.personResultater
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurderingMedSøkersvilkår, aktør = barn1)
        val barnFødselsdato = barnPerson.fødselsdato
        val barnehagePlassPeriodeMedAntallTimer = Periode(
            fom = barnFødselsdato.plusYears(1),
            tom = barnFødselsdato.plusYears(1).plusMonths(7)
        ) to null // antallTimer null betyr at barn ikke har fått barnehageplass. Da får barn full KS
        val vilkårResultaterForBarn = lagVilkårResultaterForBarn(
            personResultat = personResultatForBarn,
            barnFødselsdato = barnFødselsdato,
            barnehageplassPerioder = listOf(barnehagePlassPeriodeMedAntallTimer),
            behandlingId = behandling.id
        )

        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurderingMedSøkersvilkår.personResultater = personResultaterForSøker + personResultatForBarn

        val result = hentInnvilgedePerioder(personopplysningGrunnlag, vilkårsvurderingMedSøkersvilkår)
        assertNotNull(result)

        val innvilgedePeriodeResultaterSøker = result.first
        assertTrue { innvilgedePeriodeResultaterSøker.isNotEmpty() && innvilgedePeriodeResultaterSøker.size == 1 }
        assertEquals(LocalDate.of(1987, 1, 1), innvilgedePeriodeResultaterSøker.single().periodeFom)
        assertNull(innvilgedePeriodeResultaterSøker.single().periodeTom)

        val innvilgedePeriodeResultaterBarn = result.second
        assertTrue { innvilgedePeriodeResultaterBarn.isNotEmpty() && innvilgedePeriodeResultaterBarn.size == 1 }
        assertEquals(
            barnFødselsdato.plusYears(1).førsteDagIInneværendeMåned(),
            innvilgedePeriodeResultaterBarn.single().periodeFom
        )
        assertEquals(
            barnFødselsdato.plusYears(1).plusMonths(7).sisteDagIMåned(),
            innvilgedePeriodeResultaterBarn.single().periodeTom
        )
    }
}
