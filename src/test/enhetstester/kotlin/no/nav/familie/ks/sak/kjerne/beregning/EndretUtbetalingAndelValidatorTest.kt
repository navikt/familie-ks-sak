package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndel
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultaterForDeltBosted
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidator
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class EndretUtbetalingAndelValidatorTest {

    val søker = randomAktør()
    private val barn1 = randomAktør()

    val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
        behandlingId = behandling.id,
        søkerPersonIdent = søker.aktivFødselsnummer(),
        barnasIdenter = listOf(barn1.aktivFødselsnummer())
    )
    private val søkerPerson = lagPerson(personopplysningGrunnlag, søker, PersonType.SØKER)
    private val barnPerson = lagPerson(personopplysningGrunnlag, barn1, PersonType.BARN)
    val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

    @Test
    fun `validerPeriodeInnenforTilkjentytelse skal kaste feil når EndretUtbetaling periode slutter etter ty perioder`() {
        val andelTilkjentYtelse = lagAndelTilkjentYtelse(
            behandling = behandling,
            aktør = søker,
            stønadFom = YearMonth.now().minusMonths(1),
            stønadTom = YearMonth.now().plusMonths(5)
        )
        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = søkerPerson,
            prosent = BigDecimal(50),
            periodeFom = YearMonth.now().minusMonths(1),
            periodeTom = YearMonth.now().plusMonths(7)
        )

        val exception = assertThrows<FunksjonellFeil> {
            EndretUtbetalingAndelValidator.validerPeriodeInnenforTilkjentYtelse(
                endretUtbetalingAndel,
                listOf(andelTilkjentYtelse)
            )
        }
        assertFeilMeldingerNårEndretUtbetalingPerioderIkkeInnenforTyPerioder(exception)
    }

    @Test
    fun `validerPeriodeInnenforTilkjentytelse skal kaste feil når EndretUtbetaling periode starter før ty perioder`() {
        val andelTilkjentYtelse = lagAndelTilkjentYtelse(
            behandling = behandling,
            aktør = søker,
            stønadFom = YearMonth.now().minusMonths(1),
            stønadTom = YearMonth.now().plusMonths(5)
        )
        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = søkerPerson,
            prosent = BigDecimal(50),
            periodeFom = YearMonth.now().minusMonths(2),
            periodeTom = YearMonth.now().plusMonths(5)
        )

        val exception = assertThrows<FunksjonellFeil> {
            EndretUtbetalingAndelValidator.validerPeriodeInnenforTilkjentYtelse(
                endretUtbetalingAndel,
                listOf(andelTilkjentYtelse)
            )
        }
        assertFeilMeldingerNårEndretUtbetalingPerioderIkkeInnenforTyPerioder(exception)
    }

    @Test
    fun `validerPeriodeInnenforTilkjentytelse skal kaste feil når EndretUtbetaling periode ikke finnes for person`() {
        val andelTilkjentYtelse = lagAndelTilkjentYtelse(
            behandling = behandling,
            aktør = barn1,
            stønadFom = YearMonth.now().minusMonths(1),
            stønadTom = YearMonth.now().plusMonths(5)
        )
        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = søkerPerson,
            prosent = BigDecimal(50),
            periodeFom = YearMonth.now().minusMonths(1),
            periodeTom = YearMonth.now().plusMonths(5)
        )

        val exception = assertThrows<FunksjonellFeil> {
            EndretUtbetalingAndelValidator.validerPeriodeInnenforTilkjentYtelse(
                endretUtbetalingAndel,
                listOf(andelTilkjentYtelse)
            )
        }
        assertFeilMeldingerNårEndretUtbetalingPerioderIkkeInnenforTyPerioder(exception)
    }

    @Test
    fun `validerPeriodeInnenforTilkjentytelse skal ikke kaste feil når EndretUtbetaling periode er innefor ty periode`() {
        val andelTilkjentYtelse = lagAndelTilkjentYtelse(
            behandling = behandling,
            aktør = søker,
            stønadFom = YearMonth.now().minusMonths(2),
            stønadTom = YearMonth.now().plusMonths(5)
        )
        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = søkerPerson,
            prosent = BigDecimal(50),
            periodeFom = YearMonth.now().minusMonths(1),
            periodeTom = YearMonth.now().plusMonths(4)
        )

        assertDoesNotThrow {
            EndretUtbetalingAndelValidator.validerPeriodeInnenforTilkjentYtelse(
                endretUtbetalingAndel,
                listOf(andelTilkjentYtelse)
            )
        }
    }

    @Test
    fun `finnDeltBostedPerioder skal finne riktige delt bosted perioder for barn, og slå sammen de som er sammenhengende`() {
        val fom = LocalDate.now().minusMonths(5)
        val tom = LocalDate.now().plusMonths(7)

        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn = lagVilkårResultaterForDeltBosted(
            personResultat = personResultatForBarn,
            behandlingId = behandling.id,
            fom1 = fom,
            tom1 = tom
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val deltBostedPerioder = EndretUtbetalingAndelValidator.finnDeltBostedPerioder(
            person = barnPerson,
            vilkårsvurdering = vilkårsvurdering
        )

        assertTrue(deltBostedPerioder.size == 1)
        assertEquals(fom.plusMonths(1).førsteDagIInneværendeMåned(), deltBostedPerioder.single().fom)
        assertEquals(tom.sisteDagIMåned(), deltBostedPerioder.single().tom)
    }

    @Test
    fun `finnDeltBostedPerioder skal finne riktige delt bosted perioder for barn, og ikke slå sammen når de ikke er sammenhengende`() {
        val fom1 = LocalDate.now().minusMonths(5)
        val tom1 = LocalDate.now().minusMonths(2)
        val fom2 = LocalDate.now()
        val tom2 = LocalDate.now().plusMonths(7)

        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)

        val vilkårResultaterForBarn = lagVilkårResultaterForDeltBosted(
            personResultat = personResultatForBarn,
            behandlingId = behandling.id,
            fom1 = fom1,
            tom1 = tom1,
            fom2 = fom2,
            tom2 = tom2
        )

        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val deltBostedPerioder = EndretUtbetalingAndelValidator.finnDeltBostedPerioder(
            person = barnPerson,
            vilkårsvurdering = vilkårsvurdering
        )

        assertTrue(deltBostedPerioder.size == 2)
        assertEquals(fom1.plusMonths(1).førsteDagIInneværendeMåned(), deltBostedPerioder[0].fom)
        assertEquals(tom1.sisteDagIMåned(), deltBostedPerioder[0].tom)
        assertEquals(fom2.plusMonths(1).førsteDagIInneværendeMåned(), deltBostedPerioder[1].fom)
        assertEquals(tom2.sisteDagIMåned(), deltBostedPerioder[1].tom)
    }

    @Test
    fun `finnDeltBostedPerioder Skal finne riktige delt bosted perioder for søker, og slå sammen de som er sammenhengende`() {
        val fomBarn1 = LocalDate.now().minusMonths(5)
        val tomBarn1 = LocalDate.now().plusMonths(7)
        val fomBarn2 = fomBarn1.minusMonths(5)

        val barn2 = randomAktør()
        val personResultatForBarn1 = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val personResultatForBarn2 = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn2)

        val vilkårResultaterForBarn1 = lagVilkårResultaterForDeltBosted(
            personResultat = personResultatForBarn1,
            behandlingId = behandling.id,
            fom1 = fomBarn1,
            tom1 = LocalDate.now().minusMonths(1).sisteDagIMåned(),
            fom2 = LocalDate.now().førsteDagIInneværendeMåned(),
            tom2 = tomBarn1
        )
        val vilkårResultaterForBarn2 = lagVilkårResultaterForDeltBosted(
            personResultat = personResultatForBarn2,
            behandlingId = behandling.id,
            fom1 = fomBarn2,
            tom1 = fomBarn1 // sammenhengde periode med første barn vilkår resultat
        )

        personResultatForBarn1.setSortedVilkårResultater(vilkårResultaterForBarn1)
        personResultatForBarn2.setSortedVilkårResultater(vilkårResultaterForBarn2)

        vilkårsvurdering.personResultater = setOf(personResultatForBarn1, personResultatForBarn2)

        val deltBostedPerioder = EndretUtbetalingAndelValidator.finnDeltBostedPerioder(
            person = søkerPerson,
            vilkårsvurdering = vilkårsvurdering
        )

        assertTrue(deltBostedPerioder.size == 1)
        assertEquals(fomBarn2.plusMonths(1).førsteDagIInneværendeMåned(), deltBostedPerioder.single().fom)
        assertEquals(tomBarn1.sisteDagIMåned(), deltBostedPerioder.single().tom)
    }

    @Test
    fun `finnDeltBostedPerioder Skal finne riktige delt bosted perioder for søker, og slå sammen de som overlapper`() {
        val fomBarn1 = LocalDate.now().minusMonths(5)
        val tomBarn1 = LocalDate.now().plusMonths(7)
        val fomBarn2 = fomBarn1.minusMonths(5)

        val barn2 = randomAktør()
        val personResultatForBarn1 = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val personResultatForBarn2 = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn2)

        val vilkårResultaterForBarn1 = lagVilkårResultaterForDeltBosted(
            personResultat = personResultatForBarn1,
            behandlingId = behandling.id,
            fom1 = fomBarn1,
            tom1 = LocalDate.now().minusMonths(1).sisteDagIMåned(),
            fom2 = LocalDate.now().førsteDagIInneværendeMåned(),
            tom2 = tomBarn1
        )
        val vilkårResultaterForBarn2 = lagVilkårResultaterForDeltBosted(
            personResultat = personResultatForBarn2,
            behandlingId = behandling.id,
            fom1 = fomBarn2,
            tom1 = tomBarn1 // overlapper med første barn vilkårresultat
        )

        personResultatForBarn1.setSortedVilkårResultater(vilkårResultaterForBarn1)
        personResultatForBarn2.setSortedVilkårResultater(vilkårResultaterForBarn2)

        vilkårsvurdering.personResultater = setOf(personResultatForBarn1, personResultatForBarn2)

        val deltBostedPerioder = EndretUtbetalingAndelValidator.finnDeltBostedPerioder(
            person = søkerPerson,
            vilkårsvurdering = vilkårsvurdering
        )

        assertTrue(deltBostedPerioder.size == 1)
        assertEquals(fomBarn2.plusMonths(1).førsteDagIInneværendeMåned(), deltBostedPerioder.single().fom)
        assertEquals(tomBarn1.sisteDagIMåned(), deltBostedPerioder.single().tom)
    }

    @Test
    fun `validerÅrsak skal kaste feil når delt bosted periode ikke er innenfor endringsperiode`() {
        val fom = LocalDate.now().minusMonths(5)
        val tom = LocalDate.now().plusMonths(7)

        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn = lagVilkårResultaterForDeltBosted(
            personResultat = personResultatForBarn,
            behandlingId = behandling.id,
            fom1 = fom,
            tom1 = tom
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = barnPerson,
            periodeFom = YearMonth.now().minusMonths(10),
            periodeTom = YearMonth.now().minusMonths(8)
        )

        val exception = assertThrows<FunksjonellFeil> {
            EndretUtbetalingAndelValidator.validerÅrsak(Årsak.DELT_BOSTED, endretUtbetalingAndel, vilkårsvurdering)
        }

        assertEquals(
            "Det finnes ingen delt bosted perioder i perioden det opprettes en endring med årsak delt bosted for.",
            exception.message
        )
        assertEquals(
            "Du har valgt årsaken 'delt bosted', " +
                "denne samstemmer ikke med vurderingene gjort på vilkårsvurderingssiden i perioden du har valgt.",
            exception.frontendFeilmelding
        )
    }

    @Test
    fun `validerÅrsak skal ikke kaste feil når delt bosted periode er innenfor endringsperiode`() {
        val fom = LocalDate.now().minusMonths(5)
        val tom = LocalDate.now().plusMonths(7)

        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn = lagVilkårResultaterForDeltBosted(
            personResultat = personResultatForBarn,
            behandlingId = behandling.id,
            fom1 = fom,
            tom1 = tom
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn)

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = barnPerson,
            periodeFom = YearMonth.now().minusMonths(3),
            periodeTom = YearMonth.now().minusMonths(2)
        )

        assertDoesNotThrow {
            EndretUtbetalingAndelValidator.validerÅrsak(Årsak.DELT_BOSTED, endretUtbetalingAndel, vilkårsvurdering)
        }
    }

    @Test
    fun `validerÅrsak skal kaste feil når årsak er ETTERBETALING_3MND, men perioden skal utbetales `() {
        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = barnPerson,
            periodeFom = YearMonth.now().minusMonths(3),
            periodeTom = YearMonth.now().minusMonths(2),
            prosent = BigDecimal(100)
        )
        val exception = assertThrows<FunksjonellFeil> {
            EndretUtbetalingAndelValidator.validerÅrsak(Årsak.ETTERBETALING_3MND, endretUtbetalingAndel, null)
        }

        assertEquals(
            "Du kan ikke sette årsak etterbetaling 3 måned når du har valgt at perioden skal utbetales.",
            exception.message
        )
    }

    @Test
    fun `validerÅrsak skal kaste feil når årsak er ETTERBETALING_3MND, men endringsperiode slutter etter etterbetalingsgrense`() {
        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = barnPerson,
            periodeFom = YearMonth.now().minusMonths(1),
            periodeTom = YearMonth.now().plusMonths(2),
            prosent = BigDecimal.ZERO
        )
        val exception = assertThrows<FunksjonellFeil> {
            EndretUtbetalingAndelValidator.validerÅrsak(Årsak.ETTERBETALING_3MND, endretUtbetalingAndel, null)
        }

        assertEquals(
            "Du kan ikke stoppe etterbetaling for en periode som ikke strekker seg mer enn 3 måned tilbake i tid.",
            exception.message
        )
    }

    @Test
    fun `validerÅrsak skal ikke kaste feil når årsak er ETTERBETALING_3MND, men endringsperiode slutter innefor etterbetalingsgrense`() {
        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = barnPerson,
            periodeFom = YearMonth.now().minusMonths(5),
            periodeTom = YearMonth.now().minusMonths(4),
            prosent = BigDecimal.ZERO
        )
        assertDoesNotThrow {
            EndretUtbetalingAndelValidator.validerÅrsak(Årsak.ETTERBETALING_3MND, endretUtbetalingAndel, null)
        }
    }

    @Test
    fun `validerAtAlleOpprettedeEndringerErUtfylt skal ikke kaste feil når endret utbetaling andel er oppfylt`() {
        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = barnPerson,
            periodeFom = YearMonth.now().minusMonths(5),
            periodeTom = YearMonth.now().minusMonths(4),
            prosent = BigDecimal.ZERO
        )
        assertDoesNotThrow {
            EndretUtbetalingAndelValidator.validerAtAlleOpprettedeEndringerErUtfylt(listOf(endretUtbetalingAndel))
        }
    }

    @Test
    fun `validerAtAlleOpprettedeEndringerErUtfylt skal kaste feil når endret utbetaling andel ikke er oppfylt`() {
        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = barnPerson,
            periodeFom = YearMonth.now().minusMonths(5),
            periodeTom = YearMonth.now().minusMonths(4),
            prosent = null
        )
        val exception = assertThrows<FunksjonellFeil> {
            EndretUtbetalingAndelValidator.validerAtAlleOpprettedeEndringerErUtfylt(listOf(endretUtbetalingAndel))
        }
        assertEquals(
            "Det er opprettet instanser av EndretUtbetalingandel som ikke er fylt ut " +
                "før navigering til neste steg.",
            exception.message
        )
        assertEquals(
            "Du har opprettet en eller flere endrede utbetalingsperioder " +
                "som er ufullstendig utfylt. Disse må enten fylles ut eller slettes før du kan gå videre.",
            exception.frontendFeilmelding
        )
    }

    @Test
    fun `validerAtEndringerErTilknyttetAndelTilkjentYtelse skal ikke kaste feil når endret utbetaling andel har ATY`() {
        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = barnPerson,
            periodeFom = YearMonth.now().minusMonths(5),
            periodeTom = YearMonth.now().minusMonths(4),
            prosent = BigDecimal.ZERO
        )
        val andelerTilkjentYtelse = listOf(lagAndelTilkjentYtelse(behandling = behandling))
        val endretUtbetalingAndelMedAndelerTilkjentYtelse =
            EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel, andelerTilkjentYtelse)
        assertDoesNotThrow {
            EndretUtbetalingAndelValidator
                .validerAtEndringerErTilknyttetAndelTilkjentYtelse(listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse))
        }
    }

    @Test
    fun `validerAtEndringerErTilknyttetAndelTilkjentYtelse skal kaste feil når endret utbetaling andel ikke har ATY`() {
        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = barnPerson,
            periodeFom = YearMonth.now().minusMonths(5),
            periodeTom = YearMonth.now().minusMonths(4),
            prosent = BigDecimal.ZERO
        )
        val endretUtbetalingAndelMedAndelerTilkjentYtelse =
            EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel, emptyList())
        val exception = assertThrows<FunksjonellFeil> {
            EndretUtbetalingAndelValidator
                .validerAtEndringerErTilknyttetAndelTilkjentYtelse(listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse))
        }

        assertEquals(
            "Det er opprettet instanser av EndretUtbetalingandel som ikke er tilknyttet noen andeler. " +
                "De må enten lagres eller slettes av SB.",
            exception.message
        )
        assertEquals(
            "Du har endrede utbetalingsperioder. Bekreft, slett eller oppdater periodene i listen.",
            exception.frontendFeilmelding
        )
    }

    private fun assertFeilMeldingerNårEndretUtbetalingPerioderIkkeInnenforTyPerioder(exception: FunksjonellFeil) {
        assertEquals(
            "Det er ingen tilkjent ytelse for personen det blir forsøkt lagt til en endret periode for.",
            exception.message
        )
        assertEquals(
            "Du har valgt en periode der det ikke finnes tilkjent ytelse for valgt person " +
                "i hele eller deler av perioden.",
            exception.frontendFeilmelding
        )
    }
}
