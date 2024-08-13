package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.OpphørMedEndringDelmaler

data class OpphørMedEndringSammensattKontrollsak(
    override val mal: Brevmal,
    override val data: OpphørMedEndringSammensattKontrollsakData,
) : BrevDto {
    constructor(
        mal: Brevmal = Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
        vedtakFellesfelter: VedtakFellesfelterSammensattKontrollsak,
        etterbetaling: Etterbetaling? = null,
        erFeilutbetalingPåBehandling: Boolean,
        refusjonEosAvklart: RefusjonEøsAvklart? = null,
        refusjonEosUavklart: RefusjonEøsUavklart? = null,
        erKlage: Boolean,
    ) :
        this(
            mal = mal,
            data =
                OpphørMedEndringSammensattKontrollsakData(
                    delmalData =
                        OpphørMedEndringSammensattKontrollsakData.Delmaler(
                            signaturVedtak =
                                SignaturVedtak(
                                    enhet = vedtakFellesfelter.enhet,
                                    saksbehandler = vedtakFellesfelter.saksbehandler,
                                    beslutter = vedtakFellesfelter.beslutter,
                                ),
                            feilutbetaling = erFeilutbetalingPåBehandling,
                            etterbetaling = etterbetaling,
                            korrigertVedtak = vedtakFellesfelter.korrigertVedtakData,
                            refusjonEosAvklart = refusjonEosAvklart,
                            refusjonEosUavklart = refusjonEosUavklart,
                            klage = erKlage,
                        ),
                    flettefelter =
                        FlettefelterForDokumentDtoImpl(
                            navn = vedtakFellesfelter.søkerNavn,
                            fodselsnummer = vedtakFellesfelter.søkerFødselsnummer,
                        ),
                    sammensattKontrollsakFritekst = vedtakFellesfelter.sammensattKontrollsakFritekst,
                ),
        )
}

data class OpphørMedEndringSammensattKontrollsakData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentDto,
    override val sammensattKontrollsakFritekst: String,
) : VedtaksbrevSammensattKontrollsak {
    data class Delmaler(
        override val signaturVedtak: SignaturVedtak,
        override val feilutbetaling: Boolean,
        override val etterbetaling: Etterbetaling?,
        override val korrigertVedtak: KorrigertVedtakData?,
        override val refusjonEosAvklart: RefusjonEøsAvklart?,
        override val refusjonEosUavklart: RefusjonEøsUavklart?,
        override val klage: Boolean,
    ) : OpphørMedEndringDelmaler
}
