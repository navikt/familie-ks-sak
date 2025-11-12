package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import io.mockk.mockk
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndel
import no.nav.familie.ks.sak.data.lagKompetanse
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.BehandlingsresultatEndringUtils.erEndringIBeløpForPerson
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.BehandlingsresultatEndringUtils.utledEndringsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.BehandlingsresultatOpphørUtils.utledOpphørsdatoForNåværendeBehandlingMedFallback
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

class BehandlingsresultatEndringUtilsTest {
    val søker = tilfeldigPerson()

    private val barn1Aktør = randomAktør()

    val jan22 = YearMonth.of(2022, 1)
    val mai22 = YearMonth.of(2022, 5)
    val aug22 = YearMonth.of(2022, 8)
    val des22 = YearMonth.of(2022, 12)

    @Test
    fun `utledEndringsresultat skal returnere INGEN_ENDRING dersom det ikke finnes noe endringer i behandling`() {
        val endringsresultat =
            utledEndringsresultat(
                nåværendeAndeler = emptyList(),
                forrigeAndeler = emptyList(),
                personerFremstiltKravFor = emptyList(),
                nåværendeKompetanser = emptyList(),
                forrigeKompetanser = emptyList(),
                nåværendePersonResultater = emptySet(),
                forrigePersonResultater = emptySet(),
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                personerIForrigeBehandling = emptySet(),
            )

        assertThat(endringsresultat, Is(Endringsresultat.INGEN_ENDRING))
    }

    @Test
    fun `utledEndringsresultat skal returnere ENDRING dersom det finnes endringer i beløp`() {
        val person = lagPerson(aktør = randomAktør())

        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = person.aktør,
            )

        val endringsresultat =
            utledEndringsresultat(
                forrigeAndeler = listOf(forrigeAndel),
                nåværendeAndeler = listOf(forrigeAndel.copy(kalkulertUtbetalingsbeløp = 40)),
                personerFremstiltKravFor = emptyList(),
                forrigeKompetanser = emptyList(),
                nåværendeKompetanser = emptyList(),
                nåværendePersonResultater = emptySet(),
                forrigePersonResultater = emptySet(),
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                personerIForrigeBehandling = setOf(person),
            )

        assertThat(endringsresultat, Is(Endringsresultat.ENDRING))
    }

    @Test
    fun `utledEndringsresultat skal returnere ENDRING dersom det finnes endringer i vilkårsvurderingen`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val barn = lagPerson(aktør = barn1Aktør, fødselsdato = fødselsdato)
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2015, 2),
                    tom = YearMonth.of(2020, 1),
                ),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2015, 2),
                    tom = YearMonth.of(2020, 1),
                ),
            )

        val forrigeVilkårResultater =
            listOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val nåværendeVilkårResultater =
            listOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                ),
            )

        val forrigePersonResultat =
            PersonResultat(
                id = 0,
                vilkårsvurdering = mockk(),
                aktør = barn1Aktør,
                vilkårResultater = forrigeVilkårResultater.toMutableSet(),
            )

        val nåværendePersonResultat =
            PersonResultat(
                id = 0,
                vilkårsvurdering = mockk(),
                aktør = barn1Aktør,
                vilkårResultater = nåværendeVilkårResultater.toMutableSet(),
            )

        val endringsresultat =
            utledEndringsresultat(
                forrigeAndeler = forrigeAndeler,
                nåværendeAndeler = nåværendeAndeler,
                personerFremstiltKravFor = emptyList(),
                forrigeKompetanser = emptyList(),
                nåværendeKompetanser = emptyList(),
                forrigePersonResultater = setOf(forrigePersonResultat),
                nåværendePersonResultater = setOf(nåværendePersonResultat),
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                personerIForrigeBehandling = setOf(barn),
            )

        assertThat(endringsresultat, Is(Endringsresultat.ENDRING))
    }

    @Test
    fun `utledEndringsresultat skal returnere ENDRING dersom det finnes endringer i kompetanse`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()

        val barnPerson = lagPerson(aktør = barn1Aktør)

        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endringsresultat =
            utledEndringsresultat(
                nåværendeAndeler = emptyList(),
                forrigeAndeler = emptyList(),
                personerFremstiltKravFor = emptyList(),
                forrigeKompetanser = listOf(forrigeKompetanse),
                nåværendeKompetanser =
                    listOf(
                        forrigeKompetanse
                            .copy(søkersAktivitet = KompetanseAktivitet.ARBEIDER_PÅ_NORSK_SOKKEL)
                            .apply { behandlingId = nåværendeBehandling.id },
                    ),
                nåværendePersonResultater = emptySet(),
                forrigePersonResultater = emptySet(),
                nåværendeEndretAndeler = emptyList(),
                forrigeEndretAndeler = emptyList(),
                personerIForrigeBehandling = setOf(barnPerson),
            )

        assertThat(endringsresultat, Is(Endringsresultat.ENDRING))
    }

    @Test
    fun `utledEndringsresultat skal returnere ENDRING dersom det finnes endringer i endret utbetaling andeler`() {
        val barn = lagPerson(aktør = barn1Aktør)

        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                behandlingId = 0,
                personer = setOf(barn),
                prosent = BigDecimal.ZERO,
                periodeFom = jan22,
                periodeTom = aug22,
                årsak = Årsak.ETTERBETALING_3MND,
            )

        val endringsresultat =
            utledEndringsresultat(
                nåværendeAndeler = emptyList(),
                forrigeAndeler = emptyList(),
                personerFremstiltKravFor = emptyList(),
                forrigeKompetanser = emptyList(),
                nåværendeKompetanser = emptyList(),
                nåværendePersonResultater = emptySet(),
                forrigePersonResultater = emptySet(),
                forrigeEndretAndeler = listOf(forrigeEndretAndel),
                nåværendeEndretAndeler = listOf(forrigeEndretAndel.copy(årsak = Årsak.ALLEREDE_UTBETALT)),
                personerIForrigeBehandling = setOf(barn),
            )

        assertThat(endringsresultat, Is(Endringsresultat.ENDRING))
    }

    @Test
    fun `Endring i beløp - Skal returnere false dersom eneste endring er opphør`() {
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = mai22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
            )

        val opphørstidspunktForBehandling =
            nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                forrigeAndelerIBehandling = forrigeAndeler,
                nåværendeEndretAndelerIBehandling = emptyList(),
                endretAndelerForForrigeBehandling = emptyList(),
            )

        val erEndringIBeløp =
            erEndringIBeløpForPerson(
                nåværendeAndelerForPerson = nåværendeAndeler,
                forrigeAndelerForPerson = forrigeAndeler,
                opphørstidspunktForBehandling = opphørstidspunktForBehandling!!,
                erFremstiltKravForPerson = false,
            )

        assertEquals(false, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere true når beløp i periode har gått fra større enn 0 til null og det er søkt for person`() {
        val barn2Aktør = randomAktør()
        val personerFramstiltKravFor = listOf(barn1Aktør)

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = mai22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val erEndringIBeløp =
            listOf(barn1Aktør, barn2Aktør).any { aktør ->
                val erFremstiltKravForPerson = personerFramstiltKravFor.contains(aktør)

                val opphørstidspunktForBehandling =
                    nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                        forrigeAndelerIBehandling = forrigeAndeler,
                        nåværendeEndretAndelerIBehandling = emptyList(),
                        endretAndelerForForrigeBehandling = emptyList(),
                    )

                val erEndringIBeløpForPerson =
                    erEndringIBeløpForPerson(
                        nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør },
                        forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør },
                        opphørstidspunktForBehandling = opphørstidspunktForBehandling!!,
                        erFremstiltKravForPerson = erFremstiltKravForPerson,
                    )

                erEndringIBeløpForPerson
            }

        assertEquals(true, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere false når beløp i periode har gått fra større enn 0 til at annet tall større enn 0 og det er søkt for person`() {
        val barn2Aktør = randomAktør()

        val personerFramstiltKravFor = listOf(barn1Aktør)

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = mai22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = mai22.plusMonths(1),
                    tom = aug22,
                    beløp = 527,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val erEndringIBeløp =
            listOf(barn1Aktør, barn2Aktør).any { aktør ->
                val erFremstiltKravForPerson = personerFramstiltKravFor.contains(aktør)

                val opphørstidspunktForBehandling =
                    nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                        forrigeAndelerIBehandling = forrigeAndeler,
                        nåværendeEndretAndelerIBehandling = emptyList(),
                        endretAndelerForForrigeBehandling = emptyList(),
                    )

                val erEndringIBeløpForPerson =
                    erEndringIBeløpForPerson(
                        nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør },
                        forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør },
                        opphørstidspunktForBehandling = opphørstidspunktForBehandling!!,
                        erFremstiltKravForPerson = erFremstiltKravForPerson,
                    )

                erEndringIBeløpForPerson
            }

        assertEquals(false, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere true når beløp i periode har gått fra null til et tall større enn 0 og det ikke er søkt for person`() {
        val barn2Aktør = randomAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = des22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val erEndringIBeløp =
            listOf(barn1Aktør, barn2Aktør).any { aktør ->
                val opphørstidspunktForBehandling =
                    nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                        forrigeAndelerIBehandling = forrigeAndeler,
                        nåværendeEndretAndelerIBehandling = emptyList(),
                        endretAndelerForForrigeBehandling = emptyList(),
                    )

                val erEndringIBeløpForPerson =
                    erEndringIBeløpForPerson(
                        nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør },
                        forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør },
                        opphørstidspunktForBehandling = opphørstidspunktForBehandling!!,
                        erFremstiltKravForPerson = false,
                    )

                erEndringIBeløpForPerson
            }

        assertEquals(true, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere false når beløp i periode har gått fra null til et tall større enn 0 og det er søkt for person`() {
        val barn2Aktør = randomAktør()

        val personerFramstiltKravFor = listOf(barn1Aktør)

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = des22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val erEndringIBeløp =
            listOf(barn1Aktør, barn2Aktør).any { aktør ->
                val erFremstiltKravForPerson = personerFramstiltKravFor.contains(aktør)

                val opphørstidspunktForBehandling =
                    nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                        forrigeAndelerIBehandling = forrigeAndeler,
                        nåværendeEndretAndelerIBehandling = emptyList(),
                        endretAndelerForForrigeBehandling = emptyList(),
                    )

                val erEndringIBeløpForPerson =
                    erEndringIBeløpForPerson(
                        nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør },
                        forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør },
                        opphørstidspunktForBehandling = opphørstidspunktForBehandling!!,
                        erFremstiltKravForPerson = erFremstiltKravForPerson,
                    )

                erEndringIBeløpForPerson
            }

        assertEquals(false, erEndringIBeløp)
    }

    @Test
    fun `Endring i beløp - Skal returnere true når beløp i periode har gått fra større enn 0 til at annet tall større enn 0 og det ikke er søkt for person`() {
        val barn2Aktør = randomAktør()

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = mai22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = mai22.plusMonths(1),
                    tom = aug22,
                    beløp = 527,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        val erEndringIBeløp =
            listOf(barn1Aktør, barn2Aktør).any { aktør ->

                val opphørstidspunktForBehandling =
                    nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                        forrigeAndelerIBehandling = forrigeAndeler,
                        nåværendeEndretAndelerIBehandling = emptyList(),
                        endretAndelerForForrigeBehandling = emptyList(),
                    )

                val erEndringIBeløpForPerson =
                    erEndringIBeløpForPerson(
                        nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør },
                        forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør },
                        opphørstidspunktForBehandling = opphørstidspunktForBehandling!!,
                        erFremstiltKravForPerson = false,
                    )

                erEndringIBeløpForPerson
            }

        assertEquals(true, erEndringIBeløp)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere true hvis årsak er endret`() {
        val barn = lagPerson(aktør = randomAktør())

        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                behandlingId = 0,
                personer = setOf(barn),
                prosent = BigDecimal.ZERO,
                periodeFom = jan22,
                periodeTom = aug22,
                årsak = Årsak.ETTERBETALING_3MND,
            )

        val erEndringIEndretAndeler =
            erEndringIEndretUtbetalingAndelerForPerson(
                forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                nåværendeEndretAndelerForPerson = listOf(forrigeEndretAndel.copy(årsak = Årsak.ALLEREDE_UTBETALT)),
            )

        assertTrue(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere false hvis prosent er endret`() {
        val barn = lagPerson(aktør = randomAktør())
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                behandlingId = 0,
                personer = setOf(barn),
                prosent = BigDecimal.ZERO,
                periodeFom = jan22,
                periodeTom = aug22,
                årsak = Årsak.ALLEREDE_UTBETALT,
            )

        val erEndringIEndretAndeler =
            erEndringIEndretUtbetalingAndelerForPerson(
                forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                nåværendeEndretAndelerForPerson = listOf(forrigeEndretAndel.copy(prosent = BigDecimal(100))),
            )

        assertFalse(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere true hvis eneste endring er at perioden blir lenger`() {
        val barn = lagPerson(aktør = randomAktør())
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                behandlingId = 0,
                personer = setOf(barn),
                prosent = BigDecimal.ZERO,
                periodeFom = jan22,
                periodeTom = aug22,
                årsak = Årsak.ALLEREDE_UTBETALT,
            )

        val erEndringIEndretAndeler =
            erEndringIEndretUtbetalingAndelerForPerson(
                forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                nåværendeEndretAndelerForPerson = listOf(forrigeEndretAndel.copy(tom = des22)),
            )

        assertTrue(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere true hvis endringsperiode oppstår i nåværende behandling`() {
        val barn = lagPerson(aktør = randomAktør())

        val nåværendeEndretAndel =
            lagEndretUtbetalingAndel(
                behandlingId = 0,
                personer = setOf(barn),
                prosent = BigDecimal.ZERO,
                periodeFom = jan22,
                periodeTom = aug22,
                årsak = Årsak.ALLEREDE_UTBETALT,
            )

        val erEndringIEndretAndeler =
            erEndringIEndretUtbetalingAndelerForPerson(
                forrigeEndretAndelerForPerson = emptyList(),
                nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
            )

        assertTrue(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere true hvis et av to barn har endring på årsak`() {
        val barn1 = lagPerson(aktør = randomAktør())
        val barn2 = lagPerson(aktør = randomAktør())

        val forrigeEndretAndelBarn1 =
            lagEndretUtbetalingAndel(
                personer = setOf(barn1),
                prosent = BigDecimal.ZERO,
                periodeFom = jan22,
                periodeTom = aug22,
                årsak = Årsak.ALLEREDE_UTBETALT,
            )

        val forrigeEndretAndelBarn2 =
            lagEndretUtbetalingAndel(
                personer = setOf(barn2),
                prosent = BigDecimal.ZERO,
                periodeFom = jan22,
                periodeTom = aug22,
                årsak = Årsak.ETTERBETALING_3MND,
            )

        val erEndringIEndretAndeler =
            listOf(barn1, barn2).any {
                erEndringIEndretUtbetalingAndelerForPerson(
                    forrigeEndretAndelerForPerson = listOf(forrigeEndretAndelBarn1, forrigeEndretAndelBarn2).filter { endretAndel -> endretAndel.personer.contains(it) },
                    nåværendeEndretAndelerForPerson = listOf(forrigeEndretAndelBarn1, forrigeEndretAndelBarn2.copy(årsak = Årsak.ALLEREDE_UTBETALT)).filter { endretAndel -> endretAndel.personer.contains(it) },
                )
            }

        assertTrue(erEndringIEndretAndeler)
    }

    @Test
    fun `Endring i kompetanse - skal returnere false når ingenting endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson = listOf(forrigeKompetanse.copy().apply { behandlingId = nåværendeBehandling.id }),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
            )

        assertEquals(false, endring)
    }

    @Test
    fun `Endring i kompetanse - skal returnere true når søkers aktivitetsland endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson =
                    listOf(
                        forrigeKompetanse.copy(søkersAktivitetsland = "DK").apply { behandlingId = nåværendeBehandling.id },
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
            )

        assertEquals(true, endring)
    }

    @Test
    fun `Endring i kompetanse - skal returnere true når søkers aktivitet endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson =
                    listOf(
                        forrigeKompetanse
                            .copy(søkersAktivitet = KompetanseAktivitet.ARBEIDER_PÅ_NORSK_SOKKEL)
                            .apply { behandlingId = nåværendeBehandling.id },
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
            )

        assertEquals(true, endring)
    }

    @Test
    fun `Endring i kompetanse - skal returnere true når annen forelders aktivitetsland endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson =
                    listOf(
                        forrigeKompetanse
                            .copy(annenForeldersAktivitetsland = "DK")
                            .apply { behandlingId = nåværendeBehandling.id },
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
            )

        assertEquals(true, endring)
    }

    @Test
    fun `Endring i kompetanse - skal returnere true når annen forelders aktivitet endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson =
                    listOf(
                        forrigeKompetanse
                            .copy(annenForeldersAktivitet = KompetanseAktivitet.FORSIKRET_I_BOSTEDSLAND)
                            .apply { behandlingId = nåværendeBehandling.id },
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
            )

        assertEquals(true, endring)
    }

    @Test
    fun `Endring i kompetanse - skal returnere true når barnets bostedsland endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson =
                    listOf(
                        forrigeKompetanse.copy(barnetsBostedsland = "DK").apply { behandlingId = nåværendeBehandling.id },
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
            )

        assertEquals(true, endring)
    }

    @Test
    fun `Endring i kompetanse - skal returnere true når resultat på kompetansen endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson =
                    listOf(
                        forrigeKompetanse
                            .copy(resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND)
                            .apply { behandlingId = nåværendeBehandling.id },
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
            )

        assertEquals(true, endring)
    }

    @Test
    fun `Endring i kompetanse - skal returnere false når det kun blir lagt på en ekstra kompetanseperiode`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val endring =
            erEndringIKompetanseForPerson(
                nåværendeKompetanserForPerson =
                    listOf(
                        forrigeKompetanse
                            .copy(fom = YearMonth.now().minusMonths(10))
                            .apply { behandlingId = nåværendeBehandling.id },
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
            )

        assertEquals(false, endring)
    }

    @Test
    fun `Endring i vilkårsvurdering - skal returnere false dersom vilkårresultatene er helt like`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.of(2020, 1, 2),
                    periodeTom = LocalDate.of(2022, 1, 1),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.of(2020, 1, 2),
                    periodeTom = LocalDate.of(2022, 1, 1),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val aktør = randomAktør()

        val erEndringIVilkårvurderingForPerson =
            erEndringIVilkårsvurderingForPerson(
                nåværendePersonResultat = lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør),
                forrigePersonResultat = lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør),
            )

        assertThat(erEndringIVilkårvurderingForPerson, Is(false))
    }

    @Test
    fun `Endring i vilkårsvurdering - skal returnere true dersom det har vært endringer i regelverk`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                ),
            )

        val aktør = randomAktør()

        val erEndringIVilkårvurderingForPerson =
            erEndringIVilkårsvurderingForPerson(
                nåværendePersonResultat = lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør),
                forrigePersonResultat = lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør),
            )

        assertThat(erEndringIVilkårvurderingForPerson, Is(true))
    }

    @Test
    fun `Endring i vilkårsvurdering - skal returnere true dersom det har vært endringer i utdypendevilkårsvurdering`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.VURDERING_ANNET_GRUNNLAG,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val aktør = randomAktør()

        val erEndringIVilkårvurderingForPerson =
            erEndringIVilkårsvurderingForPerson(
                nåværendePersonResultat = lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør),
                forrigePersonResultat = lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør),
            )

        assertThat(erEndringIVilkårvurderingForPerson, Is(true))
    }

    @Test
    fun `Endring i vilkårsvurdering - skal returnere false hvis det kun er opphørt`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = null,
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val aktør = randomAktør()

        val erEndringIVilkårvurderingForPerson =
            erEndringIVilkårsvurderingForPerson(
                nåværendePersonResultat = lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør),
                forrigePersonResultat = lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør),
            )

        assertThat(erEndringIVilkårvurderingForPerson, Is(false))
    }

    private fun lagPersonResultatFraVilkårResultater(
        vilkårResultater: Set<VilkårResultat>,
        aktør: Aktør,
    ): PersonResultat {
        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = lagBehandling(),
                resultat = Resultat.OPPFYLT,
                søkerAktør = randomAktør(),
            )
        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = aktør)

        personResultat.setSortedVilkårResultater(vilkårResultater)

        return personResultat
    }
}
