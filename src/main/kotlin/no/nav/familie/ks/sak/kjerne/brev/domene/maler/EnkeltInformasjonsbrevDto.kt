package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

data class EnkeltInformasjonsbrevDto(
    override val mal: Brevmal,
    override val data: EnkeltInformasjonsbrevDataDto,
) : BrevDto {
    constructor(
        navn: String,
        fodselsnummer: String,
        enhet: String,
        mal: Brevmal,
    ) : this(
        mal = mal,
        data =
            EnkeltInformasjonsbrevDataDto(
                flettefelter =
                    EnkeltInformasjonsbrevDataDto.FlettefelterDto(
                        navn = navn,
                        fodselsnummer = fodselsnummer,
                    ),
                delmalData =
                    EnkeltInformasjonsbrevDataDto.DelmalData(
                        SignaturDelmal(
                            enhet = enhet,
                        ),
                    ),
            ),
    )
}

data class EnkeltInformasjonsbrevDataDto(
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
    )
}
