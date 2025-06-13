package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.overlapperHeltEllerDelvisMed
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidator
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.slåSammen
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.YearMonth

@Service
class AndelerTilkjentYtelseOgEndreteUtbetalingerService(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
) {
    @Transactional
    fun finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId: Long): List<AndelTilkjentYtelseMedEndreteUtbetalinger> = lagKombinator(behandlingId).lagAndelerMedEndringer()

    @Transactional
    fun finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId: Long): List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> =
        lagKombinator(behandlingId).lagEndreteUtbetalingMedAndeler().map { endretUtbetalingAndelMedAndelTilkjentYtelse ->
            endretUtbetalingAndelMedAndelTilkjentYtelse
                .utenAndelerVedValideringsfeil {
                    EndretUtbetalingAndelValidator.validerPeriodeInnenforTilkjentYtelse(
                        endretUtbetalingAndelMedAndelTilkjentYtelse.endretUtbetaling,
                        endretUtbetalingAndelMedAndelTilkjentYtelse.andelerTilkjentYtelse,
                    )
                }.utenAndelerVedValideringsfeil {
                    EndretUtbetalingAndelValidator.validerÅrsak(
                        årsak = endretUtbetalingAndelMedAndelTilkjentYtelse.årsak,
                        endretUtbetalingAndel = endretUtbetalingAndelMedAndelTilkjentYtelse.endretUtbetaling,
                        vilkårsvurdering = vilkårsvurderingRepository.finnAktivForBehandling(behandlingId),
                    )
                }
        }

    private fun lagKombinator(behandlingId: Long) =
        AndelTilkjentYtelseOgEndreteUtbetalingerKombinator(
            andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId),
            endretUtbetalingAndeler = endretUtbetalingAndelRepository.hentEndretUtbetalingerForBehandling(behandlingId),
        )

    /**
     * Fjerner andelene hvis det funksjonen som sendes inn kaster en exception
     * Brukes som en wrapper rundt en del valideringsfunksjoner som kaster exception når ting ikke validerer
     * Manglende andeler brukes et par steder som et signal om at noe er feil
     */
    private fun EndretUtbetalingAndelMedAndelerTilkjentYtelse.utenAndelerVedValideringsfeil(
        validator: () -> Unit,
    ): EndretUtbetalingAndelMedAndelerTilkjentYtelse =
        try {
            validator()
            this
        } catch (e: Throwable) {
            this.copy(andeler = emptyList())
        }
}

fun Collection<AndelTilkjentYtelse>.tilAndelerTilkjentYtelseMedEndreteUtbetalinger(endretUtbetalingAndeler: Collection<EndretUtbetalingAndel>) = AndelTilkjentYtelseOgEndreteUtbetalingerKombinator(this, endretUtbetalingAndeler).lagAndelerMedEndringer()

fun Collection<EndretUtbetalingAndel>.tilEndretUtbetalingAndelMedAndelerTilkjentYtelse(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>) = AndelTilkjentYtelseOgEndreteUtbetalingerKombinator(andelerTilkjentYtelse, this).lagEndreteUtbetalingMedAndeler()

private class AndelTilkjentYtelseOgEndreteUtbetalingerKombinator(
    private val andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    private val endretUtbetalingAndeler: Collection<EndretUtbetalingAndel>,
) {
    fun lagAndelerMedEndringer(): List<AndelTilkjentYtelseMedEndreteUtbetalinger> = andelerTilkjentYtelse.map { lagAndelMedEndringer(it) }

    fun lagEndreteUtbetalingMedAndeler(): List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> = endretUtbetalingAndeler.map { lagEndringMedAndeler(it) }

    private fun lagAndelMedEndringer(andelTilkjentYtelse: AndelTilkjentYtelse): AndelTilkjentYtelseMedEndreteUtbetalinger {
        val endreteUtbetalinger = endretUtbetalingAndeler.filter { overlapper(andelTilkjentYtelse, it) }

        return AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelse, endreteUtbetalinger)
    }

    private fun lagEndringMedAndeler(endretUtbetalingAndel: EndretUtbetalingAndel): EndretUtbetalingAndelMedAndelerTilkjentYtelse {
        val andeler = andelerTilkjentYtelse.filter { overlapper(it, endretUtbetalingAndel) }

        return EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel, andeler)
    }

    private fun overlapper(
        andelTilkjentYtelse: AndelTilkjentYtelse,
        endretUtbetalingAndel: EndretUtbetalingAndel,
    ): Boolean =
        endretUtbetalingAndel.personer.any { person -> person.aktør == andelTilkjentYtelse.aktør } &&
            endretUtbetalingAndel.fom != null &&
            endretUtbetalingAndel.tom != null &&
            endretUtbetalingAndel.periode.overlapperHeltEllerDelvisMed(andelTilkjentYtelse.periode)
}

data class AndelTilkjentYtelseMedEndreteUtbetalinger(
    private val andelTilkjentYtelse: AndelTilkjentYtelse,
    private val endreteUtbetalingerAndeler: List<EndretUtbetalingAndel>,
) {
    val periodeOffset get() = andelTilkjentYtelse.periodeOffset
    val sats get() = andelTilkjentYtelse.sats
    val type get() = andelTilkjentYtelse.type
    val kalkulertUtbetalingsbeløp get() = andelTilkjentYtelse.kalkulertUtbetalingsbeløp
    val aktør get() = andelTilkjentYtelse.aktør

    fun medTom(tom: YearMonth): AndelTilkjentYtelseMedEndreteUtbetalinger = AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelse.copy(stønadTom = tom), endreteUtbetalinger)

    val stønadFom get() = andelTilkjentYtelse.stønadFom
    val stønadTom get() = andelTilkjentYtelse.stønadTom
    val prosent get() = andelTilkjentYtelse.prosent
    val andel get() = andelTilkjentYtelse
    val endreteUtbetalinger get() = endreteUtbetalingerAndeler

    val erInnvilget get() = this.endreteUtbetalinger.none { it.prosent == BigDecimal.ZERO }

    companion object {
        fun utenEndringer(andelTilkjentYtelse: AndelTilkjentYtelse): AndelTilkjentYtelseMedEndreteUtbetalinger = AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelse, emptyList())
    }
}

data class EndretUtbetalingAndelMedAndelerTilkjentYtelse(
    val endretUtbetalingAndel: EndretUtbetalingAndel,
    private val andeler: List<AndelTilkjentYtelse>,
) {
    fun overlapperMed(månedPeriode: MånedPeriode) = endretUtbetalingAndel.overlapperMed(månedPeriode)

    val periode get() = endretUtbetalingAndel.periode
    val personer get() = endretUtbetalingAndel.personer
    val begrunnelse get() = endretUtbetalingAndel.begrunnelse
    val vedtaksbegrunnelser get() = endretUtbetalingAndel.vedtaksbegrunnelser
    val søknadstidspunkt get() = endretUtbetalingAndel.søknadstidspunkt
    val prosent get() = endretUtbetalingAndel.prosent
    val personIdenter get() = endretUtbetalingAndel.personer.map { it.aktør.aktivFødselsnummer() }
    val årsak get() = endretUtbetalingAndel.årsak
    val id get() = endretUtbetalingAndel.id
    val fom get() = endretUtbetalingAndel.fom
    val tom get() = endretUtbetalingAndel.tom
    val erEksplisittAvslagPåSøknad get() = endretUtbetalingAndel.erEksplisittAvslagPåSøknad
    val endretUtbetaling get() = endretUtbetalingAndel
    val andelerTilkjentYtelse get() = andeler
}

/**
 * Hjelpefunksjon som oppretter AndelTilkjentYtelseMedEndreteUtbetalinger fra AndelTilkjentYtelse og legger til en endring.
 * Utnytter at <endretUtbetalingAndelMedAndelerTilkjentYtelse> vet om funksjonsbryteren <brukFrikobleteAndelerOgEndringer> er satt
 * og viderefører den til den opprettede AndelTilkjentYtelseMedEndreteUtbetalinger
 */
fun AndelTilkjentYtelse.medEndring(endretUtbetalingAndelMedAndelerTilkjentYtelse: EndretUtbetalingAndelMedAndelerTilkjentYtelse) =
    AndelTilkjentYtelseMedEndreteUtbetalinger(
        this,
        listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse.endretUtbetaling),
    )

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilTidslinjer(): List<Tidslinje<AndelTilkjentYtelseMedEndreteUtbetalinger>> =
    this.map {
        listOf(
            Periode(
                verdi = it,
                fom = it.stønadFom.førsteDagIInneværendeMåned(),
                tom = it.stønadTom.sisteDagIInneværendeMåned(),
            ),
        ).tilTidslinje()
    }

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.lagVertikalePerioder(): Tidslinje<Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>> = this.tilTidslinjer().slåSammen()

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilKombinertTidslinjePerAktør(): Tidslinje<Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>> {
    val andelTilkjentYtelsePerPerson = groupBy { it.aktør }

    val tidslinjer = andelTilkjentYtelsePerPerson.values.map { it.tilTidslinje() }

    return tidslinjer.slåSammen()
}

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilTidslinje() =
    this
        .map {
            Periode(
                it,
                it.stønadFom.førsteDagIInneværendeMåned(),
                it.stønadTom.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()

fun Collection<EndretUtbetalingAndelMedAndelerTilkjentYtelse>.tilTidslinje(): Tidslinje<EndretUtbetalingAndelMedAndelerTilkjentYtelse> =
    this
        .map {
            Periode(
                verdi = it,
                fom = it.fom?.førsteDagIInneværendeMåned(),
                tom = it.tom?.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()
