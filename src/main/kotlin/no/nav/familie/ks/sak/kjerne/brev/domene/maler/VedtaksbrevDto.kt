package no.nav.familie.ks.sak.kjerne.brev.domene

import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto

interface VedtaksbrevDto : BrevDto {

    override val mal: Brevmal
    override val data: VedtaksbrevData
}

interface VedtaksbrevData : BrevDataDto {

    val perioder: List<BrevPeriodeDto>
}

enum class EndretUtbetalingBrevPeriodeType(val apiNavn: String) {
    ENDRET_UTBETALINGSPERIODE("endretUtbetalingsperiode"),
    ENDRET_UTBETALINGSPERIODE_DELVIS_UTBETALING("endretUtbetalingsperiodeDelvisUtbetaling"),
    ENDRET_UTBETALINGSPERIODE_INGEN_UTBETALING("endretUtbetalingsperiodeIngenUtbetaling")
}

data class FellesdataForVedtaksbrev(
    val enhet: String,
    val saksbehandler: String,
    val beslutter: String,
    val hjemmeltekst: Hjemmeltekst,
    val søkerNavn: String,
    val søkerFødselsnummer: String,
    val perioder: List<BrevPeriodeDto>,
    val organisasjonsnummer: String? = null,
    val gjelder: String? = null
)
