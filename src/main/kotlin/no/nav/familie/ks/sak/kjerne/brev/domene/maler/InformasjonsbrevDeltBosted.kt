package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

data class InformasjonsbrevDeltBostedBrevDto(
    override val mal: Brevmal = Brevmal.INFORMASJONSBREV_DELT_BOSTED,
    override val data: InformasjonsbrevDeltBostedDataDto,
) : BrevDto

data class InformasjonsbrevDeltBostedDataDto(
    override val delmalData: DelmalData,
    override val flettefelter: FlettefelterDto,
) : BrevDataDto {
    data class FlettefelterDto(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val barnMedDeltBostedAvtale: Flettefelt,
    ) : FlettefelterForDokumentDto {
        constructor(
            navn: String,
            fodselsnummer: String,
            barnMedDeltBostedAvtale: List<String>,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            barnMedDeltBostedAvtale = flettefelt(barnMedDeltBostedAvtale),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
