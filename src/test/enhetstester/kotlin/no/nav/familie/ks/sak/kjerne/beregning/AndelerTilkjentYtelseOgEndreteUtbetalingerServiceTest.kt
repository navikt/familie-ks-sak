package no.nav.familie.ks.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
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
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.maksBeløp
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class AndelerTilkjentYtelseOgEndreteUtbetalingerServiceTest {
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val endretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val unleashService = mockk<UnleashNextMedContextService>()

    private val andelerTilkjentYtelseOgEndreteUtbetalingerService =
        AndelerTilkjentYtelseOgEndreteUtbetalingerService(
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            endretUtbetalingAndelRepository = endretUtbetalingAndelRepository,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            unleashService = unleashService,
        )

    @BeforeEach
    fun setup() {
        every { unleashService.isEnabled(FeatureToggle.ALLEREDE_UTBETALT_SOM_ENDRINGSÅRSAK) } returns true
    }

    val søker = randomAktør()
    private val barn1 = randomAktør()

    val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    val personopplysningGrunnlag =
        lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer(),
            barnasIdenter = listOf(barn1.aktivFødselsnummer()),
        )
    private val søkerPerson = lagPerson(personopplysningGrunnlag, søker, PersonType.SØKER)
    val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

    @Test
    fun `finnAndelerTilkjentYtelseMedEndreteUtbetalinger finner andeler med overlappende endrete utbetalinger`() {
        val fom = YearMonth.now().minusMonths(6)
        val tom = YearMonth.now().plusMonths(5)
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = søker,
                    stønadFom = fom,
                    stønadTom = tom,
                ),
            )

        every { endretUtbetalingAndelRepository.hentEndretUtbetalingerForBehandling(behandling.id) } returns
            listOf(
                // overlappende periode, kommer med andelTilkjentYtelse
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søkerPerson,
                    prosent = BigDecimal(100),
                    periodeFom = YearMonth.now().minusMonths(2),
                    periodeTom = YearMonth.now().minusMonths(1),
                ),
                // ikke overlappende perioder, kommer ikke med andelTilkjentYtelse
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søkerPerson,
                    prosent = BigDecimal(100),
                    periodeFom = YearMonth.now().minusMonths(10),
                    periodeTom = YearMonth.now().minusMonths(9),
                ),
            )

        val andeler =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
        assertTrue { andeler.size == 1 }

        val andelTilkjentYtelse = andeler[0].andel
        assertEquals(fom, andelTilkjentYtelse.stønadFom)
        assertEquals(tom, andelTilkjentYtelse.stønadTom)
        assertEquals(BigDecimal(100), andelTilkjentYtelse.prosent)
        assertEquals(maksBeløp(), andelTilkjentYtelse.kalkulertUtbetalingsbeløp)

        val endretUtbetalingAndeler = andeler[0].endreteUtbetalinger
        assertTrue { endretUtbetalingAndeler.size == 1 }
        assertEquals(YearMonth.now().minusMonths(2), endretUtbetalingAndeler[0].fom)
        assertEquals(YearMonth.now().minusMonths(1), endretUtbetalingAndeler[0].tom)
        assertNotNull(endretUtbetalingAndeler[0].søknadstidspunkt)
    }

    @Test
    fun `finnEndreteUtbetalingerMedAndelerTilkjentYtelse finner endrete utbetalinger med overlappende andeler`() {
        val fom = YearMonth.now().minusMonths(6)
        val tom = YearMonth.now().minusMonths(1)
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
            listOf(
                // begge 2 er overlappende perioder og følger med
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = søker,
                    stønadFom = YearMonth.now().minusMonths(6),
                    stønadTom = YearMonth.now().minusMonths(3),
                ),
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = søker,
                    stønadFom = YearMonth.now().minusMonths(2),
                    stønadTom = YearMonth.now().minusMonths(1),
                ),
            )

        every { endretUtbetalingAndelRepository.hentEndretUtbetalingerForBehandling(behandling.id) } returns
            listOf(
                // overlappende periode, kommer med andelTilkjentYtelse
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søkerPerson,
                    prosent = BigDecimal(100),
                    periodeFom = fom,
                    periodeTom = tom,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                ),
            )
        val personResultatForBarn1 = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val vilkårResultaterForBarn1 =
            lagVilkårResultaterForDeltBosted(
                personResultat = personResultatForBarn1,
                behandlingId = behandling.id,
                fom1 = fom.minusMonths(1).førsteDagIInneværendeMåned(),
                tom1 = tom.sisteDagIInneværendeMåned(),
            )
        personResultatForBarn1.setSortedVilkårResultater(vilkårResultaterForBarn1)
        vilkårsvurdering.personResultater = setOf(personResultatForBarn1)

        every { vilkårsvurderingRepository.finnAktivForBehandling(behandling.id) } returns vilkårsvurdering

        val endreteUtbetalinger =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id)
        assertTrue { endreteUtbetalinger.size == 1 }

        val endretUtbetalingAndel = endreteUtbetalinger[0].endretUtbetaling
        assertEquals(fom, endretUtbetalingAndel.fom)
        assertEquals(tom, endretUtbetalingAndel.tom)
        assertNotNull(endretUtbetalingAndel.søknadstidspunkt)

        val andelerTilkjentYtelse = endreteUtbetalinger[0].andelerTilkjentYtelse
        assertTrue { andelerTilkjentYtelse.size == 2 }

        assertTrue { andelerTilkjentYtelse.any { it.stønadFom == YearMonth.now().minusMonths(6) } }
        assertTrue { andelerTilkjentYtelse.any { it.stønadTom == YearMonth.now().minusMonths(3) } }
        assertTrue { andelerTilkjentYtelse.any { it.stønadFom == YearMonth.now().minusMonths(2) } }
        assertTrue { andelerTilkjentYtelse.any { it.stønadTom == YearMonth.now().minusMonths(1) } }
        assertTrue { andelerTilkjentYtelse.any { it.prosent == BigDecimal(100) } }
        assertTrue { andelerTilkjentYtelse.any { it.kalkulertUtbetalingsbeløp == maksBeløp() } }
    }
}
