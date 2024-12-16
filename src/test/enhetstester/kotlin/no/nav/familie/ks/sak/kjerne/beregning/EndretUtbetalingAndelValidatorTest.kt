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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class EndretUtbetalingAndelValidatorTest {
    private val søker = randomAktør()
    private val barn1 = randomAktør()

    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

    private val personopplysningGrunnlag =
        lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer(),
            barnasIdenter = listOf(barn1.aktivFødselsnummer()),
        )
    private val søkerPerson =
        lagPerson(
            personopplysningGrunnlag,
            søker,
            PersonType.SØKER,
        )
    private val barnPerson =
        lagPerson(
            personopplysningGrunnlag,
            barn1,
            PersonType.BARN,
        )

    @Nested
    inner class ValiderPeriodeInnenforTilkjentytelseTest {
        @Test
        fun `skal kaste feil når EndretUtbetaling periode slutter etter ty perioder`() {
            // Arrange
            val andelTilkjentYtelse =
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = søker,
                    stønadFom = YearMonth.now().minusMonths(1),
                    stønadTom = YearMonth.now().plusMonths(5),
                )

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søkerPerson,
                    prosent = BigDecimal(50),
                    periodeFom = YearMonth.now().minusMonths(1),
                    periodeTom = YearMonth.now().plusMonths(7),
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    EndretUtbetalingAndelValidator.validerPeriodeInnenforTilkjentYtelse(
                        endretUtbetalingAndel,
                        listOf(andelTilkjentYtelse),
                    )
                }
            assertThat(exception.message).isEqualTo(
                "Det er ingen tilkjent ytelse for personen det blir forsøkt lagt til en endret periode for.",
            )
            assertThat(exception.frontendFeilmelding).isEqualTo(
                "Du har valgt en periode der det ikke finnes tilkjent ytelse for valgt person i hele eller deler av perioden.",
            )
        }

        @Test
        fun `skal kaste feil når EndretUtbetaling periode starter før ty perioder`() {
            // Arrange
            val andelTilkjentYtelse =
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = søker,
                    stønadFom = YearMonth.now().minusMonths(1),
                    stønadTom = YearMonth.now().plusMonths(5),
                )

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søkerPerson,
                    prosent = BigDecimal(50),
                    periodeFom = YearMonth.now().minusMonths(2),
                    periodeTom = YearMonth.now().plusMonths(5),
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    EndretUtbetalingAndelValidator.validerPeriodeInnenforTilkjentYtelse(
                        endretUtbetalingAndel,
                        listOf(andelTilkjentYtelse),
                    )
                }
            assertThat(exception.message).isEqualTo(
                "Det er ingen tilkjent ytelse for personen det blir forsøkt lagt til en endret periode for.",
            )
            assertThat(exception.frontendFeilmelding).isEqualTo(
                "Du har valgt en periode der det ikke finnes tilkjent ytelse for valgt person i hele eller deler av perioden.",
            )
        }

        @Test
        fun `skal kaste feil når EndretUtbetaling periode ikke finnes for person`() {
            // Arrange
            val andelTilkjentYtelse =
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn1,
                    stønadFom = YearMonth.now().minusMonths(1),
                    stønadTom = YearMonth.now().plusMonths(5),
                )

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søkerPerson,
                    prosent = BigDecimal(50),
                    periodeFom = YearMonth.now().minusMonths(1),
                    periodeTom = YearMonth.now().plusMonths(5),
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    EndretUtbetalingAndelValidator.validerPeriodeInnenforTilkjentYtelse(
                        endretUtbetalingAndel,
                        listOf(andelTilkjentYtelse),
                    )
                }
            assertThat(exception.message).isEqualTo(
                "Det er ingen tilkjent ytelse for personen det blir forsøkt lagt til en endret periode for.",
            )
            assertThat(exception.frontendFeilmelding).isEqualTo(
                "Du har valgt en periode der det ikke finnes tilkjent ytelse for valgt person i hele eller deler av perioden.",
            )
        }

        @Test
        fun `validerPeriodeInnenforTilkjentytelse skal ikke kaste feil når EndretUtbetaling periode er innefor ty periode`() {
            val andelTilkjentYtelse =
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = søker,
                    stønadFom = YearMonth.now().minusMonths(2),
                    stønadTom = YearMonth.now().plusMonths(5),
                )
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søkerPerson,
                    prosent = BigDecimal(50),
                    periodeFom = YearMonth.now().minusMonths(1),
                    periodeTom = YearMonth.now().plusMonths(4),
                )

            assertDoesNotThrow {
                EndretUtbetalingAndelValidator.validerPeriodeInnenforTilkjentYtelse(
                    endretUtbetalingAndel,
                    listOf(andelTilkjentYtelse),
                )
            }
        }
    }

    @Nested
    inner class FinnDeltBostedPerioderTest {
        @Test
        fun `skal finne riktige delt bosted perioder for barn, og slå sammen de som er sammenhengende`() {
            // Arrange
            val fom = LocalDate.now().minusMonths(5)
            val tom = LocalDate.now().plusMonths(7)

            val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
            val vilkårResultaterForBarn =
                lagVilkårResultaterForDeltBosted(
                    personResultat = personResultatForBarn,
                    behandlingId = behandling.id,
                    fom1 = fom,
                    tom1 = tom,
                )
            personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
            vilkårsvurdering.personResultater = setOf(personResultatForBarn)

            // Act
            val deltBostedPerioder =
                EndretUtbetalingAndelValidator.finnDeltBostedPerioder(
                    person = barnPerson,
                    vilkårsvurdering = vilkårsvurdering,
                )

            // Assert
            assertThat(deltBostedPerioder).hasSize(1)
            assertThat(deltBostedPerioder.single().fom).isEqualTo(fom.plusMonths(1).førsteDagIInneværendeMåned())
            assertThat(deltBostedPerioder.single().tom).isEqualTo(tom.sisteDagIMåned())
        }

        @Test
        fun `skal finne riktige delt bosted perioder for barn, og ikke slå sammen når de ikke er sammenhengende`() {
            // Arrange
            val fom1 = LocalDate.now().minusMonths(5)
            val tom1 = LocalDate.now().minusMonths(2)
            val fom2 = LocalDate.now()
            val tom2 = LocalDate.now().plusMonths(7)

            val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)

            val vilkårResultaterForBarn =
                lagVilkårResultaterForDeltBosted(
                    personResultat = personResultatForBarn,
                    behandlingId = behandling.id,
                    fom1 = fom1,
                    tom1 = tom1,
                    fom2 = fom2,
                    tom2 = tom2,
                )

            personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
            vilkårsvurdering.personResultater = setOf(personResultatForBarn)

            // Act
            val deltBostedPerioder =
                EndretUtbetalingAndelValidator.finnDeltBostedPerioder(
                    person = barnPerson,
                    vilkårsvurdering = vilkårsvurdering,
                )

            // Assert
            assertThat(deltBostedPerioder).hasSize(2)
            assertThat(deltBostedPerioder[0].fom).isEqualTo(fom1.plusMonths(1).førsteDagIInneværendeMåned())
            assertThat(deltBostedPerioder[0].tom).isEqualTo(tom1.sisteDagIMåned())
            assertThat(deltBostedPerioder[1].fom).isEqualTo(fom2.plusMonths(1).førsteDagIInneværendeMåned())
            assertThat(deltBostedPerioder[1].tom).isEqualTo(tom2.sisteDagIMåned())
        }

        @Test
        fun `skal finne riktige delt bosted perioder for søker, og slå sammen de som er sammenhengende`() {
            // Arrange
            val fomBarn1 = LocalDate.now().minusMonths(5)
            val tomBarn1 = LocalDate.now().plusMonths(7)
            val fomBarn2 = fomBarn1.minusMonths(5)

            val barn2 = randomAktør()
            val personResultatForBarn1 = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
            val personResultatForBarn2 = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn2)

            val vilkårResultaterForBarn1 =
                lagVilkårResultaterForDeltBosted(
                    personResultat = personResultatForBarn1,
                    behandlingId = behandling.id,
                    fom1 = fomBarn1,
                    tom1 = LocalDate.now().minusMonths(1).sisteDagIMåned(),
                    fom2 = LocalDate.now().førsteDagIInneværendeMåned(),
                    tom2 = tomBarn1,
                )
            val vilkårResultaterForBarn2 =
                lagVilkårResultaterForDeltBosted(
                    personResultat = personResultatForBarn2,
                    behandlingId = behandling.id,
                    fom1 = fomBarn2,
                    // sammenhengde periode med første barn vilkår resultat
                    tom1 = fomBarn1,
                )

            personResultatForBarn1.setSortedVilkårResultater(vilkårResultaterForBarn1)
            personResultatForBarn2.setSortedVilkårResultater(vilkårResultaterForBarn2)

            vilkårsvurdering.personResultater = setOf(personResultatForBarn1, personResultatForBarn2)

            // Act
            val deltBostedPerioder =
                EndretUtbetalingAndelValidator.finnDeltBostedPerioder(
                    person = søkerPerson,
                    vilkårsvurdering = vilkårsvurdering,
                )

            // Assert
            assertThat(deltBostedPerioder).hasSize(1)
            assertThat(deltBostedPerioder.single().fom).isEqualTo(fomBarn2.plusMonths(1).førsteDagIInneværendeMåned())
            assertThat(deltBostedPerioder.single().tom).isEqualTo(tomBarn1.sisteDagIMåned())
        }

        @Test
        fun `finnDeltBostedPerioder Skal finne riktige delt bosted perioder for søker, og slå sammen de som overlapper`() {
            // Arrange
            val fomBarn1 = LocalDate.now().minusMonths(5)
            val tomBarn1 = LocalDate.now().plusMonths(7)
            val fomBarn2 = fomBarn1.minusMonths(5)

            val barn2 = randomAktør()
            val personResultatForBarn1 = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
            val personResultatForBarn2 = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn2)

            val vilkårResultaterForBarn1 =
                lagVilkårResultaterForDeltBosted(
                    personResultat = personResultatForBarn1,
                    behandlingId = behandling.id,
                    fom1 = fomBarn1,
                    tom1 = LocalDate.now().minusMonths(1).sisteDagIMåned(),
                    fom2 = LocalDate.now().førsteDagIInneværendeMåned(),
                    tom2 = tomBarn1,
                )
            val vilkårResultaterForBarn2 =
                lagVilkårResultaterForDeltBosted(
                    personResultat = personResultatForBarn2,
                    behandlingId = behandling.id,
                    fom1 = fomBarn2,
                    // overlapper med første barn vilkårresultat
                    tom1 = tomBarn1,
                )

            personResultatForBarn1.setSortedVilkårResultater(vilkårResultaterForBarn1)
            personResultatForBarn2.setSortedVilkårResultater(vilkårResultaterForBarn2)

            vilkårsvurdering.personResultater = setOf(personResultatForBarn1, personResultatForBarn2)

            // Act
            val deltBostedPerioder =
                EndretUtbetalingAndelValidator.finnDeltBostedPerioder(
                    person = søkerPerson,
                    vilkårsvurdering = vilkårsvurdering,
                )

            // Assert
            assertThat(deltBostedPerioder).hasSize(1)
            assertThat(deltBostedPerioder.single().fom).isEqualTo(fomBarn2.plusMonths(1).førsteDagIInneværendeMåned())
            assertThat(deltBostedPerioder.single().tom).isEqualTo(tomBarn1.sisteDagIMåned())
        }
    }

    @Nested
    inner class ValiderAtAlleOpprettedeEndringerErUtfyltTest {
        @Test
        fun `skal ikke kaste feil når endret utbetaling andel er oppfylt`() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = barnPerson,
                    periodeFom = YearMonth.now().minusMonths(5),
                    periodeTom = YearMonth.now().minusMonths(4),
                    prosent = BigDecimal.ZERO,
                )

            // Act & assert
            assertDoesNotThrow {
                EndretUtbetalingAndelValidator.validerAtAlleOpprettedeEndringerErUtfylt(
                    listOf(endretUtbetalingAndel),
                )
            }
        }

        @Test
        fun `skal kaste feil når endret utbetaling andel ikke er oppfylt`() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = barnPerson,
                    periodeFom = YearMonth.now().minusMonths(5),
                    periodeTom = YearMonth.now().minusMonths(4),
                    prosent = null,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    EndretUtbetalingAndelValidator.validerAtAlleOpprettedeEndringerErUtfylt(
                        listOf(endretUtbetalingAndel),
                    )
                }
            assertThat(exception.message).isEqualTo(
                "Det er opprettet instanser av EndretUtbetalingandel som ikke er fylt ut " +
                    "før navigering til neste steg.",
            )
            assertThat(exception.frontendFeilmelding).isEqualTo(
                "Du har opprettet en eller flere endrede utbetalingsperioder " +
                    "som er ufullstendig utfylt. Disse må enten fylles ut eller slettes før du kan gå videre.",
            )
        }
    }

    @Nested
    inner class ValiderAtEndringerErTilknyttetAndelTilkjentYtelseTest {
        @Test
        fun `skal ikke kaste feil når endret utbetaling andel har ATY`() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = barnPerson,
                    periodeFom = YearMonth.now().minusMonths(5),
                    periodeTom = YearMonth.now().minusMonths(4),
                    prosent = BigDecimal.ZERO,
                )

            val andelerTilkjentYtelse =
                listOf(
                    lagAndelTilkjentYtelse(behandling = behandling),
                )

            val endretUtbetalingAndelMedAndelerTilkjentYtelse =
                EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    endretUtbetalingAndel,
                    andelerTilkjentYtelse,
                )

            // Act & assert
            assertDoesNotThrow {
                EndretUtbetalingAndelValidator.validerAtEndringerErTilknyttetAndelTilkjentYtelse(
                    listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse),
                )
            }
        }

        @Test
        fun `skal kaste feil når endret utbetaling andel ikke har ATY`() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = barnPerson,
                    periodeFom = YearMonth.now().minusMonths(5),
                    periodeTom = YearMonth.now().minusMonths(4),
                    prosent = BigDecimal.ZERO,
                )

            val endretUtbetalingAndelMedAndelerTilkjentYtelse =
                EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    endretUtbetalingAndel,
                    emptyList(),
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    EndretUtbetalingAndelValidator.validerAtEndringerErTilknyttetAndelTilkjentYtelse(
                        listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse),
                    )
                }
            assertThat(exception.message).isEqualTo(
                "Det er opprettet instanser av EndretUtbetalingandel som ikke er tilknyttet noen andeler. De må enten lagres eller slettes av SB.",
            )
            assertThat(exception.frontendFeilmelding).isEqualTo(
                "Du har endrede utbetalingsperioder. Bekreft, slett eller oppdater periodene i listen.",
            )
        }
    }

    @Nested
    inner class ValiderÅrsakTest {
        @Test
        fun `skal kaste feil når delt bosted periode ikke er innenfor endringsperiode`() {
            // Arrange
            val fom = LocalDate.now().minusMonths(5)
            val tom = LocalDate.now().plusMonths(7)

            val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
            val vilkårResultaterForBarn =
                lagVilkårResultaterForDeltBosted(
                    personResultat = personResultatForBarn,
                    behandlingId = behandling.id,
                    fom1 = fom,
                    tom1 = tom,
                )
            personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
            vilkårsvurdering.personResultater = setOf(personResultatForBarn)

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = barnPerson,
                    periodeFom = YearMonth.now().minusMonths(10),
                    periodeTom = YearMonth.now().minusMonths(8),
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    EndretUtbetalingAndelValidator.validerÅrsak(
                        Årsak.DELT_BOSTED,
                        endretUtbetalingAndel,
                        vilkårsvurdering,
                    )
                }
            assertThat(exception.message).isEqualTo(
                "Det finnes ingen delt bosted perioder i perioden det opprettes en endring med årsak delt bosted for.",
            )
            assertThat(exception.frontendFeilmelding).isEqualTo(
                "Du har valgt årsaken 'delt bosted', " +
                    "denne samstemmer ikke med vurderingene gjort på vilkårsvurderingssiden i perioden du har valgt.",
            )
        }

        @Test
        fun `skal ikke kaste feil når delt bosted periode er innenfor endringsperiode`() {
            // Arrange
            val fom = LocalDate.now().minusMonths(5)
            val tom = LocalDate.now().plusMonths(7)

            val personResultatForBarn = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1)
            val vilkårResultaterForBarn =
                lagVilkårResultaterForDeltBosted(
                    personResultat = personResultatForBarn,
                    behandlingId = behandling.id,
                    fom1 = fom,
                    tom1 = tom,
                )
            personResultatForBarn.setSortedVilkårResultater(vilkårResultaterForBarn)
            vilkårsvurdering.personResultater = setOf(personResultatForBarn)

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = barnPerson,
                    periodeFom = YearMonth.now().minusMonths(3),
                    periodeTom = YearMonth.now().minusMonths(2),
                )

            // Act & assert
            assertDoesNotThrow {
                EndretUtbetalingAndelValidator.validerÅrsak(
                    Årsak.DELT_BOSTED,
                    endretUtbetalingAndel,
                    vilkårsvurdering,
                )
            }
        }

        @Test
        fun `skal kaste feil når årsak er ETTERBETALING_3MND, men perioden skal utbetales `() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = barnPerson,
                    periodeFom = YearMonth.now().minusMonths(3),
                    periodeTom = YearMonth.now().minusMonths(2),
                    prosent = BigDecimal(100),
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    EndretUtbetalingAndelValidator.validerÅrsak(
                        Årsak.ETTERBETALING_3MND,
                        endretUtbetalingAndel,
                        null,
                    )
                }

            assertThat(exception.message).isEqualTo(
                "Du kan ikke sette årsak etterbetaling 3 måned når du har valgt at perioden skal utbetales.",
            )
        }

        @Test
        fun `skal kaste feil når årsak er ETTERBETALING_3MND, men endringsperiode slutter etter etterbetalingsgrense`() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = barnPerson,
                    periodeFom = YearMonth.now().minusMonths(1),
                    periodeTom = YearMonth.now().plusMonths(2),
                    prosent = BigDecimal.ZERO,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    EndretUtbetalingAndelValidator.validerÅrsak(
                        Årsak.ETTERBETALING_3MND,
                        endretUtbetalingAndel,
                        null,
                    )
                }

            assertThat(exception.message).isEqualTo(
                "Du kan ikke stoppe etterbetaling for en periode som ikke strekker seg mer enn 3 måned tilbake i tid.",
            )
        }

        @Test
        fun `skal ikke kaste feil når årsak er ETTERBETALING_3MND, men endringsperiode slutter innefor etterbetalingsgrense`() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = barnPerson,
                    periodeFom = YearMonth.now().minusMonths(5),
                    periodeTom = YearMonth.now().minusMonths(4),
                    prosent = BigDecimal.ZERO,
                )

            // Act & assert
            assertDoesNotThrow {
                EndretUtbetalingAndelValidator.validerÅrsak(
                    Årsak.ETTERBETALING_3MND,
                    endretUtbetalingAndel,
                    null,
                )
            }
        }

        @Test
        fun `skal kaste feil når årsak er FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 og endringsperiode fom ikke er i august 2024`() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    periodeFom = YearMonth.of(2024, 7),
                    periodeTom = YearMonth.of(2024, 8),
                    årsak = Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    EndretUtbetalingAndelValidator.validerÅrsak(
                        Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
                        endretUtbetalingAndel,
                        null,
                    )
                }
            assertThat(exception.message).isEqualTo(
                "Årsak \"Fulltidsplass i barnehage august 2024\" er bare mulig å sette til august 2024",
            )
        }

        @Test
        fun `skal kaste feil når årsak er FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 og endringsperiode tom ikke er i august 2024`() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    periodeFom = YearMonth.of(2024, 8),
                    periodeTom = YearMonth.of(2024, 9),
                    årsak = Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    EndretUtbetalingAndelValidator.validerÅrsak(
                        Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
                        endretUtbetalingAndel,
                        null,
                    )
                }
            assertThat(exception.message).isEqualTo(
                "Årsak \"Fulltidsplass i barnehage august 2024\" er bare mulig å sette til august 2024",
            )
        }

        @Test
        fun `skal kaste feil når årsak er FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 og endringsperiode fom og tom ikke er i august 2024`() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    periodeFom = YearMonth.of(2024, 7),
                    periodeTom = YearMonth.of(2024, 9),
                    årsak = Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    EndretUtbetalingAndelValidator.validerÅrsak(
                        Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
                        endretUtbetalingAndel,
                        null,
                    )
                }
            assertThat(exception.message).isEqualTo(
                "Årsak \"Fulltidsplass i barnehage august 2024\" er bare mulig å sette til august 2024",
            )
        }

        @Test
        fun `skal kaste feil når årsak er FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 og EndretUtbetalingAndel mangler årsak`() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    periodeFom = YearMonth.of(2024, 7),
                    periodeTom = YearMonth.of(2024, 9),
                    årsak = null,
                )

            // Act & assert
            val exception =
                assertThrows<IllegalStateException> {
                    EndretUtbetalingAndelValidator.validerÅrsak(
                        Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
                        endretUtbetalingAndel,
                        null,
                    )
                }
            assertThat(exception.message).isEqualTo(
                "Årsak må være satt",
            )
        }

        @Test
        fun `skal ikke kaste feil for årsak FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024`() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    periodeFom = YearMonth.of(2024, 8),
                    periodeTom = YearMonth.of(2024, 8),
                    årsak = Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
                )

            // Act & assert
            assertDoesNotThrow {
                EndretUtbetalingAndelValidator.validerÅrsak(
                    Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
                    endretUtbetalingAndel,
                    null,
                )
            }
        }

        @Test
        fun `skal kaste feil når årsak er ENDRE_MOTTAKER`() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    årsak = Årsak.ENDRE_MOTTAKER,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    EndretUtbetalingAndelValidator.validerÅrsak(
                        Årsak.ENDRE_MOTTAKER,
                        endretUtbetalingAndel,
                        null,
                    )
                }
            assertThat(exception.message).isEqualTo(
                "Årsak ${Årsak.ENDRE_MOTTAKER.visningsnavn} er ikke implementert enda!!",
            )
        }

        @Test
        fun `skal kaste feil når årsak er ALLEREDE_UTBETALT og unleash-togglen er skrudd av`() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    periodeFom = YearMonth.of(2024, 8),
                    periodeTom = YearMonth.of(2024, 8),
                    årsak = Årsak.ALLEREDE_UTBETALT,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    EndretUtbetalingAndelValidator.validerÅrsak(
                        Årsak.ALLEREDE_UTBETALT,
                        endretUtbetalingAndel,
                        null,
                        kanBrukeÅrsakAlleredeUtbetalt = false,
                    )
                }

            assertThat(exception.message).isEqualTo("Årsak Allerede utbetalt er ikke implementert enda!!")
        }

        @Test
        fun `skal ikke kaste feil når årsak er ALLEREDE_UTBETALT`() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    periodeFom = YearMonth.of(2024, 8),
                    periodeTom = YearMonth.of(2024, 8),
                    årsak = Årsak.ALLEREDE_UTBETALT,
                )

            // Act & assert
            assertDoesNotThrow {
                EndretUtbetalingAndelValidator.validerÅrsak(
                    Årsak.ALLEREDE_UTBETALT,
                    endretUtbetalingAndel,
                    null,
                    kanBrukeÅrsakAlleredeUtbetalt = true,
                )
            }
        }
    }
}
