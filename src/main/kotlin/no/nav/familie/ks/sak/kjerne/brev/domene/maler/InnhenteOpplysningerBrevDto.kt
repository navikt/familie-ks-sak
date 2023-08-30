package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

data class InnhenteOpplysningerBrevDto(
    override val mal: Brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
    override val data: InnhenteOpplysningerDataDto,
) : BrevDto

data class InnhenteOpplysningerDataDto(
    override val delmalData: DelmalData,
    override val flettefelter: FlettefelterDto,
) : BrevDataDto {

    data class FlettefelterDto(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val dokumentliste: Flettefelt,
    ) : FlettefelterForDokumentDto {

        constructor(
            navn: String,
            fodselsnummer: String,
            dokumentliste: List<String>,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            dokumentliste = flettefelt(dokumentliste),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
