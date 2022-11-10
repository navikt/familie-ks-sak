package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.util.Periode
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
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
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
        val tom = barnFødselsdato.plusYears(1).plusMonths(7)

        // antallTimer null betyr at barn ikke har fått barnehageplass. Da får barn full KS
        val barnehagePlassPeriodeMedAntallTimer = Periode(fom = fom, tom = tom) to null

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

        assertEquals(LocalDate.now(), tilkjentYtelse.opprettetDato)
        assertEquals(LocalDate.now(), tilkjentYtelse.endretDato)
        assertTrue { tilkjentYtelse.andelerTilkjentYtelse.isNotEmpty() && tilkjentYtelse.andelerTilkjentYtelse.size == 1 }

        val andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first()
        assertEquals(barn1, andelTilkjentYtelse.aktør)
        assertEquals(YtelseType.ORDINÆR_KONTANTSTØTTE, andelTilkjentYtelse.type)
        assertEquals(BigDecimal(100), andelTilkjentYtelse.prosent)
        assertEquals(fom.plusMonths(1).førsteDagIInneværendeMåned().toYearMonth(), andelTilkjentYtelse.stønadFom)
        assertEquals(tom.minusMonths(1).sisteDagIMåned().toYearMonth(), andelTilkjentYtelse.stønadTom)
        assertEquals(maksBeløp, andelTilkjentYtelse.kalkulertUtbetalingsbeløp)
        assertEquals(maksBeløp, andelTilkjentYtelse.nasjonaltPeriodebeløp)
    }

    @Test
    fun `beregnTilkjentYtelse skal beregne TY for et barn med 8 timer barnehageplass`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val barnFødselsdato = barnPerson.fødselsdato
        val fom = barnFødselsdato.plusYears(1)
        val tom = barnFødselsdato.plusYears(1).plusMonths(7)

        // antallTimer null betyr at barn ikke har fått barnehageplass. Da får barn full KS
        val barnehagePlassPeriodeMedAntallTimer = Periode(fom = fom, tom = tom) to BigDecimal(8)

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

        assertEquals(LocalDate.now(), tilkjentYtelse.opprettetDato)
        assertEquals(LocalDate.now(), tilkjentYtelse.endretDato)
        assertTrue { tilkjentYtelse.andelerTilkjentYtelse.isNotEmpty() && tilkjentYtelse.andelerTilkjentYtelse.size == 1 }

        val andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first()
        assertEquals(barn1, andelTilkjentYtelse.aktør)
        assertEquals(YtelseType.ORDINÆR_KONTANTSTØTTE, andelTilkjentYtelse.type)
        assertEquals(BigDecimal(80), andelTilkjentYtelse.prosent)
        assertEquals(fom.plusMonths(1).førsteDagIInneværendeMåned().toYearMonth(), andelTilkjentYtelse.stønadFom)
        assertEquals(tom.minusMonths(1).sisteDagIMåned().toYearMonth(), andelTilkjentYtelse.stønadTom)
        assertEquals(maksBeløp.prosent(BigDecimal(80)), andelTilkjentYtelse.kalkulertUtbetalingsbeløp)
        assertEquals(maksBeløp.prosent(BigDecimal(80)), andelTilkjentYtelse.nasjonaltPeriodebeløp)
    }
}
