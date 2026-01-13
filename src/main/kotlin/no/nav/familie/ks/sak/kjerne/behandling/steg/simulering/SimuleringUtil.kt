package no.nav.familie.ks.sak.kjerne.behandling.steg.simulering

import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import no.nav.familie.kontrakter.felles.tilbakekreving.Varsel
import no.nav.familie.ks.sak.api.dto.SimuleringsPeriodeDto
import no.nav.familie.ks.sak.api.dto.TilbakekrevingRequestDto
import no.nav.familie.ks.sak.api.mapper.SimuleringMapper.tilSimuleringDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringPostering
import java.math.BigDecimal
import java.time.LocalDate

fun filterBortIrrelevanteVedtakSimuleringPosteringer(
    økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>,
): List<ØkonomiSimuleringMottaker> =
    økonomiSimuleringMottakere.map {
        it.copy(
            økonomiSimuleringPostering =
                it.økonomiSimuleringPostering.filter { postering ->
                    postering.posteringType == PosteringType.FEILUTBETALING ||
                        postering.posteringType == PosteringType.YTELSE
                },
        )
    }

fun hentNyttBeløpIPeriode(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    val sumPositiveYtelser =
        periode
            .filter { postering ->
                postering.posteringType == PosteringType.YTELSE && postering.beløp > BigDecimal.ZERO
            }.sumOf { it.beløp }
    val feilutbetaling = hentFeilbetalingIPeriode(periode)
    return if (feilutbetaling > BigDecimal.ZERO) sumPositiveYtelser - feilutbetaling else sumPositiveYtelser
}

fun hentFeilbetalingIPeriode(periode: List<ØkonomiSimuleringPostering>) =
    periode
        .filter { postering ->
            postering.posteringType == PosteringType.FEILUTBETALING
        }.sumOf { it.beløp }

fun hentPositivFeilbetalingIPeriode(periode: List<ØkonomiSimuleringPostering>) =
    periode
        .filter { postering ->
            postering.posteringType == PosteringType.FEILUTBETALING &&
                postering.beløp > BigDecimal.ZERO
        }.sumOf { it.beløp }

fun hentTidligereUtbetaltIPeriode(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    val sumNegativeYtelser =
        periode
            .filter { postering ->
                (postering.posteringType == PosteringType.YTELSE && postering.beløp < BigDecimal.ZERO)
            }.sumOf { it.beløp }
    val feilutbetaling = hentFeilbetalingIPeriode(periode)
    return if (feilutbetaling < BigDecimal.ZERO) -(sumNegativeYtelser - feilutbetaling) else -sumNegativeYtelser
}

fun hentResultatIPeriode(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    val feilutbetaling = hentFeilbetalingIPeriode(periode)

    return if (feilutbetaling > BigDecimal.ZERO) {
        -feilutbetaling
    } else {
        hentNyttBeløpIPeriode(periode) - hentTidligereUtbetaltIPeriode(periode)
    }
}

fun hentEtterbetalingIPeriode(
    periode: List<ØkonomiSimuleringPostering>,
    tidSimuleringHentet: LocalDate?,
): BigDecimal {
    val periodeHarPositivFeilutbetaling =
        periode.any { it.posteringType == PosteringType.FEILUTBETALING && it.beløp > BigDecimal.ZERO }
    val sumYtelser =
        periode
            .filter { it.posteringType == PosteringType.YTELSE && it.forfallsdato <= tidSimuleringHentet }
            .sumOf { it.beløp }
    return when {
        periodeHarPositivFeilutbetaling -> {
            BigDecimal.ZERO
        }

        else -> {
            if (sumYtelser < BigDecimal.ZERO) {
                BigDecimal.ZERO
            } else {
                sumYtelser
            }
        }
    }
}

fun hentTotalEtterbetaling(
    simuleringPerioder: List<SimuleringsPeriodeDto>,
    fomDatoNestePeriode: LocalDate?,
): BigDecimal =
    simuleringPerioder
        .filter {
            (fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode)
        }.sumOf { it.etterbetaling }
        .takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ZERO

fun hentTotalFeilutbetaling(
    simuleringPerioder: List<SimuleringsPeriodeDto>,
    fomDatoNestePeriode: LocalDate?,
): BigDecimal =
    simuleringPerioder
        .filter { fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode }
        .sumOf { it.feilutbetaling }

fun hentManuellPosteringIPeriode(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    val sumManuellePosteringer =
        periode
            .filter { it.posteringType == PosteringType.YTELSE }
            .filter { it.erManuellPostering }
            .sumOf { it.beløp }

    val manuellFeilutbetaling = hentManuellFeilutbetalingIPeriode(periode)

    return sumManuellePosteringer - manuellFeilutbetaling
}

private fun hentManuellFeilutbetalingIPeriode(periode: List<ØkonomiSimuleringPostering>) =
    periode
        .filter { it.posteringType == PosteringType.FEILUTBETALING }
        .filter { it.erManuellPostering }
        .sumOf { it.beløp }

fun SimuleringMottaker.tilBehandlingSimuleringMottaker(behandling: Behandling): ØkonomiSimuleringMottaker {
    val behandlingSimuleringMottaker =
        ØkonomiSimuleringMottaker(
            mottakerNummer = this.mottakerNummer,
            mottakerType = this.mottakerType,
            behandling = behandling,
        )

    behandlingSimuleringMottaker.økonomiSimuleringPostering =
        this.simulertPostering.map {
            it.tilVedtakSimuleringPostering(behandlingSimuleringMottaker)
        }

    return behandlingSimuleringMottaker
}

fun SimulertPostering.tilVedtakSimuleringPostering(økonomiSimuleringMottaker: ØkonomiSimuleringMottaker) =
    ØkonomiSimuleringPostering(
        beløp = this.beløp,
        betalingType = this.betalingType,
        fagOmrådeKode = this.fagOmrådeKode,
        fom = this.fom,
        tom = this.tom,
        posteringType = this.posteringType,
        forfallsdato = this.forfallsdato,
        utenInntrekk = this.utenInntrekk,
        økonomiSimuleringMottaker = økonomiSimuleringMottaker,
    )

fun validerTilbakekrevingData(
    tilbakekrevingRequestDto: TilbakekrevingRequestDto?,
    feilutbetaling: BigDecimal,
) {
    if (feilutbetaling == BigDecimal.ZERO && tilbakekrevingRequestDto != null) {
        throw FunksjonellFeil(
            "Simuleringen har ikke en feilutbetaling, men tilbakekrevingDto var ikke null",
            frontendFeilmelding = "Du kan ikke opprette en tilbakekreving når det ikke er en feilutbetaling.",
        )
    }
}

fun hentTilbakekrevingsperioderISimulering(simulering: List<ØkonomiSimuleringMottaker>): List<Periode> {
    val tilbakekrevingsperioder = mutableListOf<Periode>()
    val feilutbetaltePerioder =
        simulering
            .tilSimuleringDto()
            .perioder
            .filter { it.feilutbetaling != BigDecimal.ZERO }
            .sortedBy { it.fom }

    var aktuellFom = feilutbetaltePerioder.first().fom
    var aktuellTom = feilutbetaltePerioder.first().tom

    feilutbetaltePerioder.forEach { periode ->
        if (aktuellTom.toYearMonth().plusMonths(1) < periode.fom.toYearMonth()) {
            tilbakekrevingsperioder.add(Periode(aktuellFom, aktuellTom))
            aktuellFom = periode.fom
        }
        aktuellTom = periode.tom
    }
    tilbakekrevingsperioder.add(Periode(aktuellFom, aktuellTom))
    return tilbakekrevingsperioder
}

fun opprettVarsel(
    varselTekst: String,
    simulering: List<ØkonomiSimuleringMottaker>,
): Varsel =
    Varsel(
        varseltekst = varselTekst,
        sumFeilutbetaling = simulering.tilSimuleringDto().feilutbetaling,
        perioder = hentTilbakekrevingsperioderISimulering(simulering),
    )
