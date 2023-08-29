package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.exception.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ks.sak.common.exception.UtbetalingsikkerhetFeil
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagInitieltTilkjentYtelse
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValidator.finnAktørIderMedUgyldigEtterbetalingsperiode
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValidator.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValidator.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp
import no.nav.familie.ks.sak.kjerne.beregning.domene.maksBeløp
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.YearMonth

internal class TilkjentYtelseValidatorTest {

    val søker = randomAktør()
    val barn = randomAktør("01012112345")
    val barn2 = randomAktør("01012112346")

    val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
        behandlingId = behandling.id,
        søkerPersonIdent = søker.aktivFødselsnummer(),
        barnasIdenter = listOf(barn.aktivFødselsnummer()),
        søkerAktør = søker,
        barnAktør = listOf(barn)
    )
    private val tilkjentYtelse = lagInitieltTilkjentYtelse(behandling)

    @Test
    fun `validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp skal kaste feil når utbetalingsperiode er mer enn 11 måneder`() {
        val andelTilkjentYtelse1 = lagAndelTilkjentYtelse(
            tilkjentYtelse = tilkjentYtelse,
            behandling = behandling,
            aktør = barn,
            stønadFom = YearMonth.now().minusMonths(10),
            stønadTom = YearMonth.now().minusMonths(4)
        )
        val andelTilkjentYtelse2 = lagAndelTilkjentYtelse(
            tilkjentYtelse = tilkjentYtelse,
            behandling = behandling,
            aktør = barn,
            stønadFom = YearMonth.now().minusMonths(3),
            stønadTom = YearMonth.now().plusMonths(6)
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(setOf(andelTilkjentYtelse1, andelTilkjentYtelse2))

        val exception = assertThrows<FunksjonellFeil> {
            validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(tilkjentYtelse, personopplysningGrunnlag)
        }
        val feilmelding =
            "Kontantstøtte kan maks utbetales for 11 måneder. Du er i ferd med å utbetale mer enn dette for barn med fnr ${barn.aktivFødselsnummer()}. " +
                "Kontroller datoene på vilkårene eller ta kontakt med team familie"

        assertEquals(feilmelding, exception.frontendFeilmelding)
        assertEquals(feilmelding, exception.message)
    }

    @Test
    fun `validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp skal ikke kaste feil når selvom utbetalingsperioden er over 11 måneder dersom det er fordelt på flere barn`() {
        val andelTilkjentYtelse1 = lagAndelTilkjentYtelse(
            tilkjentYtelse = tilkjentYtelse,
            behandling = behandling,
            aktør = barn,
            stønadFom = YearMonth.now().minusMonths(10),
            stønadTom = YearMonth.now().minusMonths(4)
        )
        val andelTilkjentYtelse2 = lagAndelTilkjentYtelse(
            tilkjentYtelse = tilkjentYtelse,
            behandling = behandling,
            aktør = barn2,
            stønadFom = YearMonth.now().minusMonths(3),
            stønadTom = YearMonth.now().plusMonths(6)
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(setOf(andelTilkjentYtelse1, andelTilkjentYtelse2))

        assertDoesNotThrow {
            validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(tilkjentYtelse, personopplysningGrunnlag)
        }
    }

    @Test
    fun `validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp skal kaste feil når søkers andel ikke er tom`() {
        val andelTilkjentYtelseForSøker = lagAndelTilkjentYtelse(
            tilkjentYtelse = tilkjentYtelse,
            behandling = behandling,
            aktør = søker
        )
        val andelTilkjentYtelseForBarn = lagAndelTilkjentYtelse(
            tilkjentYtelse = tilkjentYtelse,
            behandling = behandling,
            aktør = barn
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(listOf(andelTilkjentYtelseForSøker, andelTilkjentYtelseForBarn))

        val exception = assertThrows<Feil> {
            validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(tilkjentYtelse, personopplysningGrunnlag)
        }

        assertEquals("Feil i beregning. Søkers andeler må være tom", exception.message)
    }

    @Test
    fun `validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp skal kaste feil når barn har andel med beløp som er større en maks beløp`() {
        val andelTilkjentYtelseForBarn = lagAndelTilkjentYtelse(
            tilkjentYtelse = tilkjentYtelse,
            behandling = behandling,
            aktør = barn,
            stønadFom = YearMonth.now().minusMonths(11),
            stønadTom = YearMonth.now().minusMonths(6),
            sats = 8000
        )
        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelseForBarn)

        val andeler = tilkjentYtelse.andelerTilkjentYtelse

        val exception = assertThrows<FunksjonellFeil> {
            validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(tilkjentYtelse, personopplysningGrunnlag)
        }

        assertEquals(
            "Validering av andeler for BARN i perioden (${andeler.first().stønadFom} - " +
                "${andeler.first().stønadTom}) feilet: Tillatt totalbeløp = ${maksBeløp()}, faktiske totalbeløp = 8000.",
            exception.message
        )
        assertEquals(
            "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. $KONTAKT_TEAMET_SUFFIX",
            exception.frontendFeilmelding
        )
    }

    @Test
    fun `validerAtBarnIkkeFårFlereUtbetalingerSammePeriode - skal kaste feil dersom et eller flere barn får flere utbetalinger samme periode`() {
        val andelTilkjentYtelseForBarn = lagAndelTilkjentYtelse(
            tilkjentYtelse = tilkjentYtelse,
            behandling = behandling,
            aktør = barn,
            stønadFom = YearMonth.now().minusMonths(11),
            stønadTom = YearMonth.now().minusMonths(6),
            sats = 8000
        )
        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelseForBarn)

        val annenBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

        val annenTilkjentYtelse = lagInitieltTilkjentYtelse(annenBehandling).also {
            it.stønadFom = YearMonth.now().minusMonths(11)
            it.stønadTom = YearMonth.now()
            it.andelerTilkjentYtelse.add(andelTilkjentYtelseForBarn)
        }

        val andreTilkjenteYtelser = listOf(annenTilkjentYtelse)
        val person = lagPerson(personopplysningGrunnlag, barn, PersonType.BARN)
        val utbetalingsikkerhetFeil = assertThrows<UtbetalingsikkerhetFeil> {
            validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
                tilkjentYtelse,
                listOf(
                    Pair(person, andreTilkjenteYtelser)
                ),
                personopplysningGrunnlag
            )
        }

        assertEquals(
            "Vi finner utbetalinger som overstiger 100% på hvert av barna: ${person.fødselsdato.tilKortString()}",
            utbetalingsikkerhetFeil.message
        )
        assertEquals(
            "Du kan ikke godkjenne dette vedtaket fordi det vil betales ut mer enn 100% for barn født ${person.fødselsdato.tilKortString()}. Reduksjonsvedtak til annen person må være sendt til godkjenning før du kan gå videre.",
            utbetalingsikkerhetFeil.frontendFeilmelding
        )
    }

    @Test
    fun `validerAtBarnIkkeFårFlereUtbetalingerSammePeriode - skal ikke kaste feil dersom ingen barn har flere utbetalinger i samme periode`() {
        val andelTilkjentYtelseForBarn = lagAndelTilkjentYtelse(
            tilkjentYtelse = tilkjentYtelse,
            behandling = behandling,
            aktør = barn,
            stønadFom = YearMonth.now().minusMonths(11),
            stønadTom = YearMonth.now().minusMonths(6),
            sats = 8000
        )
        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelseForBarn)

        val annenBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

        val annenTilkjentYtelse = lagInitieltTilkjentYtelse(annenBehandling).also {
            it.stønadFom = YearMonth.now().minusMonths(11)
            it.stønadTom = YearMonth.now()
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelse,
                    behandling = behandling,
                    aktør = barn,
                    stønadFom = YearMonth.now().minusMonths(5),
                    stønadTom = YearMonth.now(),
                    sats = 8000
                )
            )
        }

        val andreTilkjenteYtelser = listOf(annenTilkjentYtelse)
        val person = lagPerson(personopplysningGrunnlag, barn, PersonType.BARN)

        validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
            tilkjentYtelse,
            listOf(
                Pair(person, andreTilkjenteYtelser)
            ),
            personopplysningGrunnlag
        )
    }

    @Test
    fun `finnAktørIderMedUgyldigEtterbetalingsperiode - skal returnere liste over aktører med ugyldig etterbetalingsperiode når det finnes andeler med fom før gyldig etterbetallings fom hvor beløpet har økt`() {
        val andeler = listOf(
            lagAndelTilkjentYtelse(
                behandling = behandling,
                aktør = barn,
                stønadFom = YearMonth.now().minusMonths(4),
                stønadTom = YearMonth.now(),
                sats = 5000
            )
        )
        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                behandling = behandling,
                aktør = barn,
                stønadFom = YearMonth.now().minusMonths(4),
                stønadTom = YearMonth.now(),
                sats = 4000
            )
        )
        val aktørerMedUgyldigEtterbetalingsperiode =
            finnAktørIderMedUgyldigEtterbetalingsperiode(forrigeAndeler, andeler, LocalDateTime.now())

        assertEquals(1, aktørerMedUgyldigEtterbetalingsperiode.size)
        assertEquals(aktørerMedUgyldigEtterbetalingsperiode.single(), barn.aktørId)
    }

    @Test
    fun `finnAktørIderMedUgyldigEtterbetalingsperiode - skal returnere liste over aktører med ugyldig etterbetalingsperiode når det er lagt til nye andeler før gyldig etterbetalings fom med beløp større enn 0`() {
        val andeler = listOf(
            lagAndelTilkjentYtelse(
                behandling = behandling,
                aktør = barn,
                stønadFom = YearMonth.now().minusMonths(4),
                stønadTom = YearMonth.now(),
                sats = 5000
            )
        )
        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                behandling = behandling,
                aktør = barn,
                stønadFom = YearMonth.now().minusMonths(3),
                stønadTom = YearMonth.now(),
                sats = 5000
            )
        )
        var aktørerMedUgyldigEtterbetalingsperiode =
            finnAktørIderMedUgyldigEtterbetalingsperiode(forrigeAndeler, andeler, LocalDateTime.now())

        assertEquals(1, aktørerMedUgyldigEtterbetalingsperiode.size)
        assertEquals(aktørerMedUgyldigEtterbetalingsperiode.single(), barn.aktørId)

        aktørerMedUgyldigEtterbetalingsperiode =
            finnAktørIderMedUgyldigEtterbetalingsperiode(null, andeler, LocalDateTime.now())

        assertEquals(1, aktørerMedUgyldigEtterbetalingsperiode.size)
        assertEquals(aktørerMedUgyldigEtterbetalingsperiode.single(), barn.aktørId)
    }

    @Test
    fun `finnAktørIderMedUgyldigEtterbetalingsperiode - skal returnere tom liste over aktører når andel tilkjent ytelse er uendret selv om fom er mer enn 3 mnd tilbake i tid`() {
        val andeler = listOf(
            lagAndelTilkjentYtelse(
                behandling = behandling,
                aktør = barn,
                stønadFom = YearMonth.now().minusMonths(4),
                stønadTom = YearMonth.now(),
                sats = 5000
            )
        )
        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                behandling = behandling,
                aktør = barn,
                stønadFom = YearMonth.now().minusMonths(4),
                stønadTom = YearMonth.now(),
                sats = 5000
            )
        )
        val aktørerMedUgyldigEtterbetalingsperiode =
            finnAktørIderMedUgyldigEtterbetalingsperiode(forrigeAndeler, andeler, LocalDateTime.now())

        assertEquals(0, aktørerMedUgyldigEtterbetalingsperiode.size)
    }

    @Test
    fun `finnAktørIderMedUgyldigEtterbetalingsperiode - skal returnere tom liste over aktører når andel tilkjent ytelse er endret innenfor siste 3 mnd`() {
        val andeler = listOf(
            lagAndelTilkjentYtelse(
                behandling = behandling,
                aktør = barn,
                stønadFom = YearMonth.now().minusMonths(2),
                stønadTom = YearMonth.now(),
                sats = 5000
            )
        )
        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                behandling = behandling,
                aktør = barn,
                stønadFom = YearMonth.now().minusMonths(4),
                stønadTom = YearMonth.now(),
                sats = 4000
            )
        )
        val aktørerMedUgyldigEtterbetalingsperiode =
            finnAktørIderMedUgyldigEtterbetalingsperiode(forrigeAndeler, andeler, LocalDateTime.now())

        assertEquals(0, aktørerMedUgyldigEtterbetalingsperiode.size)
    }
}
