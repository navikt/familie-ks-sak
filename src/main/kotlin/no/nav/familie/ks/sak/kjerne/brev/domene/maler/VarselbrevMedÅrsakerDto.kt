package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

data class VarselbrevMedÅrsakerDto(
    override val mal: Brevmal,
    override val data: VarselOmRevurderingDataDto,
) : BrevDto {
    constructor(
        mal: Brevmal,
        navn: String,
        fødselsnummer: String,
        varselÅrsaker: List<String>,
        enhet: String,
        saksbehandlerNavn: String,
    ) : this(
        mal = mal,
        data =
            VarselOmRevurderingDataDto(
                delmalData =
                    VarselOmRevurderingDataDto.DelmalData(
                        signatur =
                            SignaturDelmal(
                                enhet = enhet,
                                saksbehandlerNavn = saksbehandlerNavn,
                            ),
                    ),
                flettefelter =
                    VarselOmRevurderingDataDto.FlettefelterDto(
                        navn = navn,
                        fodselsnummer = fødselsnummer,
                        varselÅrsaker = varselÅrsaker,
                    ),
            ),
    )
}

data class VarselOmRevurderingDataDto(
    override val delmalData: DelmalData,
    override val flettefelter: FlettefelterDto,
) : BrevDataDto {
    data class FlettefelterDto(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val varselAarsaker: Flettefelt,
    ) : FlettefelterForDokumentDto {
        constructor(
            navn: String,
            fodselsnummer: String,
            varselÅrsaker: List<String>,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            varselAarsaker = flettefelt(varselÅrsaker),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
