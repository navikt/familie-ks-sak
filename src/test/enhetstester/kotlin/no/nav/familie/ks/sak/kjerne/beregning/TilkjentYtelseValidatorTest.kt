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
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.lagAutomatiskGenererteVilkårForBarnetsAlder
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValidator.finnAktørIderMedUgyldigEtterbetalingsperiode
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValidator.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValidator.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp
import no.nav.familie.ks.sak.kjerne.beregning.domene.maksBeløp
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

internal class TilkjentYtelseValidatorTest {
    val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
    val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør("01012112345"))
    val barn2 = lagPerson(personType = PersonType.BARN, aktør = randomAktør("01012112346"))

    val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    val personopplysningGrunnlag =
        lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
            barnasIdenter = listOf(barn.aktør.aktivFødselsnummer(), barn2.aktør.aktivFødselsnummer()),
            søkerAktør = søker.aktør,
            barnAktør = listOf(barn.aktør, barn2.aktør),
        )
    var vilkårsvurdering = lagVilkårsvurdering(søkerAktør = søker.aktør, behandling = behandling, resultat = Resultat.OPPFYLT, søkerPeriodeFom = LocalDate.of(2021, 1, 1))
    private val tilkjentYtelse = lagInitieltTilkjentYtelse(behandling)

    @Test
    fun `validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp skal kaste feil når utbetalingsperiode er mer enn 11 måneder`() {
        val andelTilkjentYtelse1 =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                behandling = behandling,
                aktør = barn.aktør,
                stønadFom = YearMonth.of(2022, 1),
                stønadTom = YearMonth.of(2022, 6),
            )
        val andelTilkjentYtelse2 =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                behandling = behandling,
                aktør = barn.aktør,
                stønadFom = YearMonth.of(2022, 7),
                stønadTom = YearMonth.of(2022, 12),
            )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(setOf(andelTilkjentYtelse1, andelTilkjentYtelse2))

        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        val barnetsAlderVilkårResultater = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = behandling.id, fødselsdato = LocalDate.of(2022, 1, 1).minusMonths(11), adopsjonsdato = null)

        val exception =
            assertThrows<FunksjonellFeil> {
                validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                    tilkjentYtelse = tilkjentYtelse,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    alleBarnetsAlderVilkårResultater = barnetsAlderVilkårResultater,
                    adopsjonerIBehandling = emptyList(),
                    dagensDato = LocalDate.of(2025, 4, 1),
                )
            }
        val feilmelding =
            "Kontantstøtte kan maks utbetales for 11 måneder. Du er i ferd med å utbetale 12 måneder for barn med fnr ${barn.aktør.aktivFødselsnummer()}. " +
                "Kontroller datoene på vilkårene eller ta kontakt med Team BAKS"

        assertEquals(feilmelding, exception.frontendFeilmelding)
        assertEquals(feilmelding, exception.message)
    }

    @Test
    fun `validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp skal kaste feil når utbetalingsperiode er mer enn 7 måneder når barn er født i 2023 og treffes av nytt lovverk fra august 2024`() {
        val barnFødtIJanuar2023 = lagPerson(personType = PersonType.BARN, aktør = randomAktør("01012312345"))

        val andelTilkjentYtelse1 =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                behandling = behandling,
                aktør = barnFødtIJanuar2023.aktør,
                stønadFom = YearMonth.of(2024, 2),
                stønadTom = YearMonth.of(2024, 5),
            )
        val andelTilkjentYtelse2 =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                behandling = behandling,
                aktør = barnFødtIJanuar2023.aktør,
                stønadFom = YearMonth.of(2024, 6),
                stønadTom = YearMonth.of(2024, 9),
            )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(setOf(andelTilkjentYtelse1, andelTilkjentYtelse2))

        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnFødtIJanuar2023.aktør)
        val barnetsAlderVilkårResultater = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = behandling.id, fødselsdato = LocalDate.of(2023, 1, 1), adopsjonsdato = null)

        val exception =
            assertThrows<FunksjonellFeil> {
                validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                    tilkjentYtelse = tilkjentYtelse,
                    personopplysningGrunnlag =
                        lagPersonopplysningGrunnlag(
                            behandlingId = behandling.id,
                            søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                            barnasIdenter = listOf(barnFødtIJanuar2023.aktør.aktivFødselsnummer()),
                            søkerAktør = søker.aktør,
                            barnAktør = listOf(barnFødtIJanuar2023.aktør),
                        ),
                    alleBarnetsAlderVilkårResultater = barnetsAlderVilkårResultater,
                    adopsjonerIBehandling = emptyList(),
                    dagensDato = LocalDate.of(2025, 4, 1),
                )
            }
        val feilmelding =
            "Kontantstøtte kan maks utbetales for 7 måneder. Du er i ferd med å utbetale 8 måneder for barn med fnr ${barnFødtIJanuar2023.aktør.aktivFødselsnummer()}. " +
                "Kontroller datoene på vilkårene eller ta kontakt med Team BAKS"

        assertEquals(feilmelding, exception.frontendFeilmelding)
        assertEquals(feilmelding, exception.message)
    }

    @Test
    fun `validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp skal kaste feil når utbetalingsperiode er mer enn 11 måneder når barn er født før september 2022 og kun treffes av gammelt lovverk`() {
        val barnFødtIAugust2022 = lagPerson(personType = PersonType.BARN, aktør = randomAktør("01082212345"))

        val andelTilkjentYtelse1 =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                behandling = behandling,
                aktør = barnFødtIAugust2022.aktør,
                stønadFom = YearMonth.of(2023, 9),
                stønadTom = YearMonth.of(2024, 8),
            )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(setOf(andelTilkjentYtelse1))

        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnFødtIAugust2022.aktør)
        val barnetsAlderVilkårResultater = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = behandling.id, fødselsdato = LocalDate.of(2022, 8, 1), adopsjonsdato = null)

        val exception =
            assertThrows<FunksjonellFeil> {
                validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                    tilkjentYtelse = tilkjentYtelse,
                    personopplysningGrunnlag =
                        lagPersonopplysningGrunnlag(
                            behandlingId = behandling.id,
                            søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                            barnasIdenter = listOf(barnFødtIAugust2022.aktør.aktivFødselsnummer()),
                            søkerAktør = søker.aktør,
                            barnAktør = listOf(barnFødtIAugust2022.aktør),
                        ),
                    alleBarnetsAlderVilkårResultater = barnetsAlderVilkårResultater,
                    adopsjonerIBehandling = emptyList(),
                    dagensDato = LocalDate.of(2025, 4, 1),
                )
            }
        val feilmelding =
            "Kontantstøtte kan maks utbetales for 11 måneder. Du er i ferd med å utbetale 12 måneder for barn med fnr ${barnFødtIAugust2022.aktør.aktivFødselsnummer()}. " +
                "Kontroller datoene på vilkårene eller ta kontakt med Team BAKS"

        assertEquals(feilmelding, exception.frontendFeilmelding)
        assertEquals(feilmelding, exception.message)
    }

    @Test
    fun `validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp skal ikke kaste feil når selvom utbetalingsperioden er over 11 måneder dersom det er fordelt på flere barn`() {
        val barnFødtAugust2023 = lagPerson(personType = PersonType.BARN, aktør = randomAktør("01082312345"))
        val barnFødtAugust2022 = lagPerson(personType = PersonType.BARN, aktør = randomAktør("01082212345"))

        val andelTilkjentYtelse1 =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                behandling = behandling,
                aktør = barnFødtAugust2023.aktør,
                stønadFom = YearMonth.now().minusMonths(10),
                stønadTom = YearMonth.now().minusMonths(4),
            )
        val andelTilkjentYtelse2 =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                behandling = behandling,
                aktør = barnFødtAugust2022.aktør,
                stønadFom = YearMonth.of(2022, 9),
                stønadTom = YearMonth.of(2023, 7),
            )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(setOf(andelTilkjentYtelse1, andelTilkjentYtelse2))

        val personResultatBarn1 = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnFødtAugust2022.aktør)
        val barnetsAlderVilkårResultaterBarn1 = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultatBarn1, behandlingId = behandling.id, fødselsdato = LocalDate.of(2022, 8, 1), adopsjonsdato = null)

        val personResultatBarn2 = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnFødtAugust2023.aktør)
        val barnetsAlderVilkårResultaterBarn2 = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultatBarn2, behandlingId = behandling.id, fødselsdato = LocalDate.of(2023, 8, 1), adopsjonsdato = null)

        assertDoesNotThrow {
            validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                tilkjentYtelse = tilkjentYtelse,
                lagPersonopplysningGrunnlag(
                    behandlingId = behandling.id,
                    søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                    barnasIdenter = listOf(barnFødtAugust2023.aktør.aktivFødselsnummer(), barnFødtAugust2022.aktør.aktivFødselsnummer()),
                    søkerAktør = søker.aktør,
                    barnAktør = listOf(barnFødtAugust2023.aktør, barnFødtAugust2022.aktør),
                ),
                alleBarnetsAlderVilkårResultater = barnetsAlderVilkårResultaterBarn1 + barnetsAlderVilkårResultaterBarn2,
                adopsjonerIBehandling = emptyList(),
                dagensDato = LocalDate.of(2025, 4, 1),
            )
        }
    }

    @Test
    fun `validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp skal kaste feil når søkers andel ikke er tom`() {
        val andelTilkjentYtelseForSøker =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                behandling = behandling,
                aktør = søker.aktør,
            )
        val andelTilkjentYtelseForBarn =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                behandling = behandling,
                aktør = barn.aktør,
            )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(listOf(andelTilkjentYtelseForSøker, andelTilkjentYtelseForBarn))

        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        val barnetsAlderVilkårResultater = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = behandling.id, fødselsdato = LocalDate.now().minusYears(1).minusMonths(5), adopsjonsdato = null)

        val exception =
            assertThrows<Feil> {
                validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                    tilkjentYtelse = tilkjentYtelse,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    alleBarnetsAlderVilkårResultater = barnetsAlderVilkårResultater,
                    adopsjonerIBehandling = emptyList(),
                    dagensDato = LocalDate.of(2025, 4, 1),
                )
            }

        assertEquals("Feil i beregning. Søkers andeler må være tom", exception.message)
    }

    @Test
    fun `validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp skal kaste feil når barn har andel med beløp som er større en maks beløp`() {
        val barnFødtJuli2023 = lagPerson(personType = PersonType.BARN, aktør = randomAktør("01072312345"))

        val andelTilkjentYtelseForBarn =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                behandling = behandling,
                aktør = barnFødtJuli2023.aktør,
                stønadFom = YearMonth.now().minusMonths(11),
                stønadTom = YearMonth.now().minusMonths(6),
                sats = 8000,
            )
        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelseForBarn)

        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnFødtJuli2023.aktør)
        val barnetsAlderVilkårResultater = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = behandling.id, fødselsdato = LocalDate.now().minusYears(1).minusMonths(5), adopsjonsdato = null)

        val andeler = tilkjentYtelse.andelerTilkjentYtelse

        val exception =
            assertThrows<FunksjonellFeil> {
                validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                    tilkjentYtelse = tilkjentYtelse,
                    personopplysningGrunnlag =
                        lagPersonopplysningGrunnlag(
                            behandlingId = behandling.id,
                            søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                            barnasIdenter = listOf(barnFødtJuli2023.aktør.aktivFødselsnummer()),
                            søkerAktør = søker.aktør,
                            barnAktør = listOf(barnFødtJuli2023.aktør),
                        ),
                    alleBarnetsAlderVilkårResultater = barnetsAlderVilkårResultater,
                    adopsjonerIBehandling = emptyList(),
                    dagensDato = LocalDate.of(2025, 4, 1),
                )
            }

        assertEquals(
            "Validering av andeler for BARN i perioden (${andeler.first().stønadFom} - " +
                "${andeler.first().stønadTom}) feilet: Tillatt totalbeløp = ${maksBeløp()}, faktiske totalbeløp = 8000.",
            exception.message,
        )
        assertEquals(
            "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. $KONTAKT_TEAMET_SUFFIX",
            exception.frontendFeilmelding,
        )
    }

    @Test
    fun `validerAtBarnIkkeFårFlereUtbetalingerSammePeriode - skal kaste feil dersom et eller flere barn får flere utbetalinger samme periode`() {
        val andelTilkjentYtelseForBarn =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                behandling = behandling,
                aktør = barn.aktør,
                stønadFom = YearMonth.now().minusMonths(11),
                stønadTom = YearMonth.now().minusMonths(6),
                sats = 8000,
            )
        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelseForBarn)

        val annenBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

        val annenTilkjentYtelse =
            lagInitieltTilkjentYtelse(annenBehandling).also {
                it.stønadFom = YearMonth.now().minusMonths(11)
                it.stønadTom = YearMonth.now()
                it.andelerTilkjentYtelse.add(andelTilkjentYtelseForBarn)
            }

        val andreTilkjenteYtelser = listOf(annenTilkjentYtelse)
        val person = lagPerson(personopplysningGrunnlag, barn.aktør, PersonType.BARN)
        val utbetalingsikkerhetFeil =
            assertThrows<UtbetalingsikkerhetFeil> {
                validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
                    tilkjentYtelse,
                    listOf(
                        Pair(person, andreTilkjenteYtelser),
                    ),
                    personopplysningGrunnlag,
                )
            }

        assertEquals(
            "Vi finner utbetalinger som overstiger 100% på hvert av barna: ${person.fødselsdato.tilKortString()}",
            utbetalingsikkerhetFeil.message,
        )
        assertEquals(
            "Du kan ikke godkjenne dette vedtaket fordi det vil betales ut mer enn 100% for barn født ${person.fødselsdato.tilKortString()}. Reduksjonsvedtak til annen person må være sendt til godkjenning før du kan gå videre.",
            utbetalingsikkerhetFeil.frontendFeilmelding,
        )
    }

    @Test
    fun `validerAtBarnIkkeFårFlereUtbetalingerSammePeriode - skal ikke kaste feil dersom ingen barn har flere utbetalinger i samme periode`() {
        val andelTilkjentYtelseForBarn =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                behandling = behandling,
                aktør = barn.aktør,
                stønadFom = YearMonth.now().minusMonths(11),
                stønadTom = YearMonth.now().minusMonths(6),
                sats = 8000,
            )
        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelseForBarn)

        val annenBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

        val annenTilkjentYtelse =
            lagInitieltTilkjentYtelse(annenBehandling).also {
                it.stønadFom = YearMonth.now().minusMonths(11)
                it.stønadTom = YearMonth.now()
                it.andelerTilkjentYtelse.add(
                    lagAndelTilkjentYtelse(
                        tilkjentYtelse = tilkjentYtelse,
                        behandling = behandling,
                        aktør = barn.aktør,
                        stønadFom = YearMonth.now().minusMonths(5),
                        stønadTom = YearMonth.now(),
                        sats = 8000,
                    ),
                )
            }

        val andreTilkjenteYtelser = listOf(annenTilkjentYtelse)
        val person = lagPerson(personopplysningGrunnlag, barn.aktør, PersonType.BARN)

        validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
            tilkjentYtelse,
            listOf(
                Pair(person, andreTilkjenteYtelser),
            ),
            personopplysningGrunnlag,
        )
    }

    @Test
    fun `finnAktørIderMedUgyldigEtterbetalingsperiode - skal returnere liste over aktører med ugyldig etterbetalingsperiode når det finnes andeler med fom før gyldig etterbetallings fom hvor beløpet har økt`() {
        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn.aktør,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 5000,
                ),
            )
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn.aktør,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 4000,
                ),
            )
        val aktørerMedUgyldigEtterbetalingsperiode =
            finnAktørIderMedUgyldigEtterbetalingsperiode(forrigeAndeler, andeler, LocalDateTime.now())

        assertEquals(1, aktørerMedUgyldigEtterbetalingsperiode.size)
        assertEquals(aktørerMedUgyldigEtterbetalingsperiode.single(), barn.aktør.aktørId)
    }

    @Test
    fun `finnAktørIderMedUgyldigEtterbetalingsperiode - skal returnere liste over aktører med ugyldig etterbetalingsperiode når det er lagt til nye andeler før gyldig etterbetalings fom med beløp større enn 0`() {
        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn.aktør,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 5000,
                ),
            )
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn.aktør,
                    stønadFom = YearMonth.now().minusMonths(3),
                    stønadTom = YearMonth.now(),
                    sats = 5000,
                ),
            )
        var aktørerMedUgyldigEtterbetalingsperiode =
            finnAktørIderMedUgyldigEtterbetalingsperiode(forrigeAndeler, andeler, LocalDateTime.now())

        assertEquals(1, aktørerMedUgyldigEtterbetalingsperiode.size)
        assertEquals(aktørerMedUgyldigEtterbetalingsperiode.single(), barn.aktør.aktørId)

        aktørerMedUgyldigEtterbetalingsperiode =
            finnAktørIderMedUgyldigEtterbetalingsperiode(null, andeler, LocalDateTime.now())

        assertEquals(1, aktørerMedUgyldigEtterbetalingsperiode.size)
        assertEquals(aktørerMedUgyldigEtterbetalingsperiode.single(), barn.aktør.aktørId)
    }

    @Test
    fun `finnAktørIderMedUgyldigEtterbetalingsperiode - skal returnere tom liste over aktører når andel tilkjent ytelse er uendret selv om fom er mer enn 3 mnd tilbake i tid`() {
        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn.aktør,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 5000,
                ),
            )
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn.aktør,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 5000,
                ),
            )
        val aktørerMedUgyldigEtterbetalingsperiode =
            finnAktørIderMedUgyldigEtterbetalingsperiode(forrigeAndeler, andeler, LocalDateTime.now())

        assertEquals(0, aktørerMedUgyldigEtterbetalingsperiode.size)
    }

    @Test
    fun `finnAktørIderMedUgyldigEtterbetalingsperiode - skal returnere tom liste over aktører når andel tilkjent ytelse er endret innenfor siste 3 mnd`() {
        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn.aktør,
                    stønadFom = YearMonth.now().minusMonths(2),
                    stønadTom = YearMonth.now(),
                    sats = 5000,
                ),
            )
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn.aktør,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now(),
                    sats = 4000,
                ),
            )
        val aktørerMedUgyldigEtterbetalingsperiode =
            finnAktørIderMedUgyldigEtterbetalingsperiode(forrigeAndeler, andeler, LocalDateTime.now())

        assertEquals(0, aktørerMedUgyldigEtterbetalingsperiode.size)
    }

    @Test
    fun `Validering ved adopsjonssaker skal være gyldig uavhengig av alder med lovverk fra februar 2025`() {
        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn.aktør,
                    stønadFom = YearMonth.of(2025, 2),
                    stønadTom = YearMonth.of(2025, 8),
                    sats = 5000,
                ),
            )

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andeler)

        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        val barnetsAlderVilkårResultater = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = behandling.id, fødselsdato = LocalDate.of(2024, 2, 1), adopsjonsdato = LocalDate.of(2024, 4, 10))

        assertDoesNotThrow {
            validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                tilkjentYtelse = tilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag,
                alleBarnetsAlderVilkårResultater = barnetsAlderVilkårResultater,
                adopsjonerIBehandling = emptyList(),
                dagensDato = LocalDate.of(2025, 4, 1),
            )
        }
    }

    @Test
    fun `Validering ved adopsjonssaker skal være gyldig uavhengig av alder med lovverk før august 2024`() {
        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn.aktør,
                    stønadFom = YearMonth.of(2023, 9),
                    stønadTom = YearMonth.of(2024, 7),
                    sats = 5000,
                ),
            )

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andeler)

        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        val barnetsAlderVilkårResultater = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = behandling.id, fødselsdato = LocalDate.of(2022, 8, 1), adopsjonsdato = LocalDate.of(2022, 10, 10))

        assertDoesNotThrow {
            validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                tilkjentYtelse = tilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag,
                alleBarnetsAlderVilkårResultater = barnetsAlderVilkårResultater,
                adopsjonerIBehandling = emptyList(),
                dagensDato = LocalDate.of(2025, 4, 1),
            )
        }
    }

    @Test
    fun `Validering ved adopsjonssaker skal være gyldig uavhengig av alder med regelverk august 2024`() {
        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn.aktør,
                    stønadFom = YearMonth.of(2024, 8),
                    stønadTom = YearMonth.of(2025, 2),
                    sats = 5000,
                ),
            )

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andeler)

        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        val barnetsAlderVilkårResultater = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = behandling.id, fødselsdato = LocalDate.of(2023, 7, 1), adopsjonsdato = LocalDate.of(2023, 10, 10))

        assertDoesNotThrow {
            validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                tilkjentYtelse = tilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag,
                alleBarnetsAlderVilkårResultater = barnetsAlderVilkårResultater,
                adopsjonerIBehandling = emptyList(),
                dagensDato = LocalDate.of(2025, 4, 1),
            )
        }
    }

    @Test
    fun `Det skal kastes feil dersom det forsøkes å innvilge mer enn 1 måned fram i tid`() {
        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn.aktør,
                    stønadFom = YearMonth.of(2025, 6),
                    stønadTom = YearMonth.of(2025, 7),
                    sats = 5000,
                ),
            )

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andeler)

        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        val barnetsAlderVilkårResultater = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = behandling.id, fødselsdato = LocalDate.of(2023, 7, 1), adopsjonsdato = LocalDate.of(2023, 10, 10))

        val feilmelding =
            assertThrows<FunksjonellFeil> {
                validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                    tilkjentYtelse = tilkjentYtelse,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    alleBarnetsAlderVilkårResultater = barnetsAlderVilkårResultater,
                    adopsjonerIBehandling = emptyList(),
                    dagensDato = LocalDate.of(2025, 4, 1),
                )
            }.frontendFeilmelding

        assertThat(feilmelding).isEqualTo("Det er ikke mulig å innvilge kontantstøtte for perioder som er lengre enn 2 måneder fram i tid. Dette gjelder barn født 2021-01-01.")
    }

    @Test
    fun `Validering ved adopsjonssaker skal være ugyldig dersom mer enn 7 mnd`() {
        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    aktør = barn.aktør,
                    stønadFom = YearMonth.of(2024, 2),
                    stønadTom = YearMonth.of(2024, 9),
                    sats = 5000,
                ),
            )

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andeler)

        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        val barnetsAlderVilkårResultater = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = behandling.id, fødselsdato = LocalDate.of(2023, 1, 1), adopsjonsdato = LocalDate.of(2023, 10, 10))

        val feil =
            assertThrows<FunksjonellFeil> {
                validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
                    tilkjentYtelse = tilkjentYtelse,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    alleBarnetsAlderVilkårResultater = barnetsAlderVilkårResultater,
                    adopsjonerIBehandling = emptyList(),
                    dagensDato = LocalDate.of(2025, 4, 1),
                )
            }
        assertEquals(
            "Kontantstøtte kan maks utbetales for 7 måneder. Du er i ferd med å utbetale 8 måneder for barn med fnr ${barn.aktør.aktivFødselsnummer()}. " +
                "Kontroller datoene på vilkårene eller ta kontakt med Team BAKS",
            feil.melding,
        )
    }
}
