package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev.EndringVedtakDelmaler

data class VedtakEndringSammensattKontrollsakDto(
    override val mal: Brevmal,
    override val data: EndringVedtakSammensattKontrollsakDataDto,
) : BrevDto {
    constructor(
        mal: Brevmal = Brevmal.VEDTAK_ENDRING,
        vedtakFellesfelter: VedtakFellesfelterSammensattKontrollsakDto,
        etterbetaling: Etterbetaling? = null,
        erFeilutbetalingPåBehandling: Boolean,
        erKlage: Boolean,
        informasjonOmAarligKontroll: Boolean,
        feilutbetaltValuta: FeilutbetaltValuta? = null,
        refusjonEosAvklart: RefusjonEøsAvklart? = null,
        refusjonEosUavklart: RefusjonEøsUavklart? = null,
        duMåMeldeFraOmEndringer: Boolean = true,
        duMåMeldeFraOmEndringerEøsSelvstendigRett: Boolean = false,
        informasjonOmUtbetaling: Boolean = false,
    ) :
        this(
            mal = mal,
            data =
                EndringVedtakSammensattKontrollsakDataDto(
                    delmalData =
                        EndringVedtakSammensattKontrollsakDataDto.Delmaler(
                            signaturVedtak =
                                SignaturVedtak(
                                    enhet = vedtakFellesfelter.enhet,
                                    saksbehandler = vedtakFellesfelter.saksbehandler,
                                    beslutter = vedtakFellesfelter.beslutter,
                                ),
                            etterbetaling = etterbetaling,
                            klage = erKlage,
                            feilutbetaling = erFeilutbetalingPåBehandling,
                            korrigertVedtak = vedtakFellesfelter.korrigertVedtakData,
                            informasjonOmAarligKontroll = informasjonOmAarligKontroll,
                            forMyeUtbetaltBarnetrygd = feilutbetaltValuta,
                            refusjonEosAvklart = refusjonEosAvklart,
                            refusjonEosUavklart = refusjonEosUavklart,
                            duMaaMeldeFraOmEndringerEosSelvstendigRett = duMåMeldeFraOmEndringerEøsSelvstendigRett,
                            duMaaMeldeFraOmEndringer = duMåMeldeFraOmEndringer,
                            informasjonOmUtbetaling = informasjonOmUtbetaling,
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

data class EndringVedtakSammensattKontrollsakDataDto(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentDto,
    override val sammensattKontrollsakFritekst: String,
) : VedtaksbrevSammensattKontrollsakDto {
    data class Delmaler(
        override val signaturVedtak: SignaturVedtak,
        override val etterbetaling: Etterbetaling?,
        override val feilutbetaling: Boolean,
        override val klage: Boolean,
        override val korrigertVedtak: KorrigertVedtakData?,
        override val informasjonOmAarligKontroll: Boolean,
        override val forMyeUtbetaltBarnetrygd: FeilutbetaltValuta?,
        override val refusjonEosAvklart: RefusjonEøsAvklart?,
        override val refusjonEosUavklart: RefusjonEøsUavklart?,
        override val duMaaMeldeFraOmEndringerEosSelvstendigRett: Boolean,
        override val duMaaMeldeFraOmEndringer: Boolean,
        override val informasjonOmUtbetaling: Boolean,
    ) : EndringVedtakDelmaler
}
