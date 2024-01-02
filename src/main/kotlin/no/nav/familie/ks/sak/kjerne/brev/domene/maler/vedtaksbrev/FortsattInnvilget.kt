package no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev

import no.nav.familie.ks.sak.kjerne.brev.domene.FellesdataForVedtaksbrev
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevData
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Etterbetaling
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FlettefelterForDokumentDtoImpl
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.KorrigertVedtakData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.RefusjonEøsAvklart
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.RefusjonEøsUavklart
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SignaturVedtak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.flettefelt

data class FortsattInnvilget(
    override val mal: Brevmal,
    override val data: ForsattInnvilgetData,
) : VedtaksbrevDto {
    constructor(
        mal: Brevmal = Brevmal.VEDTAK_FORTSATT_INNVILGET,
        fellesdataForVedtaksbrev: FellesdataForVedtaksbrev,
        etterbetaling: Etterbetaling? = null,
        informasjonOmAarligKontroll: Boolean = false,
        refusjonEosAvklart: RefusjonEøsAvklart? = null,
        refusjonEosUavklart: RefusjonEøsUavklart? = null,
        duMåMeldeFraOmEndringer: Boolean = true,
        duMåMeldeFraOmEndringerEøsSelvstendigRett: Boolean = false,
    ) :
        this(
            mal = mal,
            data =
                ForsattInnvilgetData(
                    delmalData =
                        ForsattInnvilgetData.Delmaler(
                            signaturVedtak =
                                SignaturVedtak(
                                    enhet = fellesdataForVedtaksbrev.enhet,
                                    saksbehandler = fellesdataForVedtaksbrev.saksbehandler,
                                    beslutter = fellesdataForVedtaksbrev.beslutter,
                                ),
                            hjemmeltekst = fellesdataForVedtaksbrev.hjemmeltekst,
                            etterbetaling = etterbetaling,
                            korrigertVedtak = fellesdataForVedtaksbrev.korrigertVedtakData,
                            informasjonOmAarligKontroll = informasjonOmAarligKontroll,
                            refusjonEosAvklart = refusjonEosAvklart,
                            refusjonEosUavklart = refusjonEosUavklart,
                            duMaaMeldeFraOmEndringer = duMåMeldeFraOmEndringer,
                            duMaaMeldeFraOmEndringerEosSelvstendigRett = duMåMeldeFraOmEndringerEøsSelvstendigRett,
                        ),
                    flettefelter =
                        FlettefelterForDokumentDtoImpl(
                            gjelder = flettefelt(fellesdataForVedtaksbrev.gjelder),
                            navn = flettefelt(fellesdataForVedtaksbrev.søkerNavn),
                            fodselsnummer = flettefelt(fellesdataForVedtaksbrev.søkerFødselsnummer),
                        ),
                    perioder = fellesdataForVedtaksbrev.perioder,
                ),
        )
}

data class ForsattInnvilgetData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentDtoImpl,
    override val perioder: List<BrevPeriodeDto>,
) : VedtaksbrevData {
    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val hjemmeltekst: Hjemmeltekst,
        val etterbetaling: Etterbetaling?,
        val korrigertVedtak: KorrigertVedtakData?,
        val informasjonOmAarligKontroll: Boolean,
        val refusjonEosAvklart: RefusjonEøsAvklart?,
        val refusjonEosUavklart: RefusjonEøsUavklart?,
        val duMaaMeldeFraOmEndringerEosSelvstendigRett: Boolean,
        val duMaaMeldeFraOmEndringer: Boolean,
    )
}
