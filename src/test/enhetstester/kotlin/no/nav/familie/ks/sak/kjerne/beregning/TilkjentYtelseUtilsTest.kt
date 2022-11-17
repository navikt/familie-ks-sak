package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.util.NullablePeriode
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultaterForBarn
import no.nav.familie.ks.sak.data.lagVilkårsvurderingMedSøkersVilkår
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.maksBeløp
import no.nav.familie.ks.sak.kjerne.beregning.domene.prosent
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

internal class TilkjentYtelseUtilsTest {

    private val søker = randomAktør()
    private val barn1 = randomAktør("01012112345")

    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
        behandlingId = behandling.id,
        søkerPersonIdent = søker.aktivFødselsnummer(),
        barnasIdenter = listOf(barn1.aktivFødselsnummer()),
        søkerAktør = søker,
        barnAktør = listOf(barn1)
    )
    private val barnPerson = lagPerson(personopplysningGrunnlag, barn1, PersonType.BARN)

    private val maksBeløp = maksBeløp()

    private lateinit var vilkårsvurdering: Vilkårsvurdering

    @BeforeEach
    fun init() {
        vilkårsvurdering = lagVilkårsvurderingMedSøkersVilkår(
            søkerAktør = søker,
            behandling = behandling,
            resultat = Resultat.OPPFYLT,
            søkerPeriodeFom = LocalDate.of(1987, 1, 1),
            søkerPeriodeTom = null
        )
    }

    @Test
    fun `beregnTilkjentYtelse skal beregne TY for et barn uten barnehageplass`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val barnFødselsdato = barnPerson.fødselsdato
        val fom = barnFødselsdato.plusYears(1)
        val tom = barnFødselsdato.plusYears(2)

        // antallTimer null betyr at barn ikke har fått barnehageplass. Da får barn full KS
        val barnehagePlassPeriodeMedAntallTimer = NullablePeriode(fom = fom, tom = tom) to null

        val vilkårResultaterForBarn = lagVilkårResultaterForBarn(
            personResultat = personResultatForBarn,
            barnFødselsdato = barnFødselsdato,
            barnehageplassPerioder = listOf(barnehagePlassPeriodeMedAntallTimer),
            behandlingId = behandling.id
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag
        )
        assertTilkjentYtelse(tilkjentYtelse, 1)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(100),
            periodeFom = fom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = tom.minusMonths(1).sisteDagIMåned()
        )
    }

    @Test
    fun `beregnTilkjentYtelse skal beregne TY for et barn med 8 timer barnehageplass`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val barnFødselsdato = barnPerson.fødselsdato
        val fom = barnFødselsdato.plusYears(1)
        val tom = barnFødselsdato.plusYears(2)

        val barnehagePlassPeriodeMedAntallTimer = NullablePeriode(fom = fom, tom = tom) to BigDecimal(8)

        val vilkårResultaterForBarn = lagVilkårResultaterForBarn(
            personResultat = personResultatForBarn,
            barnFødselsdato = barnFødselsdato,
            barnehageplassPerioder = listOf(barnehagePlassPeriodeMedAntallTimer),
            behandlingId = behandling.id
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag
        )
        assertTilkjentYtelse(tilkjentYtelse, 1)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(80),
            periodeFom = fom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = tom.minusMonths(1).sisteDagIMåned()
        )
    }

    @Test
    fun `beregnTilkjentYtelse skal beregne TY for et barn når barnehageplass endrer fra 8 timer til ingen`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val barnFødselsdato = barnPerson.fødselsdato
        val førstePeriodeFom = barnFødselsdato.plusYears(1)
        val førstePeriodeTom = barnFødselsdato.plusYears(1).plusMonths(7).withDayOfMonth(20)
        val andrePeriodeFom = førstePeriodeTom.plusDays(1)
        val andrePeriodeTom = null

        val barnehagePlassPerioderMedAntallTimer = listOf(
            NullablePeriode(førstePeriodeFom, førstePeriodeTom) to BigDecimal(8),
            NullablePeriode(andrePeriodeFom, andrePeriodeTom) to null
        )

        val vilkårResultaterForBarn = lagVilkårResultaterForBarn(
            personResultat = personResultatForBarn,
            barnFødselsdato = barnFødselsdato,
            barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
            behandlingId = behandling.id
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag
        )
        assertTilkjentYtelse(tilkjentYtelse, 2)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(80),
            periodeFom = førstePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = førstePeriodeTom.sisteDagIMåned()
        )
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.last(),
            prosent = BigDecimal(100),
            periodeFom = andrePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = barnFødselsdato.plusYears(2).minusMonths(1).sisteDagIMåned()
        )
    }

    @Test
    fun `beregnTilkjentYtelse skal beregne TY for et barn når barnehageplass øker fra 8 timer til 17 timer`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val barnFødselsdato = barnPerson.fødselsdato
        val førstePeriodeFom = barnFødselsdato.plusYears(1)
        val førstePeriodeTom = barnFødselsdato.plusYears(1).plusMonths(7).withDayOfMonth(20)
        val andrePeriodeFom = førstePeriodeTom.plusDays(1)
        val andrePeriodeTom = null

        val barnehagePlassPerioderMedAntallTimer = listOf(
            NullablePeriode(førstePeriodeFom, førstePeriodeTom) to BigDecimal(8),
            NullablePeriode(andrePeriodeFom, andrePeriodeTom) to BigDecimal(17)
        )

        val vilkårResultaterForBarn = lagVilkårResultaterForBarn(
            personResultat = personResultatForBarn,
            barnFødselsdato = barnFødselsdato,
            barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
            behandlingId = behandling.id
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag
        )
        assertTilkjentYtelse(tilkjentYtelse, 2)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(80),
            periodeFom = førstePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = førstePeriodeTom.minusMonths(1).sisteDagIMåned()
        )
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.last(),
            prosent = BigDecimal(40),
            periodeFom = andrePeriodeFom.førsteDagIInneværendeMåned(),
            periodeTom = barnFødselsdato.plusYears(2).minusMonths(1).sisteDagIMåned()
        )
    }

    @Test
    fun `beregnTilkjentYtelse skal beregne TY for et barn når barnehageplass øker fra ingen til deltids`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val barnFødselsdato = barnPerson.fødselsdato
        val førstePeriodeFom = barnFødselsdato.plusYears(1)
        val førstePeriodeTom = barnFødselsdato.plusYears(1).plusMonths(7).withDayOfMonth(20)
        val andrePeriodeFom = førstePeriodeTom.plusDays(1)
        val andrePeriodeTom = null

        val barnehagePlassPerioderMedAntallTimer = listOf(
            NullablePeriode(førstePeriodeFom, førstePeriodeTom) to null,
            NullablePeriode(andrePeriodeFom, andrePeriodeTom) to BigDecimal(8)
        )

        val vilkårResultaterForBarn = lagVilkårResultaterForBarn(
            personResultat = personResultatForBarn,
            barnFødselsdato = barnFødselsdato,
            barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
            behandlingId = behandling.id
        )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag
        )
        assertTilkjentYtelse(tilkjentYtelse, 2)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(100),
            periodeFom = førstePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = førstePeriodeTom.minusMonths(1).sisteDagIMåned()
        )
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.last(),
            prosent = BigDecimal(80),
            periodeFom = andrePeriodeFom.førsteDagIInneværendeMåned(),
            periodeTom = barnFødselsdato.plusYears(2).minusMonths(1).sisteDagIMåned()
        )
    }

    @Test
    fun `beregnTilkjentYtelse skal beregne TY for et barn når barnehageplass øker fra deltids til full`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val barnFødselsdato = barnPerson.fødselsdato
        val førstePeriodeFom = barnFødselsdato.plusYears(1)
        val førstePeriodeTom = barnFødselsdato.plusYears(1).plusMonths(7).withDayOfMonth(20)
        val andrePeriodeFom = førstePeriodeTom.plusDays(1)
        val andrePeriodeTom = null

        // kan ikke legge til full barnehageplass her fordi det gir IKKE_OPPFYLT vilkår resultat
        val barnehagePlassPerioderMedAntallTimer = listOf(
            NullablePeriode(førstePeriodeFom, førstePeriodeTom) to BigDecimal(8)
        )

        val vilkårResultaterForBarn = lagVilkårResultaterForBarn(
            personResultat = personResultatForBarn,
            barnFødselsdato = barnFødselsdato,
            barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
            behandlingId = behandling.id
        ).toMutableSet()
        // full barnehageplass vilkår
        val fullBarnehageplassVilkår = VilkårResultat(
            personResultat = personResultatForBarn,
            vilkårType = Vilkår.BARNEHAGEPLASS,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = andrePeriodeFom,
            periodeTom = andrePeriodeTom,
            begrunnelse = "",
            behandlingId = behandling.id,
            antallTimer = BigDecimal(33)
        )
        vilkårResultaterForBarn.add(fullBarnehageplassVilkår)
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag
        )
        assertTilkjentYtelse(tilkjentYtelse, 1)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(80),
            periodeFom = førstePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = førstePeriodeTom.minusMonths(1).sisteDagIMåned()
        )
    }

    @Test
    fun `beregnTilkjentYtelse skal beregne TY for et barn når barnehageplass reduserer fra full til deltids`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val barnFødselsdato = barnPerson.fødselsdato
        val førstePeriodeFom = barnFødselsdato.plusYears(1)
        val førstePeriodeTom = barnFødselsdato.plusYears(1).plusMonths(7).withDayOfMonth(20)
        val andrePeriodeFom = førstePeriodeTom.plusDays(1)
        val andrePeriodeTom = null

        // kan ikke legge til full barnehageplass her fordi det gir IKKE_OPPFYLT vilkår resultat
        val barnehagePlassPerioderMedAntallTimer = listOf(
            NullablePeriode(andrePeriodeFom, andrePeriodeTom) to BigDecimal(8)
        )

        val vilkårResultaterForBarn = lagVilkårResultaterForBarn(
            personResultat = personResultatForBarn,
            barnFødselsdato = barnFødselsdato,
            barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
            behandlingId = behandling.id
        ).toMutableSet()
        // full barnehageplass vilkår
        val fullBarnehageplassVilkår = VilkårResultat(
            personResultat = personResultatForBarn,
            vilkårType = Vilkår.BARNEHAGEPLASS,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = førstePeriodeFom,
            periodeTom = førstePeriodeTom,
            begrunnelse = "",
            behandlingId = behandling.id,
            antallTimer = BigDecimal(33)
        )
        vilkårResultaterForBarn.add(fullBarnehageplassVilkår)
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag
        )
        assertTilkjentYtelse(tilkjentYtelse, 1)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(80),
            periodeFom = andrePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = barnFødselsdato.plusYears(2).minusMonths(1).sisteDagIMåned()
        )
    }

    @Test
    fun `beregnTilkjentYtelse skal beregne TY for et barn når barnehageplass reduserer fra 17 timer til 8 timer`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val barnFødselsdato = barnPerson.fødselsdato
        val førstePeriodeFom = barnFødselsdato.plusYears(1)
        val førstePeriodeTom = barnFødselsdato.plusYears(1).plusMonths(7).withDayOfMonth(20)
        val andrePeriodeFom = førstePeriodeTom.plusDays(1)
        val andrePeriodeTom = null

        // kan ikke legge til full barnehageplass her fordi det gir IKKE_OPPFYLT vilkår resultat
        val barnehagePlassPerioderMedAntallTimer = listOf(
            NullablePeriode(førstePeriodeFom, førstePeriodeTom) to BigDecimal(17),
            NullablePeriode(andrePeriodeFom, andrePeriodeTom) to BigDecimal(8)
        )

        val vilkårResultaterForBarn = lagVilkårResultaterForBarn(
            personResultat = personResultatForBarn,
            barnFødselsdato = barnFødselsdato,
            barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
            behandlingId = behandling.id
        )

        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag
        )
        assertTilkjentYtelse(tilkjentYtelse, 2)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(40),
            periodeFom = førstePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = førstePeriodeTom.sisteDagIMåned()
        )
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.last(),
            prosent = BigDecimal(80),
            periodeFom = andrePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = barnFødselsdato.plusYears(2).minusMonths(1).sisteDagIMåned()
        )
    }

    @Test
    fun `beregnTilkjentYtelse skal beregne TY for et barn når barnehageplass reduserer med vilkårene er rett etter hverandre i månedsskifte`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val barnFødselsdato = barnPerson.fødselsdato
        val førstePeriodeFom = barnFødselsdato.plusYears(1)
        val førstePeriodeTom = barnFødselsdato.plusYears(1).plusMonths(7).sisteDagIMåned()
        val andrePeriodeFom = førstePeriodeTom.plusDays(1)
        val andrePeriodeTom = null

        // kan ikke legge til full barnehageplass her fordi det gir IKKE_OPPFYLT vilkår resultat
        val barnehagePlassPerioderMedAntallTimer = listOf(
            NullablePeriode(førstePeriodeFom, førstePeriodeTom) to BigDecimal(17),
            NullablePeriode(andrePeriodeFom, andrePeriodeTom) to BigDecimal(8)
        )

        val vilkårResultaterForBarn = lagVilkårResultaterForBarn(
            personResultat = personResultatForBarn,
            barnFødselsdato = barnFødselsdato,
            barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
            behandlingId = behandling.id
        )

        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag
        )
        assertTilkjentYtelse(tilkjentYtelse, 2)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(40),
            periodeFom = førstePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = førstePeriodeTom
        )
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.last(),
            prosent = BigDecimal(80),
            periodeFom = andrePeriodeFom,
            periodeTom = barnFødselsdato.plusYears(2).minusMonths(1).sisteDagIMåned()
        )
    }

    @Test
    fun `beregnTilkjentYtelse skal beregne TY for et barn når barnehageplass øker med vilkårene er rett etter hverandre i månedsskifte`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val barnFødselsdato = barnPerson.fødselsdato
        val førstePeriodeFom = barnFødselsdato.plusYears(1)
        val førstePeriodeTom = barnFødselsdato.plusYears(1).plusMonths(7).sisteDagIMåned()
        val andrePeriodeFom = førstePeriodeTom.plusDays(1)
        val andrePeriodeTom = null

        // kan ikke legge til full barnehageplass her fordi det gir IKKE_OPPFYLT vilkår resultat
        val barnehagePlassPerioderMedAntallTimer = listOf(
            NullablePeriode(førstePeriodeFom, førstePeriodeTom) to BigDecimal(8),
            NullablePeriode(andrePeriodeFom, andrePeriodeTom) to BigDecimal(17)
        )

        val vilkårResultaterForBarn = lagVilkårResultaterForBarn(
            personResultat = personResultatForBarn,
            barnFødselsdato = barnFødselsdato,
            barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
            behandlingId = behandling.id
        )

        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag
        )
        assertTilkjentYtelse(tilkjentYtelse, 2)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(80),
            periodeFom = førstePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = førstePeriodeTom.sisteDagIMåned()
        )
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.last(),
            prosent = BigDecimal(40),
            periodeFom = andrePeriodeFom.førsteDagIInneværendeMåned(),
            periodeTom = barnFødselsdato.plusYears(2).minusMonths(1).sisteDagIMåned()
        )
    }

    private fun assertTilkjentYtelse(tilkjentYtelse: TilkjentYtelse, antallAndeler: Int) {
        assertEquals(LocalDate.now(), tilkjentYtelse.opprettetDato)
        assertEquals(LocalDate.now(), tilkjentYtelse.endretDato)
        assertTrue { tilkjentYtelse.andelerTilkjentYtelse.isNotEmpty() && tilkjentYtelse.andelerTilkjentYtelse.size == antallAndeler }
    }

    private fun assertAndelTilkjentYtelse(andelTilkjentYtelse: AndelTilkjentYtelse, prosent: BigDecimal, periodeFom: LocalDate, periodeTom: LocalDate) {
        assertEquals(barn1, andelTilkjentYtelse.aktør)
        assertEquals(YtelseType.ORDINÆR_KONTANTSTØTTE, andelTilkjentYtelse.type)
        assertEquals(prosent, andelTilkjentYtelse.prosent)
        assertEquals(periodeFom.toYearMonth(), andelTilkjentYtelse.stønadFom)
        assertEquals(periodeTom.toYearMonth(), andelTilkjentYtelse.stønadTom)
        assertEquals(maksBeløp.prosent(prosent), andelTilkjentYtelse.kalkulertUtbetalingsbeløp)
        assertEquals(maksBeløp.prosent(prosent), andelTilkjentYtelse.nasjonaltPeriodebeløp)
    }
}
