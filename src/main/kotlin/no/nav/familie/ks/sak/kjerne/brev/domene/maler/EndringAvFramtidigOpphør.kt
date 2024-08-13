package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevData
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto

data class EndringAvFramtidigOpphør(
    override val mal: Brevmal = Brevmal.ENDRING_AV_FRAMTIDIG_OPPHØR,
    override val data: EndringAvFramtidigOpphørData,
) : VedtaksbrevDto

data class EndringAvFramtidigOpphørData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
    override val perioder: List<BrevPeriodeDto>,
) : VedtaksbrevData {
    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
    ) : FlettefelterForDokumentDto {
        constructor(
            navn: String,
            fodselsnummer: String,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
        )
    }

    data class DelmalData(
        val signaturVedtak: SignaturVedtak,
    )
}
