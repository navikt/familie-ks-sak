package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.OpphørtDelmaler

data class OpphørtSammensattKontrollsak(
    override val mal: Brevmal,
    override val data: OpphørtSammensattKontrollsakData,
) : BrevDto {
    constructor(
        mal: Brevmal = Brevmal.VEDTAK_OPPHØRT,
        vedtakFellesfelterSammensattKontrollsak: VedtakFellesfelterSammensattKontrollsak,
        erFeilutbetalingPåBehandling: Boolean,
    ) :
        this(
            mal = mal,
            data =
                OpphørtSammensattKontrollsakData(
                    delmalData =
                        OpphørtSammensattKontrollsakData.Delmaler(
                            signaturVedtak =
                                SignaturVedtak(
                                    enhet = vedtakFellesfelterSammensattKontrollsak.enhet,
                                    saksbehandler = vedtakFellesfelterSammensattKontrollsak.saksbehandler,
                                    beslutter = vedtakFellesfelterSammensattKontrollsak.beslutter,
                                ),
                            feilutbetaling = erFeilutbetalingPåBehandling,
                            korrigertVedtak = vedtakFellesfelterSammensattKontrollsak.korrigertVedtakData,
                        ),
                    flettefelter =
                        FlettefelterForDokumentDtoImpl(
                            navn = vedtakFellesfelterSammensattKontrollsak.søkerNavn,
                            fodselsnummer = vedtakFellesfelterSammensattKontrollsak.søkerFødselsnummer,
                        ),
                    sammensattKontrollsakFritekst = vedtakFellesfelterSammensattKontrollsak.sammensattKontrollsakFritekst,
                ),
        )
}

data class OpphørtSammensattKontrollsakData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentDtoImpl,
    override val sammensattKontrollsakFritekst: String,
) : VedtaksbrevSammensattKontrollsak {
    data class Delmaler(
        override val signaturVedtak: SignaturVedtak,
        override val feilutbetaling: Boolean,
        override val korrigertVedtak: KorrigertVedtakData?,
    ) : OpphørtDelmaler
}
