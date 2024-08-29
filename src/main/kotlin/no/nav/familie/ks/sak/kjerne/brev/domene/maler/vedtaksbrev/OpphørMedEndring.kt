package no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.kjerne.brev.domene.FellesdataForVedtaksbrev
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevData
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Etterbetaling
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FlettefelterForDokumentDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.KorrigertVedtakData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.RefusjonEøsAvklart
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.RefusjonEøsUavklart
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SignaturVedtak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.flettefelt
import java.time.LocalDate

data class OpphørMedEndring(
    override val mal: Brevmal,
    override val data: OpphørMedEndringData,
) : VedtaksbrevDto {
    constructor(
        mal: Brevmal = Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
        fellesdataForVedtaksbrev: FellesdataForVedtaksbrev,
        etterbetaling: Etterbetaling? = null,
        erFeilutbetalingPåBehandling: Boolean,
        refusjonEosAvklart: RefusjonEøsAvklart? = null,
        refusjonEosUavklart: RefusjonEøsUavklart? = null,
        erKlage: Boolean,
    ) :
        this(
            mal = mal,
            data =
                OpphørMedEndringData(
                    delmalData =
                        OpphørMedEndringData.Delmaler(
                            signaturVedtak =
                                SignaturVedtak(
                                    enhet = fellesdataForVedtaksbrev.enhet,
                                    saksbehandler = fellesdataForVedtaksbrev.saksbehandler,
                                    beslutter = fellesdataForVedtaksbrev.beslutter,
                                ),
                            etterbetaling = etterbetaling,
                            hjemmeltekst = fellesdataForVedtaksbrev.hjemmeltekst,
                            feilutbetaling = erFeilutbetalingPåBehandling,
                            korrigertVedtak = fellesdataForVedtaksbrev.korrigertVedtakData,
                            refusjonEosAvklart = refusjonEosAvklart,
                            refusjonEosUavklart = refusjonEosUavklart,
                            klage = erKlage,
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

data class OpphørMedEndringData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentDto,
    override val perioder: List<BrevPeriodeDto>,
) : VedtaksbrevData {
    data class Delmaler(
        override val signaturVedtak: SignaturVedtak,
        override val feilutbetaling: Boolean,
        override val etterbetaling: Etterbetaling?,
        override val korrigertVedtak: KorrigertVedtakData?,
        override val refusjonEosAvklart: RefusjonEøsAvklart?,
        override val refusjonEosUavklart: RefusjonEøsUavklart?,
        override val klage: Boolean,
        val hjemmeltekst: Hjemmeltekst,
    ) : OpphørMedEndringDelmaler
}

interface OpphørMedEndringDelmaler {
    val signaturVedtak: SignaturVedtak
    val feilutbetaling: Boolean
    val etterbetaling: Etterbetaling?
    val korrigertVedtak: KorrigertVedtakData?
    val refusjonEosAvklart: RefusjonEøsAvklart?
    val refusjonEosUavklart: RefusjonEøsUavklart?
    val klage: Boolean
}
