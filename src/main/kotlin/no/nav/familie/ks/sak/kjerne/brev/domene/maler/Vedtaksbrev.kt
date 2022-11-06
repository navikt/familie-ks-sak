package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode

interface Vedtaksbrev : BrevDto {

    override val mal: Brevmal
    override val data: VedtaksbrevData
}

interface VedtaksbrevData : BrevDataDto {

    val perioder: List<BrevPeriode>
}

enum class BrevPeriodeType(val apiNavn: String) {
    INNVILGELSE("innvilgelse"),
    INNVILGELSE_INGEN_UTBETALING("innvilgelseIngenUtbetaling"),
    INNVILGELSE_KUN_UTBETALING_PÅ_SØKER("innvilgelseKunUtbetalingPaSoker"),
    OPPHOR("opphor"),
    AVSLAG("avslag"),
    AVSLAG_UTEN_PERIODE("avslagUtenPeriode"),
    FORTSATT_INNVILGET("fortsattInnvilget")
}

enum class EndretUtbetalingBrevPeriodeType(val apiNavn: String) {
    ENDRET_UTBETALINGSPERIODE("endretUtbetalingsperiode"),
    ENDRET_UTBETALINGSPERIODE_DELVIS_UTBETALING("endretUtbetalingsperiodeDelvisUtbetaling"),
    ENDRET_UTBETALINGSPERIODE_INGEN_UTBETALING("endretUtbetalingsperiodeIngenUtbetaling")
}

data class VedtakFellesfelter(
    val enhet: String,
    val saksbehandler: String,
    val beslutter: String,
    val hjemmeltekst: Hjemmeltekst,
    val søkerNavn: String,
    val søkerFødselsnummer: String,
    val perioder: List<BrevPeriode>
)
