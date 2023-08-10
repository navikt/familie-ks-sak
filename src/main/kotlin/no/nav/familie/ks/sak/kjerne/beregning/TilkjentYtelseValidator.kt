package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.exception.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ks.sak.common.exception.UtbetalingsikkerhetFeil
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.overlapperHeltEllerDelvisMed
import no.nav.familie.ks.sak.common.util.toLocalDate
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.maksBeløp
import no.nav.familie.ks.sak.kjerne.beregning.domene.tilTidslinjeMedAndeler
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilBrevTekst
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.Period
import java.time.YearMonth

object TilkjentYtelseValidator {

    fun validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
        tilkjentYtelse: TilkjentYtelse,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ) {
        val søker = personopplysningGrunnlag.søker
        val barna = personopplysningGrunnlag.barna

        val andelerPerAktør = tilkjentYtelse.andelerTilkjentYtelse.groupBy { it.aktør }

        andelerPerAktør.filter { it.value.isNotEmpty() }.forEach { (aktør, andeler) ->
            val stønadFom = andeler.minOf { it.stønadFom }
            val stønadTom = andeler.maxOf { it.stønadTom }

            val diff = Period.between(stønadFom.toLocalDate(), stønadTom.toLocalDate())
            if (diff.toTotalMonths() > 11) {
                val feilmelding =
                    "Kontantstøtte kan maks utbetales for 11 måneder. Du er i ferd med å utbetale mer enn dette for barn med fnr ${aktør.aktivFødselsnummer()}. " +
                        "Kontroller datoene på vilkårene eller ta kontakt med team familie"
                throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
            }
        }

        val tidslinjeMedAndeler = tilkjentYtelse.tilTidslinjeMedAndeler()

        tidslinjeMedAndeler.tilPerioder().forEach {
            val andeler = it.verdi?.toList() ?: emptyList()

            if (hentSøkersAndeler(andeler, søker).isNotEmpty()) {
                throw Feil("Feil i beregning. Søkers andeler må være tom")
            }
            val barnasAndeler = hentBarnasAndeler(andeler, barna)
            barnasAndeler.forEach { (_, andeler) -> validerAtBeløpForPartStemmerMedSatser(andeler) }
        }
    }

    private fun hentSøkersAndeler(
        andeler: List<AndelTilkjentYtelse>,
        søker: Person
    ) = andeler.filter { it.aktør == søker.aktør }

    private fun hentBarnasAndeler(andeler: List<AndelTilkjentYtelse>, barna: List<Person>) = barna.map { barn ->
        barn to andeler.filter { it.aktør == barn.aktør }
    }

    private fun validerAtBeløpForPartStemmerMedSatser(andeler: List<AndelTilkjentYtelse>) {
        val maksAntallAndeler = 1
        val maksTotalBeløp = maksBeløp()
        if (andeler.isEmpty()) return

        val feilmelding =
            "Validering av andeler for BARN i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet"
        val frontendFeilmelding =
            "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. $KONTAKT_TEAMET_SUFFIX"

        val totalbeløp = andeler.map { it.kalkulertUtbetalingsbeløp }.fold(0) { sum, beløp -> sum + beløp }

        when {
            andeler.size > maksAntallAndeler -> {
                throw UtbetalingsikkerhetFeil(
                    melding = "$feilmelding: Tillatte andeler = $maksAntallAndeler, faktiske andeler = ${andeler.size}.",
                    frontendFeilmelding = frontendFeilmelding
                )
            }

            totalbeløp > maksTotalBeløp -> {
                throw UtbetalingsikkerhetFeil(
                    melding = "$feilmelding: Tillatt totalbeløp = $maksTotalBeløp, faktiske totalbeløp = $totalbeløp.",
                    frontendFeilmelding = frontendFeilmelding
                )
            }
        }
    }

    fun validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
        tilkjentYtelseForBehandling: TilkjentYtelse,
        barnMedAndreRelevanteTilkjentYtelser: List<Pair<Person, List<TilkjentYtelse>>>,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ) {
        val barna = personopplysningGrunnlag.barna.sortedBy { it.fødselsdato }

        val barnasAndeler = hentBarnasAndeler(tilkjentYtelseForBehandling.andelerTilkjentYtelse.toList(), barna)

        val barnMedUtbetalingsikkerhetFeil = mutableListOf<Person>()
        barnasAndeler.forEach { (barn, andeler) ->
            val barnsAndelerFraAndreBehandlinger =
                barnMedAndreRelevanteTilkjentYtelser.filter { it.first.aktør == barn.aktør }
                    .flatMap { it.second }
                    .flatMap { it.andelerTilkjentYtelse }
                    .filter { it.aktør == barn.aktør }

            if (erOverlappAvAndeler(
                    andeler = andeler,
                    andelerFraAndreBehandlinger = barnsAndelerFraAndreBehandlinger
                )
            ) {
                barnMedUtbetalingsikkerhetFeil.add(barn)
            }
        }
        if (barnMedUtbetalingsikkerhetFeil.isNotEmpty()) {
            throw UtbetalingsikkerhetFeil(
                melding = "Vi finner utbetalinger som overstiger 100% på hvert av barna: ${
                barnMedUtbetalingsikkerhetFeil.map { it.fødselsdato }.tilBrevTekst()
                }",
                frontendFeilmelding = "Du kan ikke godkjenne dette vedtaket fordi det vil betales ut mer enn 100% for barn født ${
                barnMedUtbetalingsikkerhetFeil.map { it.fødselsdato }.tilBrevTekst()
                }. Reduksjonsvedtak til annen person må være sendt til godkjenning før du kan gå videre."
            )
        }
    }

    private fun erOverlappAvAndeler(
        andeler: List<AndelTilkjentYtelse>,
        andelerFraAndreBehandlinger: List<AndelTilkjentYtelse>
    ): Boolean {
        return andeler.any { andelTilkjentYtelse ->
            andelerFraAndreBehandlinger.any {
                andelTilkjentYtelse.overlapperMed(it) &&
                    andelTilkjentYtelse.prosent + it.prosent > BigDecimal(100)
            }
        }
    }

    fun finnAktørIderMedUgyldigEtterbetalingsperiode(
        forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>?,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        kravDato: LocalDateTime
    ): List<String> {
        val gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(kravDato)

        val aktørIder =
            hentAktørIderForDenneOgForrigeBehandling(andelerTilkjentYtelse, forrigeAndelerTilkjentYtelse)

        val personerMedUgyldigEtterbetaling =
            aktørIder.mapNotNull { aktørId ->
                val andelerTilkjentYtelseForPerson = andelerTilkjentYtelse.filter { it.aktør.aktørId == aktørId }
                val forrigeAndelerTilkjentYtelseForPerson =
                    forrigeAndelerTilkjentYtelse?.filter { it.aktør.aktørId == aktørId }

                val etterbetalingErUgyldig = erUgyldigEtterbetalingPåPerson(
                    forrigeAndelerTilkjentYtelseForPerson,
                    andelerTilkjentYtelseForPerson,
                    gyldigEtterbetalingFom
                )

                if (etterbetalingErUgyldig) {
                    aktørId
                } else {
                    null
                }
            }

        return personerMedUgyldigEtterbetaling
    }

    private fun hentAktørIderForDenneOgForrigeBehandling(
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>?
    ): Set<String> {
        val aktørIderFraAndeler = andelerTilkjentYtelse.map { it.aktør.aktørId }
        val aktøerIderFraForrigeAndeler = forrigeAndelerTilkjentYtelse?.map { it.aktør.aktørId } ?: emptyList()
        return (aktørIderFraAndeler + aktøerIderFraForrigeAndeler).toSet()
    }

    private fun erUgyldigEtterbetalingPåPerson(
        forrigeAndelerForPerson: List<AndelTilkjentYtelse>?,
        andelerForPerson: List<AndelTilkjentYtelse>,
        gyldigEtterbetalingFom: YearMonth?
    ): Boolean {
        return YtelseType.values().any { ytelseType ->
            val forrigeAndelerForPersonOgType = forrigeAndelerForPerson?.filter { it.type == ytelseType } ?: emptyList()
            val andelerForPersonOgType = andelerForPerson.filter { it.type == ytelseType }

            val forrigeAndelerTidslinje = forrigeAndelerForPersonOgType.map {
                Periode(
                    it,
                    it.stønadFom.toLocalDate(),
                    it.stønadTom.toLocalDate()
                )
            }.tilTidslinje()
            val andelerTidslinje =
                andelerForPersonOgType.map { Periode(it, it.stønadFom.toLocalDate(), it.stønadTom.toLocalDate()) }
                    .tilTidslinje()

            val erAndelMedØktBeløpFørGyldigEtterbetalingsdato =
                erAndelMedØktBeløpFørDato(
                    forrigeAndeler = forrigeAndelerForPersonOgType,
                    andeler = andelerForPersonOgType,
                    måned = gyldigEtterbetalingFom
                )

            val segmenterLagtTil =
                andelerTidslinje.kombinerMed(forrigeAndelerTidslinje) { andel1, andel2 ->
                    if (andel2 == null) andel1 else null
                }.tilPerioderIkkeNull()

            val erLagtTilSegmentFørGyldigEtterbetalingsdato =
                segmenterLagtTil.any { it.verdi.stønadFom < gyldigEtterbetalingFom && it.verdi.kalkulertUtbetalingsbeløp > 0 }

            erAndelMedØktBeløpFørGyldigEtterbetalingsdato || erLagtTilSegmentFørGyldigEtterbetalingsdato
        }
    }

    private fun hentGyldigEtterbetalingFom(kravDato: LocalDateTime) =
        kravDato.minusMonths(3)
            .toLocalDate()
            .toYearMonth()

    private fun erAndelMedØktBeløpFørDato(
        forrigeAndeler: List<AndelTilkjentYtelse>?,
        andeler: List<AndelTilkjentYtelse>,
        måned: YearMonth?
    ): Boolean = andeler
        .filter { it.stønadFom < måned }
        .any { andel ->
            forrigeAndeler?.any {
                it.periode.overlapperHeltEllerDelvisMed(andel.periode) &&
                    it.kalkulertUtbetalingsbeløp < andel.kalkulertUtbetalingsbeløp
            } ?: false
        }
}
