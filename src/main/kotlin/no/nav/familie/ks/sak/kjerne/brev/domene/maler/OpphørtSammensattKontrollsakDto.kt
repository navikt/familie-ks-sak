package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.OpphørtDelmaler

data class OpphørtSammensattKontrollsakDto(
    override val mal: Brevmal,
    override val data: OpphørtSammensattKontrollsakDataDto,
) : BrevDto {
    constructor(
        mal: Brevmal = Brevmal.VEDTAK_OPPHØRT,
        vedtakFellesfelterSammensattKontrollsakDto: VedtakFellesfelterSammensattKontrollsakDto,
        erFeilutbetalingPåBehandling: Boolean,
    ) :
        this(
            mal = mal,
            data =
                OpphørtSammensattKontrollsakDataDto(
                    delmalData =
                        OpphørtSammensattKontrollsakDataDto.Delmaler(
                            signaturVedtak =
                                SignaturVedtak(
                                    enhet = vedtakFellesfelterSammensattKontrollsakDto.enhet,
                                    saksbehandler = vedtakFellesfelterSammensattKontrollsakDto.saksbehandler,
                                    beslutter = vedtakFellesfelterSammensattKontrollsakDto.beslutter,
                                ),
                            feilutbetaling = erFeilutbetalingPåBehandling,
                            korrigertVedtak = vedtakFellesfelterSammensattKontrollsakDto.korrigertVedtakData,
                        ),
                    flettefelter =
                        FlettefelterForDokumentDtoImpl(
                            navn = vedtakFellesfelterSammensattKontrollsakDto.søkerNavn,
                            fodselsnummer = vedtakFellesfelterSammensattKontrollsakDto.søkerFødselsnummer,
                        ),
                    sammensattKontrollsakFritekst = vedtakFellesfelterSammensattKontrollsakDto.sammensattKontrollsakFritekst,
                ),
        )
}

data class OpphørtSammensattKontrollsakDataDto(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentDtoImpl,
    override val sammensattKontrollsakFritekst: String,
) : VedtaksbrevSammensattKontrollsakDto {
    data class Delmaler(
        override val signaturVedtak: SignaturVedtak,
        override val feilutbetaling: Boolean,
        override val korrigertVedtak: KorrigertVedtakData?,
    ) : OpphørtDelmaler
}
