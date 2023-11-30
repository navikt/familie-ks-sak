package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

data class InnhenteOpplysningerOmBarnDto(
    override val mal: Brevmal,
    override val data: InnhenteOpplysningerOmBarnDataDto,
) : BrevDto {
    constructor(
        mal: Brevmal,
        navn: String,
        fødselsnummer: String,
        barnasFødselsdager: String,
        enhet: String,
        dokumentliste: List<String>,
        saksbehandlerNavn: String,
    ) : this(
        mal = mal,
        data =
            InnhenteOpplysningerOmBarnDataDto(
                delmalData = InnhenteOpplysningerOmBarnDataDto.DelmalData(signatur = SignaturDelmal(enhet = enhet, saksbehandlerNavn = saksbehandlerNavn)),
                flettefelter =
                    InnhenteOpplysningerOmBarnDataDto.FlettefelterDto(
                        navn = navn,
                        fodselsnummer = fødselsnummer,
                        barnasFødselsdager = barnasFødselsdager,
                        dokumentliste = dokumentliste,
                    ),
            ),
    )
}

data class InnhenteOpplysningerOmBarnDataDto(
    override val delmalData: DelmalData,
    override val flettefelter: FlettefelterDto,
) : BrevDataDto {
    data class FlettefelterDto(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val barnasFodselsdatoer: Flettefelt,
        val dokumentliste: Flettefelt,
    ) : FlettefelterForDokumentDto {
        constructor(
            navn: String,
            fodselsnummer: String,
            barnasFødselsdager: String,
            dokumentliste: List<String>,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            barnasFodselsdatoer = flettefelt(barnasFødselsdager),
            dokumentliste = flettefelt(dokumentliste),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
