package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

data class VarselOmRevurderingSamboerBrevDto(
    override val mal: Brevmal = Brevmal.VARSEL_OM_REVURDERING_SAMBOER,
    override val data: VarselOmRevurderingSamboerDataDto
) : BrevDto

data class VarselOmRevurderingSamboerDataDto(
    override val delmalData: DelmalData,
    override val flettefelter: FlettefelterDto
) : BrevDataDto {
    data class FlettefelterDto(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val datoAvtale: Flettefelt
    ) : FlettefelterForDokumentDto {

        constructor(
            navn: String,
            fodselsnummer: String,
            datoAvtale: String
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            datoAvtale = flettefelt(datoAvtale)
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal
    )
}
