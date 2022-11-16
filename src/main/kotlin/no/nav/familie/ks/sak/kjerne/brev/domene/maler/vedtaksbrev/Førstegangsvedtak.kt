package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.kjerne.brev.domene.FellesdataForVedtaksbrev
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevData
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto

data class Førstegangsvedtak(
    override val mal: Brevmal,
    override val data: FørstegangsvedtakData
) : VedtaksbrevDto {

    constructor(
        fellesdataForVedtaksbrev: FellesdataForVedtaksbrev,
        etterbetaling: Etterbetaling? = null
    ) :
        this(
            mal = Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
            data = FørstegangsvedtakData(
                delmalData = FørstegangsvedtakData.Delmaler(
                    signaturVedtak = SignaturVedtak(
                        enhet = fellesdataForVedtaksbrev.enhet,
                        saksbehandler = fellesdataForVedtaksbrev.saksbehandler,
                        beslutter = fellesdataForVedtaksbrev.beslutter
                    ),
                    etterbetaling = etterbetaling,
                    hjemmeltekst = fellesdataForVedtaksbrev.hjemmeltekst
                ),
                flettefelter = FlettefelterForDokumentDtoImpl(
                    navn = fellesdataForVedtaksbrev.søkerNavn,
                    fodselsnummer = fellesdataForVedtaksbrev.søkerFødselsnummer
                ),
                perioder = fellesdataForVedtaksbrev.perioder
            )
        )
}

data class FørstegangsvedtakData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentDtoImpl,
    override val perioder: List<BrevPeriodeDto>
) : VedtaksbrevData {

    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val etterbetaling: Etterbetaling?,
        val hjemmeltekst: Hjemmeltekst
    )
}
