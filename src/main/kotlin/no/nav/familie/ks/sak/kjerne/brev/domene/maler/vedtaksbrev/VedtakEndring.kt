package no.nav.familie.ks.sak.kjerne.brev.domene.maler.vedtaksbrev

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.kjerne.brev.domene.FellesdataForVedtaksbrev
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevData
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Etterbetaling
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FeilutbetaltValuta
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Flettefelt
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FlettefelterForDokumentDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.KorrigertVedtakData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.RefusjonEøsAvklart
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.RefusjonEøsUavklart
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SignaturVedtak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.flettefelt
import java.time.LocalDate

data class VedtakEndring(
    override val mal: Brevmal,
    override val data: EndringVedtakData,
) : VedtaksbrevDto {
    constructor(
        mal: Brevmal = Brevmal.VEDTAK_ENDRING,
        fellesdataForVedtaksbrev: FellesdataForVedtaksbrev,
        etterbetaling: Etterbetaling? = null,
        erFeilutbetalingPåBehandling: Boolean,
        erKlage: Boolean,
        informasjonOmAarligKontroll: Boolean,
        feilutbetaltValuta: FeilutbetaltValuta? = null,
        refusjonEosAvklart: RefusjonEøsAvklart? = null,
        refusjonEosUavklart: RefusjonEøsUavklart? = null,
        duMaaMeldeFraOmEndringerEosSelvstendigRett: Boolean = false,
        duMaaMeldeFraOmEndringer: Boolean = false,
        duMaaGiNavBeskjedHvisBarnetDittFaarTildeltBarnehageplass: Boolean = false,
    ) :
        this(
            mal = mal,
            data =
                EndringVedtakData(
                    delmalData =
                        EndringVedtakData.Delmaler(
                            signaturVedtak =
                                SignaturVedtak(
                                    enhet = fellesdataForVedtaksbrev.enhet,
                                    saksbehandler = fellesdataForVedtaksbrev.saksbehandler,
                                    beslutter = fellesdataForVedtaksbrev.beslutter,
                                ),
                            etterbetaling = etterbetaling,
                            hjemmeltekst = fellesdataForVedtaksbrev.hjemmeltekst,
                            klage = erKlage,
                            feilutbetaling = erFeilutbetalingPåBehandling,
                            korrigertVedtak = fellesdataForVedtaksbrev.korrigertVedtakData,
                            informasjonOmAarligKontroll = informasjonOmAarligKontroll,
                            forMyeUtbetaltKontantstotte = feilutbetaltValuta,
                            refusjonEosAvklart = refusjonEosAvklart,
                            refusjonEosUavklart = refusjonEosUavklart,
                            duMaaMeldeFraOmEndringerEosSelvstendigRett = duMaaMeldeFraOmEndringerEosSelvstendigRett,
                            duMaaMeldeFraOmEndringer = duMaaMeldeFraOmEndringer,
                            duMaaGiNavBeskjedHvisBarnetDittFaarTildeltBarnehageplass = duMaaGiNavBeskjedHvisBarnetDittFaarTildeltBarnehageplass,
                        ),
                    flettefelter =
                        object : FlettefelterForDokumentDto {
                            val perioderMedForMyeUtbetalt: Flettefelt = feilutbetaltValuta?.perioderMedForMyeUtbetalt
                            override val navn = flettefelt(fellesdataForVedtaksbrev.søkerNavn)
                            override val fodselsnummer = flettefelt(fellesdataForVedtaksbrev.søkerFødselsnummer)
                            override val brevOpprettetDato = flettefelt(LocalDate.now().tilDagMånedÅr())
                        },
                    perioder = fellesdataForVedtaksbrev.perioder,
                ),
        )
}

data class EndringVedtakData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentDto,
    override val perioder: List<BrevPeriodeDto>,
) : VedtaksbrevData {
    data class Delmaler(
        val signaturVedtak: SignaturVedtak,
        val etterbetaling: Etterbetaling?,
        val feilutbetaling: Boolean,
        val hjemmeltekst: Hjemmeltekst,
        val klage: Boolean,
        val korrigertVedtak: KorrigertVedtakData?,
        val informasjonOmAarligKontroll: Boolean,
        val forMyeUtbetaltKontantstotte: FeilutbetaltValuta?,
        val refusjonEosAvklart: RefusjonEøsAvklart?,
        val refusjonEosUavklart: RefusjonEøsUavklart?,
        val duMaaMeldeFraOmEndringerEosSelvstendigRett: Boolean = false,
        val duMaaMeldeFraOmEndringer: Boolean,
        val duMaaGiNavBeskjedHvisBarnetDittFaarTildeltBarnehageplass: Boolean,
    )
}
