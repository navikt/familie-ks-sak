package no.nav.familie.ks.sak.kjerne.endretutbetaling

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

object EndretUtbetalingAndelValidator {
    fun validerPeriodeInnenforTilkjentYtelse(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        andelTilkjentYtelser: List<AndelTilkjentYtelse>,
    ) {
        endretUtbetalingAndel.validerUtfyltEndring()

        val feilMelding = "Det er ingen tilkjent ytelse for personen det blir forsøkt lagt til en endret periode for."
        val frontendFeilMelding =
            "Du har valgt en periode der det ikke finnes tilkjent ytelse for valgt person " +
                "i hele eller deler av perioden."
        endretUtbetalingAndel.personer.forEach { person ->
            val minsteDatoForTilkjentYtelse =
                andelTilkjentYtelser
                    .filter { it.aktør == person.aktør }
                    .minByOrNull { it.stønadFom }
                    ?.stønadFom
                    ?: throw FunksjonellFeil(melding = feilMelding, frontendFeilmelding = frontendFeilMelding)

            val størsteDatoForTilkjentYtelse =
                andelTilkjentYtelser
                    .filter { it.aktør == person.aktør }
                    .maxByOrNull { it.stønadTom }
                    ?.stønadTom
                    ?: throw FunksjonellFeil(melding = feilMelding, frontendFeilmelding = frontendFeilMelding)

            if (checkNotNull(endretUtbetalingAndel.fom).isBefore(minsteDatoForTilkjentYtelse) ||
                checkNotNull(endretUtbetalingAndel.tom).isAfter(størsteDatoForTilkjentYtelse)
            ) {
                throw FunksjonellFeil(melding = feilMelding, frontendFeilmelding = frontendFeilMelding)
            }
        }
    }

    fun validerÅrsak(
        årsak: Årsak?,
        endretUtbetalingAndel: EndretUtbetalingAndel,
        vilkårsvurdering: Vilkårsvurdering?,
    ) {
        checkNotNull(årsak) { "Årsak kan ikke være null" }
        when (årsak) {
            Årsak.ETTERBETALING_3MND -> {
                validerEtterbetaling3Måned(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    kravDato = vilkårsvurdering?.behandling?.opprettetTidspunkt ?: LocalDateTime.now(),
                )
            }

            Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 -> {
                validerFulltidsplassIBarnehageAugust2024(
                    endretUtbetalingAndel,
                )
            }

            Årsak.ALLEREDE_UTBETALT -> {
                validerAlleredeUtbetalt(endretUtbetalingAndel = endretUtbetalingAndel)
            }
        }
    }

    private fun validerFulltidsplassIBarnehageAugust2024(
        endretUtbetalingAndel: EndretUtbetalingAndel,
    ) {
        val august2024 = YearMonth.of(2024, 8)
        if (endretUtbetalingAndel.fom != august2024 || endretUtbetalingAndel.tom != august2024) {
            val årsak = endretUtbetalingAndel.årsak ?: throw FunksjonellFeil("Årsak må være satt")
            throw FunksjonellFeil("Årsak \"${årsak.visningsnavn}\" er bare mulig å sette til august 2024")
        }
    }

    private fun validerAlleredeUtbetalt(
        endretUtbetalingAndel: EndretUtbetalingAndel,
    ) {
        if (endretUtbetalingAndel.tom?.isAfter(YearMonth.now()) == true) {
            throw FunksjonellFeil("Du har valgt årsaken allerede utbetalt. Du kan ikke velge denne årsaken og en til og med dato frem i tid. Ta kontakt med superbruker om du er usikker på hva du skal gjøre.")
        }
    }

    fun validerAtAlleOpprettedeEndringerErUtfylt(endretUtbetalingAndeler: List<EndretUtbetalingAndel>) {
        runCatching {
            endretUtbetalingAndeler.forEach { it.validerUtfyltEndring() }
        }.onFailure {
            throw FunksjonellFeil(
                melding = "Det er opprettet instanser av EndretUtbetalingandel som ikke er fylt ut før navigering til neste steg.",
                frontendFeilmelding =
                    "Du har opprettet en eller flere endrede utbetalingsperioder " +
                        "som er ufullstendig utfylt. Disse må enten fylles ut eller slettes før du kan gå videre.",
            )
        }
    }

    fun validerAtEndringerErTilknyttetAndelTilkjentYtelse(endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>) {
        if (endretUtbetalingAndeler.any { it.andelerTilkjentYtelse.isEmpty() }) {
            throw FunksjonellFeil(
                melding =
                    "Det er opprettet instanser av EndretUtbetalingandel som ikke er tilknyttet noen andeler. " +
                        "De må enten lagres eller slettes av SB.",
                frontendFeilmelding = "Du har endrede utbetalingsperioder. Bekreft, slett eller oppdater periodene i listen.",
            )
        }
    }

    private fun validerEtterbetaling3Måned(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        kravDato: LocalDateTime,
    ) {
        if (endretUtbetalingAndel.prosent != BigDecimal.ZERO) {
            throw FunksjonellFeil(
                "Du kan ikke sette årsak etterbetaling 3 måned når du har valgt at perioden skal utbetales.",
            )
        } else if (
            endretUtbetalingAndel.tom?.isAfter(kravDato.minusMonths(3).toLocalDate().toYearMonth()) == true
        ) {
            throw FunksjonellFeil(
                "Du kan ikke stoppe etterbetaling for en periode som ikke strekker seg mer enn 3 måned tilbake i tid.",
            )
        }
    }

    fun validerTomDato(
        tomDato: YearMonth,
    ) {
        val dagensDato = YearMonth.now()
        if (tomDato.isAfter(dagensDato)) {
            val feilmelding = "Du kan ikke legge inn til og med dato som er i neste måned eller senere."

            throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
        }
    }

    fun validerIngenOverlappendeEndring(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        eksisterendeEndringerPåBehandling: List<EndretUtbetalingAndel>,
    ) {
        endretUtbetalingAndel.validerUtfyltEndring()
        if (eksisterendeEndringerPåBehandling.any
                {
                    it.overlapperMed(endretUtbetalingAndel.periode) &&
                        it.personer.intersect(endretUtbetalingAndel.personer).isNotEmpty()
                }
        ) {
            throw FunksjonellFeil(
                melding = "Perioden som blir forsøkt lagt til overlapper med eksisterende periode på person.",
                frontendFeilmelding = "Perioden du forsøker å legge til overlapper med eksisterende periode på personen. Om dette er ønskelig må du først endre den eksisterende perioden.",
            )
        }
    }
}
