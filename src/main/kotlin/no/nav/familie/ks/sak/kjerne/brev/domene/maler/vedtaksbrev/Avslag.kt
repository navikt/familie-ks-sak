package no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev

import no.nav.familie.ks.sak.kjerne.brev.domene.FellesdataForVedtaksbrev
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevData
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FlettefelterForDokumentDtoImpl
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.KorrigertVedtakData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SignaturVedtak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto

data class Avslag(
    override val mal: Brevmal,
    override val data: AvslagData
) : VedtaksbrevDto {

    constructor(
        mal: Brevmal = Brevmal.VEDTAK_AVSLAG,
        fellesdataForVedtaksbrev: FellesdataForVedtaksbrev
    ) :
        this(
            mal = mal,
            data = AvslagData(
                delmalData = AvslagData.Delmaler(
                    signaturVedtak = SignaturVedtak(
                        enhet = fellesdataForVedtaksbrev.enhet,
                        saksbehandler = fellesdataForVedtaksbrev.saksbehandler,
                        beslutter = fellesdataForVedtaksbrev.beslutter
                    ),
                    hjemmeltekst = fellesdataForVedtaksbrev.hjemmeltekst,
                    korrigertVedtak = fellesdataForVedtaksbrev.korrigertVedtakData
                ),
                flettefelter = FlettefelterForDokumentDtoImpl(
                    navn = fellesdataForVedtaksbrev.søkerNavn,
                    fodselsnummer = fellesdataForVedtaksbrev.søkerFødselsnummer
                ),
                perioder = fellesdataForVedtaksbrev.perioder
            )
        )
}

data class AvslagData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentDtoImpl,
    override val perioder: List<BrevPeriodeDto>
) : VedtaksbrevData {

    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val hjemmeltekst: Hjemmeltekst,
        val korrigertVedtak: KorrigertVedtakData?
    )
}
