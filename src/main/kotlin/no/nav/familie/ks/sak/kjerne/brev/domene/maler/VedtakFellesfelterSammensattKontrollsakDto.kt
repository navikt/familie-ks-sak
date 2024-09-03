package no.nav.familie.ks.sak.kjerne.brev.domene.maler

data class VedtakFellesfelterSammensattKontrollsakDto(
    val enhet: String,
    val saksbehandler: String,
    val beslutter: String,
    val søkerNavn: String,
    val søkerFødselsnummer: String,
    val sammensattKontrollsakFritekst: String,
    val korrigertVedtakData: KorrigertVedtakData? = null,
)
