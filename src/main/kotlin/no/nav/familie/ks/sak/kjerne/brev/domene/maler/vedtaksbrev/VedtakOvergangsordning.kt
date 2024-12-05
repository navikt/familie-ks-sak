package no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.kjerne.brev.domene.FellesdataForVedtaksbrev
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevData
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FlettefelterForDokumentDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SignaturVedtak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.UtbetalingOvergangsordning
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.flettefelt
import java.time.LocalDate

data class VedtakOvergangsordning(
    override val mal: Brevmal,
    override val data: OvergangsordningVedtakData,
) : VedtaksbrevDto {
    constructor(
        mal: Brevmal = Brevmal.VEDTAK_OVERGANGSORDNING,
        fellesdataForVedtaksbrev: FellesdataForVedtaksbrev,
        utbetalingOvergangsordning: UtbetalingOvergangsordning,
    ) :
        this(
            mal = mal,
            data =
                OvergangsordningVedtakData(
                    delmalData =
                        OvergangsordningVedtakData.Delmaler(
                            signaturVedtak =
                                SignaturVedtak(
                                    enhet = fellesdataForVedtaksbrev.enhet,
                                    saksbehandler = fellesdataForVedtaksbrev.saksbehandler,
                                    beslutter = fellesdataForVedtaksbrev.beslutter,
                                ),
                            hjemmeltekst = fellesdataForVedtaksbrev.hjemmeltekst,
                            utbetalingOvergangsordning = utbetalingOvergangsordning,
                        ),
                    flettefelter =
                        object : FlettefelterForDokumentDto {
                            override val navn = flettefelt(fellesdataForVedtaksbrev.søkerNavn)
                            override val fodselsnummer = flettefelt(fellesdataForVedtaksbrev.søkerFødselsnummer)
                            override val brevOpprettetDato = flettefelt(LocalDate.now().tilDagMånedÅr())
                        },
                    perioder = fellesdataForVedtaksbrev.perioder,
                ),
        )
}

data class OvergangsordningVedtakData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentDto,
    override val perioder: List<BrevPeriodeDto>,
) : VedtaksbrevData {
    data class Delmaler(
        override val signaturVedtak: SignaturVedtak,
        override val utbetalingOvergangsordning: UtbetalingOvergangsordning?,
        val hjemmeltekst: Hjemmeltekst,
    ) : OvergangsordningVedtakDelmaler
}

interface OvergangsordningVedtakDelmaler {
    val signaturVedtak: SignaturVedtak
    val utbetalingOvergangsordning: UtbetalingOvergangsordning?
}
