package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndel
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
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
                    personer = setOf(søkerPerson),
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
                    personer = setOf(søkerPerson),
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
                    personer = setOf(søkerPerson),
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
                    personer = setOf(søkerPerson),
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
    inner class ValiderAtAlleOpprettedeEndringerErUtfyltTest {
        @Test
        fun `skal ikke kaste feil når endret utbetaling andel er oppfylt`() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    personer = setOf(barnPerson),
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
                    personer = setOf(barnPerson),
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
                    personer = setOf(barnPerson),
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
                    personer = setOf(barnPerson),
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
        fun `skal kaste feil når årsak er ETTERBETALING_3MND, men perioden skal utbetales `() {
            // Arrange
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    personer = setOf(barnPerson),
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
                    personer = setOf(barnPerson),
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
                    personer = setOf(barnPerson),
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
                assertThrows<Feil> {
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
                )
            }
        }
    }
}
