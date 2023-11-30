package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

data class VarselbrevMedÅrsakerOgBarnDto(
    override val mal: Brevmal,
    override val data: VarselbrevMedÅrsakerOgBarnDataDto,
) : BrevDto {
    constructor(
        mal: Brevmal,
        navn: String,
        fødselsnummer: String,
        enhet: String,
        varselÅrsaker: List<String>,
        barnasFødselsdager: String,
        saksbehandlerNavn: String,
    ) : this(
        mal = mal,
        data =
            VarselbrevMedÅrsakerOgBarnDataDto(
                delmalData = VarselbrevMedÅrsakerOgBarnDataDto.DelmalData(signatur = SignaturDelmal(enhet = enhet, saksbehandlerNavn = saksbehandlerNavn)),
                flettefelter =
                    VarselbrevMedÅrsakerOgBarnDataDto.FlettefelterDto(
                        navn = navn,
                        fodselsnummer = fødselsnummer,
                        varselÅrsaker = varselÅrsaker,
                        barnasFødselsdager = barnasFødselsdager,
                    ),
            ),
    )
}

data class VarselbrevMedÅrsakerOgBarnDataDto(
    override val delmalData: DelmalData,
    override val flettefelter: FlettefelterDto,
) : BrevDataDto {
    data class FlettefelterDto(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val varselAarsaker: Flettefelt,
        val barnasFodselsdatoer: Flettefelt,
    ) : FlettefelterForDokumentDto {
        constructor(
            navn: String,
            fodselsnummer: String,
            varselÅrsaker: List<String>,
            barnasFødselsdager: String,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            varselAarsaker = flettefelt(varselÅrsaker),
            barnasFodselsdatoer = flettefelt(barnasFødselsdager),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
