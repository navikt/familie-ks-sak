package no.nav.familie.ks.sak.kjerne.beregning

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.UtbetalingsikkerhetFeil
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.data.fnrTilFødselsdato
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class TilkjentYtelseValideringServiceTest {
    @MockK
    private lateinit var totrinnskontrollService: TotrinnskontrollService

    @MockK
    private lateinit var beregningService: BeregningService

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var personidentService: PersonidentService

    @MockK
    private lateinit var behandlingService: BehandlingService

    private lateinit var tilkjentYtelseValideringService: TilkjentYtelseValideringService

    private val barn1 = randomAktør()
    private val barn2 = randomAktør()
    private val barn3MedUtbetalinger = randomAktør()
    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
        behandling.id,
        "12345678910",
        barnasIdenter = listOf(
            barn1.aktivFødselsnummer(),
            barn2.aktivFødselsnummer(),
            barn3MedUtbetalinger.aktivFødselsnummer()
        ),
        barnAktør = listOf(barn1, barn2, barn3MedUtbetalinger)
    )

    @BeforeEach
    fun setUp() {
        tilkjentYtelseValideringService = TilkjentYtelseValideringService(
            beregningService = beregningService,
            totrinnskontrollService = totrinnskontrollService,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
            personidentService = personidentService,
            behandlingService = behandlingService
        )

        every {
            beregningService.hentRelevanteTilkjentYtelserForBarn(
                barnAktør = barn1,
                fagsakId = any()
            )
        } answers { emptyList() }
        every {
            beregningService.hentRelevanteTilkjentYtelserForBarn(
                barnAktør = barn2,
                fagsakId = any()
            )
        } answers { emptyList() }
        every {
            beregningService.hentRelevanteTilkjentYtelserForBarn(
                barnAktør = barn3MedUtbetalinger,
                fagsakId = any()
            )
        } answers {
            listOf(
                TilkjentYtelse(
                    behandling = behandling,
                    endretDato = LocalDate.now().minusYears(1),
                    opprettetDato = LocalDate.now().minusYears(1)
                )
            )
        }
    }

    @Test
    fun `kontantstøtteLøperForAnnenForelder - skal returnere false hvis ingen barn allerede mottar kontantstøtte`() {
        assertFalse(
            tilkjentYtelseValideringService.kontantstøtteLøperForAnnenForelder(
                behandling = behandling,
                barna = listOf(
                    lagPerson(personopplysningGrunnlag, barn1, PersonType.BARN),
                    lagPerson(personopplysningGrunnlag, barn2, PersonType.BARN)
                )
            )
        )
    }

    @Test
    fun `kontantstøtteLøperForAnnenForelder - skal returnere true hvis det løper kontantstøtte for minst ett barn`() {
        assertTrue(
            tilkjentYtelseValideringService.kontantstøtteLøperForAnnenForelder(
                behandling = behandling,
                barna = listOf(
                    lagPerson(personopplysningGrunnlag, barn1, PersonType.BARN),
                    lagPerson(personopplysningGrunnlag, barn3MedUtbetalinger, PersonType.BARN)
                )
            )
        )
    }

    @Test
    fun `finnAktørerMedUgyldigEtterbetalingsperiode - skal returnere liste med personer som har etterbetaling som er mer enn 3 mnd tilbake i tid`() {
        val tilkjentYtelse = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            andelerTilkjentYtelse = mutableSetOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn1,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 8000
                ),
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn2,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 8000
                )
            )
        )

        val forrigeBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

        val forrigeTilkjentYtelse = TilkjentYtelse(
            behandling = forrigeBehandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            andelerTilkjentYtelse = mutableSetOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn1,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 8000
                ),
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn2,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 7000
                )
            )
        )

        every { beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id) } answers { tilkjentYtelse }
        every { behandlingService.hentBehandling(behandlingId = behandling.id) } answers { behandling }
        every { behandlingService.hentForrigeBehandlingSomErIverksatt(behandling = behandling) } answers { forrigeBehandling }
        every { beregningService.hentTilkjentYtelseForBehandling(behandlingId = forrigeBehandling.id) } answers { forrigeTilkjentYtelse }
        every { personidentService.hentAktør(barn1.aktørId) } answers { barn1 }
        every { personidentService.hentAktør(barn2.aktørId) } answers { barn2 }

        val aktørerMedUgyldigEtterbetalingsperiode =
            tilkjentYtelseValideringService.finnAktørerMedUgyldigEtterbetalingsperiode(behandlingId = behandling.id)

        assertTrue(aktørerMedUgyldigEtterbetalingsperiode.size == 1)
        assertEquals(
            barn2,
            aktørerMedUgyldigEtterbetalingsperiode.single()
        )
    }

    @Test
    fun `validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling - skal kaste feil dersom det finnes andeler med samme offset i behandling`() {
        val tilkjentYtelse = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            andelerTilkjentYtelse = mutableSetOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn1,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 8000
                ).also { it.periodeOffset = 1 },
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn2,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 8000
                ).also { it.periodeOffset = 1 }
            )
        )

        every { beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id) } returns tilkjentYtelse

        val feil = assertThrows<Feil> {
            tilkjentYtelseValideringService.validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling(behandling.id)
        }
        assertEquals(
            "Behandling ${behandling.id} har andel tilkjent ytelse med offset lik en annen andel i behandlingen.",
            feil.message
        )
    }

    @Test
    fun `validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling - skal ikke kaste feil dersom det ikke finnes andeler med samme offset i behandling`() {
        val tilkjentYtelse = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            andelerTilkjentYtelse = mutableSetOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn1,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 8000
                ).also { it.periodeOffset = 1 },
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn2,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 8000
                ).also { it.periodeOffset = 2 }
            )
        )

        every { beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id) } returns tilkjentYtelse

        assertDoesNotThrow {
            tilkjentYtelseValideringService.validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling(
                behandling.id
            )
        }
    }

    @Test
    fun `validerAtIngenUtbetalingerOverstiger100Prosent - skal kaste feil dersom barn får flere utbetalinger i samme periode`() {
        val tilkjentYtelse = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            andelerTilkjentYtelse = mutableSetOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn1,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 8000
                )
            )
        )
        val annenBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

        val annenTilkjentYtelse = TilkjentYtelse(
            behandling = annenBehandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            andelerTilkjentYtelse = mutableSetOf(
                lagAndelTilkjentYtelse(
                    behandling = annenBehandling,
                    aktør = barn1,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 8000
                )
            )
        )

        every { totrinnskontrollService.hentAktivForBehandling(behandling.id) } returns Totrinnskontroll(
            behandling = behandling,
            saksbehandler = "Test",
            saksbehandlerId = "1234",
            godkjent = true
        )

        every { beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id) } returns tilkjentYtelse
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id) } returns personopplysningGrunnlag
        every { beregningService.hentRelevanteTilkjentYtelserForBarn(any(), any()) } returns listOf(annenTilkjentYtelse)

        val utbetalingsikkerhetFeil = assertThrows<UtbetalingsikkerhetFeil> {
            tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(behandling)
        }

        assertEquals(
            "Vi finner utbetalinger som overstiger 100% på hvert av barna: ${fnrTilFødselsdato(barn1.aktivFødselsnummer()).tilKortString()}",
            utbetalingsikkerhetFeil.message
        )
        assertEquals(
            "Du kan ikke godkjenne dette vedtaket fordi det vil betales ut mer enn 100% for barn født ${
            fnrTilFødselsdato(
                barn1.aktivFødselsnummer()
            ).tilKortString()
            }. Reduksjonsvedtak til annen person må være sendt til godkjenning før du kan gå videre.",
            utbetalingsikkerhetFeil.frontendFeilmelding
        )
    }

    @Test
    fun `validerAtIngenUtbetalingerOverstiger100Prosent - skal ikke kaste feil dersom barn ikke har utbetalinger i samme periode`() {
        val tilkjentYtelse = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            andelerTilkjentYtelse = mutableSetOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn1,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now().minusMonths(2),
                    sats = 8000
                )
            )
        )
        val annenBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

        val annenTilkjentYtelse = TilkjentYtelse(
            behandling = annenBehandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            andelerTilkjentYtelse = mutableSetOf(
                lagAndelTilkjentYtelse(
                    behandling = annenBehandling,
                    aktør = barn1,
                    stønadFom = YearMonth.now().minusMonths(1),
                    stønadTom = YearMonth.now(),
                    sats = 8000
                )
            )
        )

        every { totrinnskontrollService.hentAktivForBehandling(behandling.id) } returns Totrinnskontroll(
            behandling = behandling,
            saksbehandler = "Test",
            saksbehandlerId = "1234",
            godkjent = true
        )

        every { beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id) } returns tilkjentYtelse
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id) } returns personopplysningGrunnlag
        every { beregningService.hentRelevanteTilkjentYtelserForBarn(any(), any()) } returns listOf(annenTilkjentYtelse)

        assertDoesNotThrow {
            tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(behandling)
        }
    }
}
