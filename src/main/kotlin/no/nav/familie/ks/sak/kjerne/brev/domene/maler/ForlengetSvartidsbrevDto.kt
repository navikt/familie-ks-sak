package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

data class ForlengetSvartidsbrevDto(
    override val mal: Brevmal = Brevmal.FORLENGET_SVARTIDSBREV,
    override val data: ForlengetSvartidsbrevDataDto
) : BrevDto {
    constructor(
        navn: String,
        fodselsnummer: String,
        enhetNavn: String,
        årsaker: List<String>,
        antallUkerSvarfrist: Int
    ) : this(
        data = ForlengetSvartidsbrevDataDto(
            delmalData = ForlengetSvartidsbrevDataDto.DelmalData(signatur = SignaturDelmal(enhet = enhetNavn)),
            flettefelter = ForlengetSvartidsbrevDataDto.FlettefelterDto(
                navn = flettefelt(navn),
                fodselsnummer = flettefelt(fodselsnummer),
                antallUkerSvarfrist = flettefelt(antallUkerSvarfrist.toString()),
                aarsakerSvartidsbrev = flettefelt(årsaker)
            )
        )
    )
}

data class ForlengetSvartidsbrevDataDto(
    override val delmalData: DelmalData,
    override val flettefelter: FlettefelterDto
) : BrevDataDto {
    data class FlettefelterDto(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val antallUkerSvarfrist: Flettefelt,
        val aarsakerSvartidsbrev: Flettefelt
    ) : FlettefelterForDokumentDto

    data class DelmalData(
        val signatur: SignaturDelmal
    )
}
