package no.nav.familie.ks.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode

interface Vedtaksbrev : Brev {

    override val mal: Brevmal
    override val data: VedtaksbrevData
}

interface VedtaksbrevData : BrevData {

    val perioder: List<BrevPeriode>
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
    val perioder: List<BrevPeriode>,
    val organisasjonsnummer: String? = null,
    val gjelder: String? = null
)
