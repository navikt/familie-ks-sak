package no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev

import no.nav.familie.ks.sak.kjerne.brev.domene.FellesdataForVedtaksbrev
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevData
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Etterbetaling
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FlettefelterForDokumentDtoImpl
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.KorrigertVedtakData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SignaturVedtak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto

data class VedtakEndring(
    override val mal: Brevmal,
    override val data: EndringVedtakData
) : VedtaksbrevDto {

    constructor(
        mal: Brevmal = Brevmal.VEDTAK_ENDRING,
        fellesdataForVedtaksbrev: FellesdataForVedtaksbrev,
        etterbetaling: Etterbetaling? = null,
        erFeilutbetalingPåBehandling: Boolean,
        erKlage: Boolean,
        informasjonOmAarligKontroll: Boolean
    ) :
        this(
            mal = mal,
            data = EndringVedtakData(
                delmalData = EndringVedtakData.Delmaler(
                    signaturVedtak = SignaturVedtak(
                        enhet = fellesdataForVedtaksbrev.enhet,
                        saksbehandler = fellesdataForVedtaksbrev.saksbehandler,
                        beslutter = fellesdataForVedtaksbrev.beslutter
                    ),
                    etterbetaling = etterbetaling,
                    hjemmeltekst = fellesdataForVedtaksbrev.hjemmeltekst,
                    klage = erKlage,
                    feilutbetaling = erFeilutbetalingPåBehandling,
                    korrigertVedtak = fellesdataForVedtaksbrev.korrigertVedtakData,
                    informasjonOmAarligKontroll = informasjonOmAarligKontroll
                ),
                flettefelter = FlettefelterForDokumentDtoImpl(
                    navn = fellesdataForVedtaksbrev.søkerNavn,
                    fodselsnummer = fellesdataForVedtaksbrev.søkerFødselsnummer
                ),
                perioder = fellesdataForVedtaksbrev.perioder
            )
        )
}

data class EndringVedtakData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentDtoImpl,
    override val perioder: List<BrevPeriodeDto>
) : VedtaksbrevData {

    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val etterbetaling: Etterbetaling?,
        val feilutbetaling: Boolean,
        val hjemmeltekst: Hjemmeltekst,
        val klage: Boolean,
        val korrigertVedtak: KorrigertVedtakData?,
        val informasjonOmAarligKontroll: Boolean
    )
}
