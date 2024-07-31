package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

data class EndringAvFramtidigOpphør(
    override val mal: Brevmal = Brevmal.ENDRING_AV_FRAMTIDIG_OPPHØR,
    override val data: EndringAvFramtidigOpphørData,
) : BrevDto

data class EndringAvFramtidigOpphørData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
) : BrevDataDto {
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
