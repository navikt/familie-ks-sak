package no.nav.familie.ks.sak.kjerne.behandling.steg.simulering

import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import no.nav.familie.ks.sak.api.dto.SimuleringDto
import no.nav.familie.ks.sak.api.dto.SimuleringsPeriodeDto
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringPostering
import java.math.BigDecimal
import java.time.LocalDate

fun filterBortIrrelevanteVedtakSimuleringPosteringer(
    økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>
): List<ØkonomiSimuleringMottaker> = økonomiSimuleringMottakere.map {
    it.copy(
        økonomiSimuleringPostering = it.økonomiSimuleringPostering.filter { postering ->
            postering.posteringType == PosteringType.FEILUTBETALING ||
                postering.posteringType == PosteringType.YTELSE
        }
    )
}

fun List<ØkonomiSimuleringMottaker>.tilSimuleringDto(): SimuleringDto {
    val perioder = this.tilSimuleringsPerioder()
    val tidSimuleringHentet = this.firstOrNull()?.opprettetTidspunkt?.toLocalDate()

    val framtidigePerioder =
        perioder.filter {
            it.fom > tidSimuleringHentet ||
                (it.tom > tidSimuleringHentet && it.forfallsdato > tidSimuleringHentet)
        }

    val nestePeriode = framtidigePerioder.filter { it.feilutbetaling == BigDecimal.ZERO }.minByOrNull { it.fom }
    val tomSisteUtbetaling =
        perioder.filter { nestePeriode == null || it.fom < nestePeriode.fom }.maxOfOrNull { it.tom }

    return SimuleringDto(
        perioder = perioder,
        fomDatoNestePeriode = nestePeriode?.fom,
        etterbetaling = hentTotalEtterbetaling(perioder, nestePeriode?.fom),
        feilutbetaling = hentTotalFeilutbetaling(perioder, nestePeriode?.fom)
            .let { if (it < BigDecimal.ZERO) BigDecimal.ZERO else it },
        fom = perioder.minOfOrNull { it.fom },
        tomDatoNestePeriode = nestePeriode?.tom,
        forfallsdatoNestePeriode = nestePeriode?.forfallsdato,
        tidSimuleringHentet = tidSimuleringHentet,
        tomSisteUtbetaling = tomSisteUtbetaling
    )
}

fun List<ØkonomiSimuleringMottaker>.tilSimuleringsPerioder(): List<SimuleringsPeriodeDto> {
    val simuleringPerioder = mutableMapOf<LocalDate, MutableList<ØkonomiSimuleringPostering>>()

    filterBortIrrelevanteVedtakSimuleringPosteringer(this).forEach {
        it.økonomiSimuleringPostering.forEach { postering ->
            if (simuleringPerioder.containsKey(postering.fom)) {
                simuleringPerioder[postering.fom]?.add(postering)
            } else {
                simuleringPerioder[postering.fom] = mutableListOf(postering)
            }
        }
    }

    val tidSimuleringHentet = this.firstOrNull()?.opprettetTidspunkt?.toLocalDate()

    return simuleringPerioder.map { (fom, posteringListe) ->
        SimuleringsPeriodeDto(
            fom,
            posteringListe[0].tom,
            posteringListe[0].forfallsdato,
            nyttBeløp = hentNyttBeløpIPeriode(posteringListe),
            tidligereUtbetalt = hentTidligereUtbetaltIPeriode(posteringListe),
            resultat = hentResultatIPeriode(posteringListe),
            feilutbetaling = hentPositivFeilbetalingIPeriode(posteringListe),
            etterbetaling = hentEtterbetalingIPeriode(posteringListe, tidSimuleringHentet)
        )
    }
}

fun hentNyttBeløpIPeriode(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    val sumPositiveYtelser = periode.filter { postering ->
        postering.posteringType == PosteringType.YTELSE && postering.beløp > BigDecimal.ZERO
    }.sumOf { it.beløp }
    val feilutbetaling = hentFeilbetalingIPeriode(periode)
    return if (feilutbetaling > BigDecimal.ZERO) sumPositiveYtelser - feilutbetaling else sumPositiveYtelser
}

fun hentFeilbetalingIPeriode(periode: List<ØkonomiSimuleringPostering>) =
    periode.filter { postering ->
        postering.posteringType == PosteringType.FEILUTBETALING
    }.sumOf { it.beløp }

fun hentPositivFeilbetalingIPeriode(periode: List<ØkonomiSimuleringPostering>) =
    periode.filter { postering ->
        postering.posteringType == PosteringType.FEILUTBETALING &&
            postering.beløp > BigDecimal.ZERO
    }.sumOf { it.beløp }

fun hentTidligereUtbetaltIPeriode(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    val sumNegativeYtelser = periode.filter { postering ->
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
    tidSimuleringHentet: LocalDate?
): BigDecimal {
    val periodeHarPositivFeilutbetaling =
        periode.any { it.posteringType == PosteringType.FEILUTBETALING && it.beløp > BigDecimal.ZERO }
    val sumYtelser =
        periode.filter { it.posteringType == PosteringType.YTELSE && it.forfallsdato <= tidSimuleringHentet }
            .sumOf { it.beløp }
    return when {
        periodeHarPositivFeilutbetaling ->
            BigDecimal.ZERO

        else ->
            if (sumYtelser < BigDecimal.ZERO) {
                BigDecimal.ZERO
            } else {
                sumYtelser
            }
    }
}

fun hentTotalEtterbetaling(
    simuleringPerioder: List<SimuleringsPeriodeDto>,
    fomDatoNestePeriode: LocalDate?
): BigDecimal {
    return simuleringPerioder.filter {
        (fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode)
    }.sumOf { it.etterbetaling }.takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ZERO
}

fun hentTotalFeilutbetaling(
    simuleringPerioder: List<SimuleringsPeriodeDto>,
    fomDatoNestePeriode: LocalDate?
): BigDecimal {
    return simuleringPerioder
        .filter { fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode }
        .sumOf { it.feilutbetaling }
}

fun SimuleringMottaker.tilBehandlingSimuleringMottaker(behandling: Behandling): ØkonomiSimuleringMottaker {
    val behandlingSimuleringMottaker = ØkonomiSimuleringMottaker(
        mottakerNummer = this.mottakerNummer,
        mottakerType = this.mottakerType,
        behandling = behandling
    )

    behandlingSimuleringMottaker.økonomiSimuleringPostering = this.simulertPostering.map {
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
        økonomiSimuleringMottaker = økonomiSimuleringMottaker
    )
