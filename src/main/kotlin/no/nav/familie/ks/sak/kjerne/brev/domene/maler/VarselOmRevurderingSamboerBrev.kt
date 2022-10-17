package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

data class VarselOmRevurderingSamboerBrev(
    override val mal: Brevmal = Brevmal.VARSEL_OM_REVURDERING_SAMBOER,
    override val data: VarselOmRevurderingSamboerData
) : Brev

data class VarselOmRevurderingSamboerData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter
) : BrevData {
    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val datoAvtale: Flettefelt
    ) : FlettefelterForDokument {

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
