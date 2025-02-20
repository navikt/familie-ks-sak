package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering

import mockAdopsjonService
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.dødsfall.Dødsfall
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.Month

class BarnetsVilkårValidatorTest {
    private val søker = randomAktør()
    private val barn1 = randomAktør()
    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val personopplysningGrunnlag =
        lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer(),
            barnasIdenter = listOf(barn1.aktivFødselsnummer()),
            søkerAktør = søker,
            barnAktør = listOf(barn1),
        )
    private val barnPerson = lagPerson(personopplysningGrunnlag, barn1, PersonType.BARN)
    private val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

    private val barnetsAlderVilkårValidator2021 = BarnetsAlderVilkårValidator2021()
    private val barnetsAlderVilkårValidator2024 = BarnetsAlderVilkårValidator2024()
    private val barnetsAlderVilkårValidator2025 = BarnetsAlderVilkårValidator2025()

    private val barnetsVilkårValidator: BarnetsVilkårValidator =
        BarnetsVilkårValidator(
            BarnetsAlderVilkårValidator(
                barnetsAlderVilkårValidator2021,
                barnetsAlderVilkårValidator2024,
                BarnetsAlderVilkårValidator2021og2024(
                    barnetsAlderVilkårValidator2021,
                    barnetsAlderVilkårValidator2024,
                ),
                barnetsAlderVilkårValidator2025,
            ),
            mockAdopsjonService(),
        )

    @Test
    fun `validerAtDatoErKorrektIBarnasVilkår skal kaste funksjonell feil når vilkår resulat er oppfylt men mangler fom`() {
        val fom = null
        val tom = LocalDate.now().plusMonths(7)

        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn =
            setOf(
                VilkårResultat(
                    id = 0,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fom,
                    periodeTom = tom,
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                ),
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val exception =
            org.junit.jupiter.api.assertThrows<FunksjonellFeil> {
                barnetsVilkårValidator.validerAtDatoErKorrektIBarnasVilkår(
                    vilkårsvurdering,
                    barna = listOf(barnPerson),
                )
            }
        assertThat(exception.message).isEqualTo(
            "Vilkår ${Vilkår.BOR_MED_SØKER} " + "for barn med fødselsdato ${barnPerson.fødselsdato.tilDagMånedÅr()} mangler fom dato.",
        )
    }

    @Test
    fun `validerAtDatoErKorrektIBarnasVilkår skal kaste funksjonell feil når vilkår resulat fom er før barnets fødselsdato`() {
        val fom = barnPerson.fødselsdato.minusMonths(2)
        val tom = fom.plusMonths(5)

        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn =
            setOf(
                VilkårResultat(
                    id = 0,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fom,
                    periodeTom = tom,
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                ),
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val exception =
            org.junit.jupiter.api.assertThrows<FunksjonellFeil> {
                barnetsVilkårValidator.validerAtDatoErKorrektIBarnasVilkår(
                    vilkårsvurdering,
                    barna = listOf(barnPerson),
                )
            }
        assertThat(exception.message).isEqualTo(
            "Vilkår ${Vilkår.BOR_MED_SØKER} for barn med fødselsdato ${barnPerson.fødselsdato.tilDagMånedÅr()}" + " har fom dato før barnets fødselsdato.",
        )
    }

    @Test
    fun `validerAtDatoErKorrektIBarnasVilkår skal ikke kaste feil når vilkår resulat mangler fom, tom med eksplisitt avslag`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn =
            setOf(
                VilkårResultat(
                    id = 0,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.IKKE_VURDERT,
                    periodeFom = null,
                    periodeTom = null,
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                    erEksplisittAvslagPåSøknad = true,
                ),
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        assertDoesNotThrow {
            barnetsVilkårValidator.validerAtDatoErKorrektIBarnasVilkår(
                vilkårsvurdering,
                barna = listOf(barnPerson),
            )
        }
    }

    @Test
    fun `validerAtDatoErKorrektIBarnasVilkår skal kaste funksjonell feil når BARNETS_ALDER vilkår resulat har fom etter barnets 1 års dag når barn er født før august 2023`() {
        val barnFødtJuli23 = lagPerson(personType = PersonType.BARN, aktør = randomAktør("30062312345"))
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnFødtJuli23.aktør)
        val vilkårResultaterForBarn =
            setOf(
                VilkårResultat(
                    id = 0,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.BARNETS_ALDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnFødtJuli23.fødselsdato.plusMonths(13),
                    periodeTom = LocalDate.of(2024, 7, 31),
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                ),
                VilkårResultat(
                    id = 0,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.BARNETS_ALDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = DATO_LOVENDRING_2024,
                    periodeTom = barnFødtJuli23.fødselsdato.plusMonths(19),
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                ),
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val exception =
            org.junit.jupiter.api.assertThrows<FunksjonellFeil> {
                barnetsVilkårValidator.validerAtDatoErKorrektIBarnasVilkår(
                    vilkårsvurdering,
                    barna = listOf(barnFødtJuli23),
                )
            }
        assertThat(exception.message).isEqualTo("F.o.m datoen på barnets alder vilkåret må være lik barnets 1 års dag.")
    }

    @Test
    fun `validerAtDatoErKorrektIBarnasVilkår skal kaste funksjonell feil når BARNETS_ALDER vilkår resulat har tom etter barnet fyller 19 måneder for barn født i 2023 og senere`() {
        val barnFødtAugust22 = lagPerson(personType = PersonType.BARN, aktør = randomAktør("01012312345"))
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnFødtAugust22.aktør)
        val vilkårResultaterForBarn =
            setOf(
                VilkårResultat(
                    id = 0,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.BARNETS_ALDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnFødtAugust22.fødselsdato.plusYears(1),
                    periodeTom = DATO_LOVENDRING_2024.minusDays(1),
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                ),
                VilkårResultat(
                    id = 0,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.BARNETS_ALDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = DATO_LOVENDRING_2024,
                    periodeTom = barnFødtAugust22.fødselsdato.plusYears(2),
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                ),
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val exception =
            org.junit.jupiter.api.assertThrows<FunksjonellFeil> {
                barnetsVilkårValidator.validerAtDatoErKorrektIBarnasVilkår(
                    vilkårsvurdering,
                    barna = listOf(barnFødtAugust22),
                )
            }
        assertThat(exception.message).isEqualTo("T.o.m datoen på barnets alder vilkåret må være lik datoen barnet fyller 19 måneder. Dersom barnet ikke lever må t.o.m datoen være lik dato for dødsfall.")
    }

    @Test
    fun `validerAtDatoErKorrektIBarnasVilkår skal kaste funksjonell feil når BARNETS_ALDER vilkår resulat har fom etter barnet blir 13 måneder og barn er født etter 2023`() {
        val barnFødtAugust22 = lagPerson(personType = PersonType.BARN, aktør = randomAktør("01082212345"))
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnFødtAugust22.aktør)
        val vilkårResultaterForBarn =
            setOf(
                VilkårResultat(
                    id = 0,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.BARNETS_ALDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnFødtAugust22.fødselsdato.plusMonths(13),
                    periodeTom = LocalDate.of(2024, 7, 31),
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                ),
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val exception =
            org.junit.jupiter.api.assertThrows<FunksjonellFeil> {
                barnetsVilkårValidator.validerAtDatoErKorrektIBarnasVilkår(
                    vilkårsvurdering,
                    barna = listOf(barnFødtAugust22),
                )
            }
        assertThat(exception.message).isEqualTo("F.o.m datoen på barnets alder vilkåret må være lik barnets 1 års dag.")
    }

    @Test
    fun `validerAtDatoErKorrektIBarnasVilkår skal kaste funksjonell feil når BARNETS_ALDER vilkår resulat har tom etter barnet fyller 2 år og barn er født før 2023`() {
        val barnFødtAugust22 = lagPerson(personType = PersonType.BARN, aktør = randomAktør("01122212345"))
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnFødtAugust22.aktør)
        val vilkårResultaterForBarn =
            setOf(
                VilkårResultat(
                    id = 0,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.BARNETS_ALDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnFødtAugust22.fødselsdato.plusYears(1),
                    periodeTom = barnFødtAugust22.fødselsdato.plusYears(2).plusMonths(2),
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                ),
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val exception =
            org.junit.jupiter.api.assertThrows<FunksjonellFeil> {
                barnetsVilkårValidator.validerAtDatoErKorrektIBarnasVilkår(
                    vilkårsvurdering,
                    barna = listOf(barnFødtAugust22),
                )
            }
        assertThat(exception.message).isEqualTo("T.o.m datoen på barnets alder vilkåret må være lik barnets 2 års dag eller 31.07.24 på grunn av lovendring fra og med 01.08.24. Dersom barnet ikke lever må t.o.m datoen være lik dato for dødsfall.")
    }

    @Test
    fun `validerAtDatoErKorrektIBarnasVilkår skal kaste funksjonell feil når BARNETS_ALDER med adopsjon vilkår resulat har tom etter barnets 6 år`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn =
            setOf(
                VilkårResultat(
                    id = 0,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.BARNETS_ALDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnPerson.fødselsdato.plusYears(3),
                    periodeTom =
                        barnPerson.fødselsdato
                            .plusYears(6)
                            .withMonth(Month.AUGUST.value)
                            .plusMonths(2),
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
                ),
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val exception =
            org.junit.jupiter.api.assertThrows<FunksjonellFeil> {
                barnetsVilkårValidator.validerAtDatoErKorrektIBarnasVilkår(
                    vilkårsvurdering,
                    barna = listOf(barnPerson),
                )
            }
        assertThat(exception.message).isEqualTo("Du kan ikke sette en t.o.m dato på barnets alder vilkåret som er etter august året barnet fyller 6 år.")
    }

    @Test
    fun `validerAtDatoErKorrektIBarnasVilkår skal kaste funksjonell feil når BARNETS_ALDER med adopsjon vilkår resulat har diff mellom fom,tom mer enn 1 år og 1 dag`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn =
            setOf(
                VilkårResultat(
                    id = 0,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.BARNETS_ALDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnPerson.fødselsdato.plusYears(3),
                    periodeTom = barnPerson.fødselsdato.plusYears(6).withMonth(Month.AUGUST.value),
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
                ),
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val exception =
            org.junit.jupiter.api.assertThrows<FunksjonellFeil> {
                barnetsVilkårValidator.validerAtDatoErKorrektIBarnasVilkår(
                    vilkårsvurdering,
                    barna = listOf(barnPerson),
                )
            }
        assertThat(exception.message).isEqualTo("Differansen mellom f.o.m datoen og t.o.m datoen på barnets alder vilkåret kan ikke være mer enn 1 år.")
    }

    @Test
    fun `validerAtDatoErKorrektIBarnasVilkår skal ikke kaste feil når BARNETS_ALDER med adopsjon vilkår resulat har tom etter August barnet fyller 6 år`() {
        val tom = barnPerson.fødselsdato.plusYears(6).withMonth(Month.AUGUST.value)
        val fom = tom.minusMonths(5)
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn =
            setOf(
                VilkårResultat(
                    id = 0,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.BARNETS_ALDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fom,
                    periodeTom = tom,
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
                ),
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        assertDoesNotThrow {
            barnetsVilkårValidator.validerAtDatoErKorrektIBarnasVilkår(
                vilkårsvurdering,
                barna = listOf(barnPerson),
            )
        }
    }

    @Test
    fun `validerAtDatoErKorrektIBarnasVilkår skal ikke kaste feil når MEDLEMSKAP_ANNEN_FORELDER har fom før barnets fødselsdato`() {
        val fom = barnPerson.fødselsdato.minusYears(10)
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn =
            setOf(
                VilkårResultat(
                    id = 0,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fom,
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
                ),
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        assertDoesNotThrow {
            barnetsVilkårValidator.validerAtDatoErKorrektIBarnasVilkår(
                vilkårsvurdering,
                barna = listOf(barnPerson),
            )
        }
    }

    @Test
    fun `validerAtDatoErKorrektIBarnasVilkår skal ikke kaste feil dersom tom på barnetsalder vilkår er satt til barnets dødsfallsdato`() {
        val dødsfallsdato = barnPerson.fødselsdato.plusYears(1).plusMonths(5)

        val barnPersonMedDødsfallsdato =
            barnPerson.apply {
                dødsfall = Dødsfall(id = 1, person = this, dødsfallDato = dødsfallsdato, dødsfallAdresse = null, dødsfallPostnummer = null, dødsfallPoststed = null)
            }

        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn =
            setOf(
                VilkårResultat(
                    id = 0,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.BARNETS_ALDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnPersonMedDødsfallsdato.fødselsdato.plusYears(1),
                    periodeTom = dødsfallsdato,
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                ),
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        assertDoesNotThrow {
            barnetsVilkårValidator.validerAtDatoErKorrektIBarnasVilkår(
                vilkårsvurdering,
                barna = listOf(barnPersonMedDødsfallsdato),
            )
        }
    }

    @Test
    fun `validerAtDatoErKorrektIBarnasVilkår skal kaste funksjonell feil dersom det finnes avslag i barnets alder vilkår uten fom og tom samtidig som det finnes oppfylte barnets alder vilkår`() {
        val barnFødtAugust22 = lagPerson(personType = PersonType.BARN, aktør = randomAktør("01012312345"))
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnFødtAugust22.aktør)
        val vilkårResultaterForBarn =
            setOf(
                VilkårResultat(
                    id = 0,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.BARNETS_ALDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnFødtAugust22.fødselsdato.plusYears(1),
                    periodeTom = barnFødtAugust22.fødselsdato.plusYears(2),
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                    erEksplisittAvslagPåSøknad = true,
                ),
                VilkårResultat(
                    id = 1,
                    personResultat = personResultatForBarn,
                    vilkårType = Vilkår.BARNETS_ALDER,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = null,
                    begrunnelse = "begrunnelse",
                    behandlingId = behandling.id,
                    erEksplisittAvslagPåSøknad = true,
                ),
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val exception =
            org.junit.jupiter.api.assertThrows<FunksjonellFeil> {
                barnetsVilkårValidator.validerAtDatoErKorrektIBarnasVilkår(
                    vilkårsvurdering,
                    barna = listOf(barnFødtAugust22),
                )
            }
        assertThat(exception.message).isEqualTo(
            "Det må være registrert fom og tom periode på avslaget på barnets alder vilkåret dersom det finnes andre perioder som er oppfylt i barnets alder. Dette gjelder barn med fødselsdato: 2023-01-01",
        )
    }
}
