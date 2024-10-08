package no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.kjerne.brev.domene.FellesdataForVedtaksbrev
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevData
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FlettefelterForDokumentDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.KorrigertVedtakData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SignaturVedtak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.flettefelt
import java.time.LocalDate

data class Opphørt(
    override val mal: Brevmal,
    override val data: OpphørtData,
) : VedtaksbrevDto {
    constructor(
        mal: Brevmal = Brevmal.VEDTAK_OPPHØRT,
        fellesdataForVedtaksbrev: FellesdataForVedtaksbrev,
        erFeilutbetalingPåBehandling: Boolean,
    ) :
        this(
            mal = mal,
            data =
                OpphørtData(
                    delmalData =
                        OpphørtData.Delmaler(
                            signaturVedtak =
                                SignaturVedtak(
                                    enhet = fellesdataForVedtaksbrev.enhet,
                                    saksbehandler = fellesdataForVedtaksbrev.saksbehandler,
                                    beslutter = fellesdataForVedtaksbrev.beslutter,
                                ),
                            hjemmeltekst = fellesdataForVedtaksbrev.hjemmeltekst,
                            feilutbetaling = erFeilutbetalingPåBehandling,
                            korrigertVedtak = fellesdataForVedtaksbrev.korrigertVedtakData,
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

data class OpphørtData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentDto,
    override val perioder: List<BrevPeriodeDto>,
) : VedtaksbrevData {
    data class Delmaler(
        override val signaturVedtak: SignaturVedtak,
        override val feilutbetaling: Boolean,
        override val korrigertVedtak: KorrigertVedtakData?,
        val hjemmeltekst: Hjemmeltekst,
    ) : OpphørtDelmaler
}

interface OpphørtDelmaler {
    val signaturVedtak: SignaturVedtak
    val feilutbetaling: Boolean
    val korrigertVedtak: KorrigertVedtakData?
}
