package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.OpphørMedEndringDelmaler

data class OpphørMedEndringSammensattKontrollsakDto(
    override val mal: Brevmal,
    override val data: OpphørMedEndringSammensattKontrollsakDataDto,
) : BrevDto {
    constructor(
        mal: Brevmal = Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
        vedtakFellesfelter: VedtakFellesfelterSammensattKontrollsakDto,
        etterbetaling: Etterbetaling? = null,
        erFeilutbetalingPåBehandling: Boolean,
        refusjonEosAvklart: RefusjonEøsAvklart? = null,
        refusjonEosUavklart: RefusjonEøsUavklart? = null,
        erKlage: Boolean,
    ) :
        this(
            mal = mal,
            data =
                OpphørMedEndringSammensattKontrollsakDataDto(
                    delmalData =
                        OpphørMedEndringSammensattKontrollsakDataDto.Delmaler(
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

data class OpphørMedEndringSammensattKontrollsakDataDto(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentDto,
    override val sammensattKontrollsakFritekst: String,
) : VedtaksbrevSammensattKontrollsakDto {
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
