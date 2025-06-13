package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import io.mockk.clearAllMocks
import io.mockk.clearStaticMockk
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndel
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.BehandlingsresultatOpphørUtils.filtrerBortIrrelevanteAndeler
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.BehandlingsresultatOpphørUtils.hentOpphørsresultatPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.BehandlingsresultatOpphørUtils.utledOpphørsdatoForNåværendeBehandlingMedFallback
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.brev.lagPersonResultat
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehandlingsresultatOpphørUtilsTest {
    val søker = tilfeldigPerson()

    val for3mndSiden = YearMonth.now().minusMonths(3)
    val for2mndSiden = YearMonth.now().minusMonths(2)
    val for1mndSiden = YearMonth.now().minusMonths(1)
    val om1mnd = YearMonth.now().plusMonths(1)
    val om4mnd = YearMonth.now().plusMonths(4)

    @BeforeEach
    fun reset() {
        clearStaticMockk(YearMonth::class)
    }

    @AfterAll
    fun clearMocks() {
        clearAllMocks()
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere IKKE_OPPHØRT dersom nåværende andeler strekker seg lengre enn dagens dato`() {
        val barn1Aktør = randomAktør()
        val barn2Aktør = randomAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om1mnd,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om1mnd,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om1mnd,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om4mnd,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                nåværendePersonResultaterPåBarn = emptyList(),
                forrigePersonResultaterPåBarn = emptyList(),
                nåMåned = YearMonth.now(),
            )

        assertEquals(Opphørsresultat.IKKE_OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT dersom nåværende andeler opphører mens forrige andeler ikke opphører til og med dagens dato`() {
        val barn1Aktør = randomAktør()
        val barn2Aktør = randomAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om4mnd,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om4mnd,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                nåværendePersonResultaterPåBarn = emptyList(),
                forrigePersonResultaterPåBarn = emptyList(),
                nåMåned = YearMonth.now(),
            )

        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT dersom nåværende andeler opphører tidligere enn forrige andeler og dagens dato`() {
        val barn1Aktør = randomAktør()
        val barn2Aktør = randomAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                nåværendePersonResultaterPåBarn = emptyList(),
                forrigePersonResultaterPåBarn = emptyList(),
                nåMåned = YearMonth.now(),
            )

        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT dersom vi går fra andeler på person til fullt opphør på person`() {
        val barn1Aktør = randomAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
            )

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = emptyList(),
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                nåværendePersonResultaterPåBarn = emptyList(),
                forrigePersonResultaterPåBarn = emptyList(),
                nåMåned = YearMonth.now(),
            )

        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere FORTSATT_OPPHØRT dersom nåværende andeler har lik opphørsdato som forrige andeler`() {
        val barn1Aktør = randomAktør()
        val barn2Aktør = randomAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                nåværendePersonResultaterPåBarn = emptyList(),
                forrigePersonResultaterPåBarn = emptyList(),
                nåMåned = YearMonth.now(),
            )

        assertEquals(Opphørsresultat.FORTSATT_OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere IKKE_OPPHØRT dersom nåværende andeler har lik opphørsdato som forrige andeler men det er i fremtiden`() {
        val barn1Aktør = randomAktør()
        val barn2Aktør = randomAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om4mnd,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om4mnd,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for1mndSiden,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                nåværendePersonResultaterPåBarn = emptyList(),
                forrigePersonResultaterPåBarn = emptyList(),
                nåMåned = YearMonth.now(),
            )

        assertEquals(Opphørsresultat.IKKE_OPPHØRT, opphørsresultat)
    }

    @ParameterizedTest
    @EnumSource(Årsak::class, names = ["ALLEREDE_UTBETALT", "ETTERBETALING_3MND", "FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024"])
    internal fun `filtrerBortIrrelevanteAndeler - skal filtrere andeler som har 0 i beløp og endret utbetaling andel med årsak ALLEREDE_UTBETALT, FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024  eller ETTERBETALING_3ÅR`(årsak: Årsak) {
        val barn = lagPerson(aktør = randomAktør())
        val barnAktør = barn.aktør

        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 0,
                    aktør = barnAktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for1mndSiden,
                    tom = om1mnd,
                    beløp = 1400,
                    aktør = barnAktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = om4mnd,
                    tom = om4mnd,
                    beløp = 0,
                    aktør = barnAktør,
                ),
            )

        val endretUtBetalingAndeler =
            listOf(
                lagEndretUtbetalingAndel(
                    personer = setOf(barn),
                    prosent = BigDecimal.ZERO,
                    periodeFom = for3mndSiden,
                    periodeTom = for2mndSiden,
                    årsak = årsak,
                ),
                lagEndretUtbetalingAndel(
                    personer = setOf(barn),
                    prosent = BigDecimal.ZERO,
                    periodeFom = om4mnd,
                    periodeTom = om4mnd,
                    årsak = årsak,
                ),
            )

        val andelerEtterFiltrering = andeler.filtrerBortIrrelevanteAndeler(endretUtBetalingAndeler)

        assertEquals(andelerEtterFiltrering.minOf { it.stønadFom }, for1mndSiden)
        assertEquals(andelerEtterFiltrering.maxOf { it.stønadTom }, om1mnd)
    }

    @Test
    internal fun `filtrerBortIrrelevanteAndeler - skal ikke filtrere andeler som har 0 i beløp grunnet differanseberegning`() {
        val barn = lagPerson(aktør = randomAktør())
        val barnAktør = barn.aktør
        val søker = lagPerson(aktør = randomAktør())
        val søkerAktør = søker.aktør

        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 0,
                    differanseberegnetPeriodebeløp = 50,
                    aktør = søkerAktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for1mndSiden,
                    tom = om1mnd,
                    beløp = 0,
                    differanseberegnetPeriodebeløp = 50,
                    aktør = barnAktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = om4mnd,
                    tom = om4mnd,
                    beløp = 0,
                    differanseberegnetPeriodebeløp = 50,
                    aktør = barnAktør,
                ),
            )

        val andelerEtterFiltrering = andeler.filtrerBortIrrelevanteAndeler(endretAndeler = emptyList())

        assertEquals(andelerEtterFiltrering.minOf { it.stønadFom }, for3mndSiden)
        assertEquals(andelerEtterFiltrering.maxOf { it.stønadTom }, om4mnd)
    }

    @Test
    fun `utledOpphørsdatoForNåværendeBehandlingMedFallback - skal returnere null hvis det ikke finnes andeler i inneværende behandling og kun irrelevante nullutbetalinger i forrige behandling`() {
        val barn = lagPerson(aktør = randomAktør())

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 0,
                    prosent = BigDecimal.ZERO,
                    aktør = barn.aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for1mndSiden,
                    tom = om4mnd,
                    beløp = 0,
                    prosent = BigDecimal.ZERO,
                    aktør = barn.aktør,
                ),
            )

        val forrigeEndretAndeler =
            listOf(
                lagEndretUtbetalingAndel(
                    personer = setOf(barn),
                    prosent = BigDecimal.ZERO,
                    periodeFom = for3mndSiden,
                    periodeTom = for2mndSiden,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                ),
                lagEndretUtbetalingAndel(
                    personer = setOf(barn),
                    prosent = BigDecimal.ZERO,
                    periodeFom = for1mndSiden,
                    periodeTom = om4mnd,
                    årsak = Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
                ),
            )

        val opphørstidspunktInneværendeBehandling =
            emptyList<AndelTilkjentYtelse>().utledOpphørsdatoForNåværendeBehandlingMedFallback(
                forrigeAndelerIBehandling = forrigeAndeler,
                endretAndelerForForrigeBehandling = forrigeEndretAndeler,
                nåværendeEndretAndelerIBehandling = emptyList(),
            )

        assertNull(opphørstidspunktInneværendeBehandling)
    }

    @Test
    fun `utledOpphørsdatoForNåværendeBehandlingMedFallback - skal returnere tidligste fom på andeler i forrige behandling hvis det ikke finnes andeler i inneværende behandling`() {
        val barn = lagPerson(aktør = randomAktør())

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = for2mndSiden,
                    beløp = 0,
                    prosent = BigDecimal.ZERO,
                    aktør = barn.aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = for1mndSiden,
                    tom = om4mnd,
                    prosent = BigDecimal.ZERO,
                    aktør = barn.aktør,
                ),
            )

        val forrigeEndretAndeler =
            listOf(
                lagEndretUtbetalingAndel(
                    personer = setOf(barn),
                    prosent = BigDecimal.ZERO,
                    periodeFom = for3mndSiden,
                    periodeTom = for2mndSiden,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                ),
            )

        val opphørstidspunktInneværendeBehandling =
            emptyList<AndelTilkjentYtelse>().utledOpphørsdatoForNåværendeBehandlingMedFallback(
                forrigeAndelerIBehandling = forrigeAndeler,
                endretAndelerForForrigeBehandling = forrigeEndretAndeler,
                nåværendeEndretAndelerIBehandling = emptyList(),
            )

        assertEquals(for1mndSiden, opphørstidspunktInneværendeBehandling)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT bruker har krysset av for meldt barnehageplass på alle barn`() {
        val barn1 = lagPerson(aktør = randomAktør())
        val barn2 = lagPerson(aktør = randomAktør())

        val personResultatBarn1 =
            lagPersonResultat(
                barn1,
                overstyrendeVilkårResultater =
                    listOf(
                        VilkårResultat(
                            behandlingId = 0,
                            personResultat = null,
                            vilkårType = Vilkår.BARNEHAGEPLASS,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2023, 1, 2),
                            periodeTom = LocalDate.of(2050, 3, 4),
                            begrunnelse = "",
                            vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
                            søkerHarMeldtFraOmBarnehageplass = true,
                            antallTimer = BigDecimal(33),
                        ),
                    ),
            )

        val personResultatBarn2 =
            lagPersonResultat(
                barn2,
                overstyrendeVilkårResultater =
                    listOf(
                        VilkårResultat(
                            behandlingId = 0,
                            personResultat = null,
                            vilkårType = Vilkår.BARNEHAGEPLASS,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2023, 1, 2),
                            periodeTom = LocalDate.of(2050, 3, 4),
                            begrunnelse = "",
                            vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
                            søkerHarMeldtFraOmBarnehageplass = true,
                            antallTimer = BigDecimal(33),
                        ),
                    ),
            )

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = listOf(lagAndelTilkjentYtelse(aktør = barn1.aktør), lagAndelTilkjentYtelse(aktør = barn2.aktør)),
                forrigeAndeler = listOf(lagAndelTilkjentYtelse(aktør = barn1.aktør), lagAndelTilkjentYtelse(aktør = barn2.aktør)),
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                nåværendePersonResultaterPåBarn = listOf(personResultatBarn1, personResultatBarn2),
                forrigePersonResultaterPåBarn = listOf(),
                nåMåned = YearMonth.now(),
            )

        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere FORTSATT_OPPHØRT hvis bruker har krysset av for meldt barnehageplass på alle barn med løpende andeler i forrige og nåværende behandling`() {
        val barn1 = lagPerson(aktør = randomAktør())
        val barn2 = lagPerson(aktør = randomAktør())

        val personResultatBarn1 =
            lagPersonResultat(
                barn1,
                overstyrendeVilkårResultater =
                    listOf(
                        VilkårResultat(
                            behandlingId = 0,
                            personResultat = null,
                            vilkårType = Vilkår.BARNEHAGEPLASS,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2023, 1, 2),
                            periodeTom = LocalDate.of(2050, 3, 4),
                            begrunnelse = "",
                            vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
                            søkerHarMeldtFraOmBarnehageplass = true,
                            antallTimer = BigDecimal(33),
                        ),
                    ),
            )

        val personResultatBarn2 =
            lagPersonResultat(
                barn2,
                overstyrendeVilkårResultater =
                    listOf(
                        VilkårResultat(
                            behandlingId = 0,
                            personResultat = null,
                            vilkårType = Vilkår.BARNEHAGEPLASS,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2023, 1, 2),
                            periodeTom = LocalDate.of(2050, 3, 4),
                            begrunnelse = "",
                            vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
                            søkerHarMeldtFraOmBarnehageplass = true,
                            antallTimer = BigDecimal(33),
                        ),
                    ),
            )

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = listOf(lagAndelTilkjentYtelse(aktør = barn1.aktør), lagAndelTilkjentYtelse(aktør = barn2.aktør)),
                forrigeAndeler = listOf(lagAndelTilkjentYtelse(aktør = barn1.aktør), lagAndelTilkjentYtelse(aktør = barn2.aktør)),
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                nåværendePersonResultaterPåBarn = listOf(personResultatBarn1, personResultatBarn2),
                forrigePersonResultaterPåBarn = listOf(personResultatBarn1, personResultatBarn2),
                nåMåned = YearMonth.now(),
            )

        assertEquals(Opphørsresultat.FORTSATT_OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal ikke returnere OPPHØRT dersom ikke alle barn med løpende ytelse har blitt krysset av for meldt barnehageplass`() {
        val barn1 = lagPerson(aktør = randomAktør())
        val barn2 = lagPerson(aktør = randomAktør())

        val personResultatBarn1 =
            lagPersonResultat(
                barn1,
                overstyrendeVilkårResultater =
                    listOf(
                        VilkårResultat(
                            behandlingId = 0,
                            personResultat = null,
                            vilkårType = Vilkår.BARNEHAGEPLASS,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2023, 1, 2),
                            periodeTom = LocalDate.of(2050, 3, 4),
                            begrunnelse = "",
                            vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
                            søkerHarMeldtFraOmBarnehageplass = true,
                        ),
                    ),
            )

        val personResultatBarn2 =
            lagPersonResultat(
                barn2,
                overstyrendeVilkårResultater =
                    listOf(
                        VilkårResultat(
                            behandlingId = 0,
                            personResultat = null,
                            vilkårType = Vilkår.BARNEHAGEPLASS,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2023, 1, 2),
                            periodeTom = LocalDate.of(2050, 3, 4),
                            begrunnelse = "",
                            vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
                            søkerHarMeldtFraOmBarnehageplass = false,
                        ),
                    ),
            )

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = listOf(lagAndelTilkjentYtelse(aktør = barn2.aktør)),
                forrigeAndeler = emptyList(),
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                nåværendePersonResultaterPåBarn = listOf(personResultatBarn1, personResultatBarn2),
                forrigePersonResultaterPåBarn = listOf(),
                nåMåned = YearMonth.now(),
            )

        assertEquals(Opphørsresultat.IKKE_OPPHØRT, opphørsresultat)
    }

    @Test
    fun `hentOpphørsresultatPåBehandling skal returnere OPPHØRT dersom kontantstøtten opphører to måneder fram i tid`() {
        val barn1Aktør = randomAktør()

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = for3mndSiden,
                    tom = om1mnd,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
            )

        val opphørsresultat =
            hentOpphørsresultatPåBehandling(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = emptyList(),
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                nåværendePersonResultaterPåBarn = emptyList(),
                forrigePersonResultaterPåBarn = emptyList(),
                nåMåned = YearMonth.now(),
            )

        assertEquals(Opphørsresultat.OPPHØRT, opphørsresultat)
    }
}
