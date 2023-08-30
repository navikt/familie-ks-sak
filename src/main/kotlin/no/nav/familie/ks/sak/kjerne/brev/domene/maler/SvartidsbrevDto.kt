package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

data class SvartidsbrevDto(
    override val mal: Brevmal,
    override val data: SvartidsbrevDataDto,
) : BrevDto {
    constructor(
        navn: String,
        fodselsnummer: String,
        enhet: String,
        mal: Brevmal,
        erEøsBehandling: Boolean,
    ) : this(
        mal = mal,
        data = SvartidsbrevDataDto(
            flettefelter = SvartidsbrevDataDto.FlettefelterDto(
                navn = navn,
                fodselsnummer = fodselsnummer,
            ),
            delmalData = SvartidsbrevDataDto.DelmalData(
                signatur = SignaturDelmal(
                    enhet = enhet,
                ),
                kontonummer = erEøsBehandling,

            ),
        ),
    )
}

data class SvartidsbrevDataDto(
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
        val kontonummer: Boolean,
    )
}
