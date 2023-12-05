package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

data class InformasjonsbrevTilForelderBrev(
    override val mal: Brevmal,
    override val data: InformasjonsbrevTilForelderDataDto,
) : BrevDto

data class InformasjonsbrevTilForelderDataDto(
    override val delmalData: DelmalData,
    override val flettefelter: FlettefelterForDokumentDto,
) : BrevDataDto {
    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val barnSoktFor: Flettefelt,
    ) : FlettefelterForDokumentDto {
        constructor(
            navn: String,
            fodselsnummer: String,
            barnSøktFor: List<String>,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            barnSoktFor = flettefelt(barnSøktFor),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
