package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

data class UtbetalingEtterKAVedtakBrevDto(
    override val mal: Brevmal = Brevmal.UTBETALING_ETTER_KA_VEDTAK,
    override val data: UtbetalingEtterKAVedtakDataDto,
) : BrevDto

data class UtbetalingEtterKAVedtakDataDto(
    override val delmalData: DelmalData,
    override val flettefelter: FlettefelterDto,
) : BrevDataDto {
    data class FlettefelterDto(
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
        val signatur: SignaturDelmal,
        val fritekstAvsnitt: FritekstAvsnitt?,
    )
}