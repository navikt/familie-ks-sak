package no.nav.familie.ks.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import mockAdopsjonService
import no.nav.familie.ks.sak.common.util.NullablePeriode
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toLocalDate
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.cucumber.mocking.mockUnleashNextMedContextService
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonResultat
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
import no.nav.familie.ks.sak.kjerne.beregning.lovverkFebruar2025.LovverkFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.lovverkFørFebruar2025.LovverkFørFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndelRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.praksisendring.Praksisendring2024Service
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class TilkjentYtelseServiceTest {
    private val søker = randomAktør()

    private val barn1 = randomAktør("01012112345")

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

    private val maksBeløp = maksBeløp()

    private lateinit var vilkårsvurdering: Vilkårsvurdering

    private val beregnAndelTilkjentYtelseService: BeregnAndelTilkjentYtelseService =
        spyk(
            BeregnAndelTilkjentYtelseService(
                andelGeneratorLookup = AndelGenerator.Lookup(listOf(LovverkFebruar2025AndelGenerator(), LovverkFørFebruar2025AndelGenerator())),
                adopsjonService = mockAdopsjonService(),
            ),
        )

    private val overgangsordningAndelRepositoryMock: OvergangsordningAndelRepository = mockk()
    private val praksisendring2024Service: Praksisendring2024Service = mockk()
    private val unleashNextMedContextServiceMock: UnleashNextMedContextService = mockUnleashNextMedContextService()

    private val tilkjentYtelseService = TilkjentYtelseService(beregnAndelTilkjentYtelseService, overgangsordningAndelRepositoryMock, praksisendring2024Service, unleashNextMedContextServiceMock)

    @BeforeEach
    fun init() {
        vilkårsvurdering =
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
                søkerPeriodeFom = LocalDate.of(1987, 1, 1),
                søkerPeriodeTom = null,
            )

        every { overgangsordningAndelRepositoryMock.hentOvergangsordningAndelerForBehandling(any()) } returns emptyList()
        every { praksisendring2024Service.genererAndelerForPraksisendring2024(any(), any(), any()) } returns emptyList()
    }

    @Test
    fun `beregnTilkjentYtelse skal generere overgangsordningAndeler`() {
        // arrange
        every { overgangsordningAndelRepositoryMock.hentOvergangsordningAndelerForBehandling(any()) } returns
            listOf(
                OvergangsordningAndel(
                    id = 1,
                    behandlingId = behandling.id,
                    person = barnPerson,
                    antallTimer = BigDecimal.ZERO,
                    deltBosted = false,
                    fom = YearMonth.of(2024, 9),
                    tom = YearMonth.of(2024, 10),
                ),
                OvergangsordningAndel(
                    id = 2,
                    behandlingId = behandling.id,
                    person = barnPerson,
                    antallTimer = BigDecimal.ZERO,
                    deltBosted = true,
                    fom = YearMonth.of(2024, 12),
                    tom = YearMonth.of(2025, 1),
                ),
            )

        vilkårsvurdering.personResultater +=
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn1,
                resultat = Resultat.OPPFYLT,
                periodeFom = barnPerson.fødselsdato.plusYears(1),
                periodeTom = barnPerson.fødselsdato.plusYears(2),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
            )

        // act
        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )

        // assert
        assertTilkjentYtelse(tilkjentYtelse, 3)

        val søkersAndeler = tilkjentYtelse.andelerTilkjentYtelse.filter { it.aktør == barn1 }
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = søkersAndeler.first { it.stønadFom == YearMonth.of(2024, 9) },
            prosent = BigDecimal(100),
            periodeFom = YearMonth.of(2024, 9).toLocalDate(),
            periodeTom = YearMonth.of(2024, 10).toLocalDate(),
            type = YtelseType.OVERGANGSORDNING,
        )
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = søkersAndeler.first { it.stønadFom == YearMonth.of(2024, 12) },
            prosent = BigDecimal(50),
            periodeFom = YearMonth.of(2024, 12).toLocalDate(),
            periodeTom = YearMonth.of(2025, 1).toLocalDate(),
            type = YtelseType.OVERGANGSORDNING,
        )
    }

    @Test
    fun `beregnTilkjentYtelse skal legge til praksisendringsandeler som blir generert`() {
        // Arrange
        every { praksisendring2024Service.genererAndelerForPraksisendring2024(any(), any(), any()) } returns
            listOf(
                lagAndelTilkjentYtelse(aktør = barn1, fom = YearMonth.of(2024, 9), tom = YearMonth.of(2024, 9), ytelseType = YtelseType.PRAKSISENDRING_2024),
            )

        vilkårsvurdering.personResultater +=
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn1,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = barnPerson.fødselsdato.plusYears(1),
                periodeTom = barnPerson.fødselsdato.plusYears(2),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
            )

        // Act
        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )

        // Assert
        assertTilkjentYtelse(tilkjentYtelse, 1)

        val søkersAndeler = tilkjentYtelse.andelerTilkjentYtelse.filter { it.aktør == barn1 }

        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = søkersAndeler.first { it.stønadFom == YearMonth.of(2024, 9) },
            prosent = BigDecimal(100),
            periodeFom = YearMonth.of(2024, 9).toLocalDate(),
            periodeTom = YearMonth.of(2024, 9).toLocalDate(),
            type = YtelseType.PRAKSISENDRING_2024,
        )
    }

    @Test
    fun `beregnTilkjentYtelse skal legge sammen praksisendringsandeler og ordinære andeler som ikke overlapper`() {
        // Arrange
        every { praksisendring2024Service.genererAndelerForPraksisendring2024(any(), any(), any()) } returns
            listOf(
                lagAndelTilkjentYtelse(aktør = barn1, fom = YearMonth.of(2024, 9), tom = YearMonth.of(2024, 9), ytelseType = YtelseType.PRAKSISENDRING_2024),
            )

        every { beregnAndelTilkjentYtelseService.beregnAndelerTilkjentYtelse(any(), any(), any()) } returns
            listOf(
                lagAndelTilkjentYtelse(aktør = barn1, fom = YearMonth.of(2024, 6), tom = YearMonth.of(2024, 8), ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE),
            )

        // Act
        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )

        // Assert
        assertTilkjentYtelse(tilkjentYtelse, 2)

        val barn1Andeler = tilkjentYtelse.andelerTilkjentYtelse.filter { it.aktør == barn1 }

        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = barn1Andeler.first { it.stønadFom == YearMonth.of(2024, 9) },
            prosent = BigDecimal(100),
            periodeFom = YearMonth.of(2024, 9).toLocalDate(),
            periodeTom = YearMonth.of(2024, 9).toLocalDate(),
            type = YtelseType.PRAKSISENDRING_2024,
        )

        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = barn1Andeler.first { it.stønadFom == YearMonth.of(2024, 6) },
            prosent = BigDecimal(100),
            periodeFom = YearMonth.of(2024, 6).toLocalDate(),
            periodeTom = YearMonth.of(2024, 8).toLocalDate(),
            type = YtelseType.ORDINÆR_KONTANTSTØTTE,
        )
    }

    @Test
    fun `beregnTilkjentYtelse skal erstatte ordinære andeler med praksisendring andeler hvis det finnes begge i samme periode`() {
        // Arrange
        every { praksisendring2024Service.genererAndelerForPraksisendring2024(any(), any(), any()) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    aktør = barn1,
                    fom = YearMonth.of(2024, 9),
                    tom = YearMonth.of(2024, 9),
                    ytelseType = YtelseType.PRAKSISENDRING_2024,
                    prosent = BigDecimal(100),
                ),
            )

        every { beregnAndelTilkjentYtelseService.beregnAndelerTilkjentYtelse(any(), any(), any()) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    aktør = barn1,
                    fom = YearMonth.of(2024, 9),
                    tom = YearMonth.of(2024, 9),
                    ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                    prosent = BigDecimal(50),
                    beløp = 3750,
                ),
            )

        // Act
        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )

        // Assert
        assertTilkjentYtelse(tilkjentYtelse, 1)

        val barnAndeler = tilkjentYtelse.andelerTilkjentYtelse.filter { it.aktør == barn1 }

        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = barnAndeler.first { it.stønadFom == YearMonth.of(2024, 9) },
            prosent = BigDecimal(100),
            periodeFom = YearMonth.of(2024, 9).toLocalDate(),
            periodeTom = YearMonth.of(2024, 9).toLocalDate(),
            type = YtelseType.PRAKSISENDRING_2024,
        )
    }

    @Test
    fun `beregnTilkjentYtelse skal erstatte ordinære andeler med praksisendring andeler og lage riktig splitt hvis ordinær andel strekker seg over flere måneder og overlapper med praksisendring `() {
        // arrange
        every { praksisendring2024Service.genererAndelerForPraksisendring2024(any(), any(), any()) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    aktør = barn1,
                    fom = YearMonth.of(2024, 9),
                    tom = YearMonth.of(2024, 9),
                    ytelseType = YtelseType.PRAKSISENDRING_2024,
                    prosent = BigDecimal(100),
                ),
            )

        every { beregnAndelTilkjentYtelseService.beregnAndelerTilkjentYtelse(any(), any(), any()) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    aktør = barn1,
                    fom = YearMonth.of(2024, 9),
                    tom = YearMonth.of(2024, 12),
                    ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                    prosent = BigDecimal(50),
                    beløp = 3750,
                ),
            )

        // act
        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )

        // assert
        assertTilkjentYtelse(tilkjentYtelse, 2)

        val barnAndeler = tilkjentYtelse.andelerTilkjentYtelse.filter { it.aktør == barn1 }

        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = barnAndeler.first { it.stønadFom == YearMonth.of(2024, 9) },
            prosent = BigDecimal(100),
            periodeFom = YearMonth.of(2024, 9).toLocalDate(),
            periodeTom = YearMonth.of(2024, 9).toLocalDate(),
            type = YtelseType.PRAKSISENDRING_2024,
        )

        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = barnAndeler.first { it.stønadFom == YearMonth.of(2024, 10) },
            prosent = BigDecimal(50),
            periodeFom = YearMonth.of(2024, 10).toLocalDate(),
            periodeTom = YearMonth.of(2024, 12).toLocalDate(),
            type = YtelseType.ORDINÆR_KONTANTSTØTTE,
        )
    }

    @Test
    fun `beregnTilkjentYtelse tar ikke med tom overgangsordningandel`() {
        // arrange
        every { overgangsordningAndelRepositoryMock.hentOvergangsordningAndelerForBehandling(any()) } returns
            listOf(
                OvergangsordningAndel(
                    id = 1,
                    behandlingId = behandling.id,
                ),
            )

        vilkårsvurdering.personResultater +=
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn1,
                resultat = Resultat.OPPFYLT,
                periodeFom = barnPerson.fødselsdato.plusYears(1),
                periodeTom = barnPerson.fødselsdato.plusYears(2),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
            )

        // act
        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )

        // assert
        assertTrue(tilkjentYtelse.andelerTilkjentYtelse.none { it.type == YtelseType.OVERGANGSORDNING })
    }

    @Test
    fun `beregnTilkjentYtelse skal beregne TY for et barn uten barnehageplass`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val barnFødselsdato = barnPerson.fødselsdato
        val fom = barnFødselsdato.plusYears(1)
        val tom = barnFødselsdato.plusYears(2)

        // antallTimer null betyr at barn ikke har fått barnehageplass. Da får barn full KS
        val barnehagePlassPeriodeMedAntallTimer = NullablePeriode(fom = fom, tom = tom) to null

        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = personResultatForBarn,
                barnFødselsdato = barnFødselsdato,
                barnehageplassPerioder = listOf(barnehagePlassPeriodeMedAntallTimer),
                behandlingId = behandling.id,
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )
        assertTilkjentYtelse(tilkjentYtelse, 1)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(100),
            periodeFom = fom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = tom.minusMonths(1).sisteDagIMåned(),
        )
    }

    @Test
    fun `beregnTilkjentYtelse skal beregne TY for et barn med 8 timer barnehageplass`() {
        val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
        val barnFødselsdato = barnPerson.fødselsdato
        val fom = barnFødselsdato.plusYears(1)
        val tom = barnFødselsdato.plusYears(2)

        val barnehagePlassPeriodeMedAntallTimer = NullablePeriode(fom = fom, tom = tom) to BigDecimal(8)

        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = personResultatForBarn,
                barnFødselsdato = barnFødselsdato,
                barnehageplassPerioder = listOf(barnehagePlassPeriodeMedAntallTimer),
                behandlingId = behandling.id,
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )
        assertTilkjentYtelse(tilkjentYtelse, 1)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(80),
            periodeFom = fom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = tom.minusMonths(1).sisteDagIMåned(),
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

        val barnehagePlassPerioderMedAntallTimer =
            listOf(
                NullablePeriode(førstePeriodeFom, førstePeriodeTom) to BigDecimal(8),
                NullablePeriode(andrePeriodeFom, andrePeriodeTom) to null,
            )

        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = personResultatForBarn,
                barnFødselsdato = barnFødselsdato,
                barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
                behandlingId = behandling.id,
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )
        assertTilkjentYtelse(tilkjentYtelse, 2)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(80),
            periodeFom = førstePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = førstePeriodeTom.sisteDagIMåned(),
        )
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.last(),
            prosent = BigDecimal(100),
            periodeFom = andrePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = barnFødselsdato.plusYears(2).minusMonths(1).sisteDagIMåned(),
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

        val barnehagePlassPerioderMedAntallTimer =
            listOf(
                NullablePeriode(førstePeriodeFom, førstePeriodeTom) to BigDecimal(8),
                NullablePeriode(andrePeriodeFom, andrePeriodeTom) to BigDecimal(17),
            )

        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = personResultatForBarn,
                barnFødselsdato = barnFødselsdato,
                barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
                behandlingId = behandling.id,
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )
        assertTilkjentYtelse(tilkjentYtelse, 2)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(80),
            periodeFom = førstePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = førstePeriodeTom.minusMonths(1).sisteDagIMåned(),
        )
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.last(),
            prosent = BigDecimal(40),
            periodeFom = andrePeriodeFom.førsteDagIInneværendeMåned(),
            periodeTom = barnFødselsdato.plusYears(2).minusMonths(1).sisteDagIMåned(),
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

        val barnehagePlassPerioderMedAntallTimer =
            listOf(
                NullablePeriode(førstePeriodeFom, førstePeriodeTom) to null,
                NullablePeriode(andrePeriodeFom, andrePeriodeTom) to BigDecimal(8),
            )

        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = personResultatForBarn,
                barnFødselsdato = barnFødselsdato,
                barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
                behandlingId = behandling.id,
            )
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )
        assertTilkjentYtelse(tilkjentYtelse, 2)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(100),
            periodeFom = førstePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = førstePeriodeTom.minusMonths(1).sisteDagIMåned(),
        )
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.last(),
            prosent = BigDecimal(80),
            periodeFom = andrePeriodeFom.førsteDagIInneværendeMåned(),
            periodeTom = barnFødselsdato.plusYears(2).minusMonths(1).sisteDagIMåned(),
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
        val barnehagePlassPerioderMedAntallTimer =
            listOf(
                NullablePeriode(førstePeriodeFom, førstePeriodeTom) to BigDecimal(8),
            )

        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = personResultatForBarn,
                barnFødselsdato = barnFødselsdato,
                barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
                behandlingId = behandling.id,
            ).toMutableSet()
        // full barnehageplass vilkår
        val fullBarnehageplassVilkår =
            VilkårResultat(
                personResultat = personResultatForBarn,
                vilkårType = Vilkår.BARNEHAGEPLASS,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = andrePeriodeFom,
                periodeTom = andrePeriodeTom,
                begrunnelse = "",
                behandlingId = behandling.id,
                antallTimer = BigDecimal(33),
            )
        vilkårResultaterForBarn.add(fullBarnehageplassVilkår)
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )
        assertTilkjentYtelse(tilkjentYtelse, 1)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(80),
            periodeFom = førstePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = førstePeriodeTom.minusMonths(1).sisteDagIMåned(),
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
        val barnehagePlassPerioderMedAntallTimer =
            listOf(
                NullablePeriode(andrePeriodeFom, andrePeriodeTom) to BigDecimal(8),
            )

        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = personResultatForBarn,
                barnFødselsdato = barnFødselsdato,
                barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
                behandlingId = behandling.id,
            ).toMutableSet()
        // full barnehageplass vilkår
        val fullBarnehageplassVilkår =
            VilkårResultat(
                personResultat = personResultatForBarn,
                vilkårType = Vilkår.BARNEHAGEPLASS,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = førstePeriodeFom,
                periodeTom = førstePeriodeTom,
                begrunnelse = "",
                behandlingId = behandling.id,
                antallTimer = BigDecimal(33),
            )
        vilkårResultaterForBarn.add(fullBarnehageplassVilkår)
        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )
        assertTilkjentYtelse(tilkjentYtelse, 1)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(80),
            periodeFom = andrePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = barnFødselsdato.plusYears(2).minusMonths(1).sisteDagIMåned(),
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
        val barnehagePlassPerioderMedAntallTimer =
            listOf(
                NullablePeriode(førstePeriodeFom, førstePeriodeTom) to BigDecimal(17),
                NullablePeriode(andrePeriodeFom, andrePeriodeTom) to BigDecimal(8),
            )

        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = personResultatForBarn,
                barnFødselsdato = barnFødselsdato,
                barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
                behandlingId = behandling.id,
            )

        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )
        assertTilkjentYtelse(tilkjentYtelse, 2)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(40),
            periodeFom = førstePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = førstePeriodeTom.sisteDagIMåned(),
        )
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.last(),
            prosent = BigDecimal(80),
            periodeFom = andrePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = barnFødselsdato.plusYears(2).minusMonths(1).sisteDagIMåned(),
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
        val barnehagePlassPerioderMedAntallTimer =
            listOf(
                NullablePeriode(førstePeriodeFom, førstePeriodeTom) to BigDecimal(17),
                NullablePeriode(andrePeriodeFom, andrePeriodeTom) to BigDecimal(8),
            )

        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = personResultatForBarn,
                barnFødselsdato = barnFødselsdato,
                barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
                behandlingId = behandling.id,
            )

        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )
        assertTilkjentYtelse(tilkjentYtelse, 2)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(40),
            periodeFom = førstePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = førstePeriodeTom,
        )
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.last(),
            prosent = BigDecimal(80),
            periodeFom = andrePeriodeFom,
            periodeTom = barnFødselsdato.plusYears(2).minusMonths(1).sisteDagIMåned(),
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
        val barnehagePlassPerioderMedAntallTimer =
            listOf(
                NullablePeriode(førstePeriodeFom, førstePeriodeTom) to BigDecimal(8),
                NullablePeriode(andrePeriodeFom, andrePeriodeTom) to BigDecimal(17),
            )

        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = personResultatForBarn,
                barnFødselsdato = barnFødselsdato,
                barnehageplassPerioder = barnehagePlassPerioderMedAntallTimer,
                behandlingId = behandling.id,
            )

        personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater += personResultatForBarn

        val tilkjentYtelse =
            tilkjentYtelseService.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
            )
        assertTilkjentYtelse(tilkjentYtelse, 2)
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first(),
            prosent = BigDecimal(80),
            periodeFom = førstePeriodeFom.plusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = førstePeriodeTom.sisteDagIMåned(),
        )
        assertAndelTilkjentYtelse(
            andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.last(),
            prosent = BigDecimal(40),
            periodeFom = andrePeriodeFom.førsteDagIInneværendeMåned(),
            periodeTom = barnFødselsdato.plusYears(2).minusMonths(1).sisteDagIMåned(),
        )
    }

    private fun assertTilkjentYtelse(
        tilkjentYtelse: TilkjentYtelse,
        antallAndeler: Int,
    ) {
        assertEquals(LocalDate.now(), tilkjentYtelse.opprettetDato)
        assertEquals(LocalDate.now(), tilkjentYtelse.endretDato)
        assertTrue { tilkjentYtelse.andelerTilkjentYtelse.isNotEmpty() && tilkjentYtelse.andelerTilkjentYtelse.size == antallAndeler }
    }

    private fun assertAndelTilkjentYtelse(
        andelTilkjentYtelse: AndelTilkjentYtelse,
        prosent: BigDecimal,
        periodeFom: LocalDate,
        periodeTom: LocalDate,
        type: YtelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
    ) {
        assertEquals(barn1, andelTilkjentYtelse.aktør)
        assertEquals(type, andelTilkjentYtelse.type)
        assertEquals(prosent, andelTilkjentYtelse.prosent)
        assertEquals(periodeFom.toYearMonth(), andelTilkjentYtelse.stønadFom)
        assertEquals(periodeTom.toYearMonth(), andelTilkjentYtelse.stønadTom)
        assertEquals(maksBeløp.prosent(prosent), andelTilkjentYtelse.kalkulertUtbetalingsbeløp)
        assertEquals(maksBeløp.prosent(prosent), andelTilkjentYtelse.nasjonaltPeriodebeløp)
    }
}
