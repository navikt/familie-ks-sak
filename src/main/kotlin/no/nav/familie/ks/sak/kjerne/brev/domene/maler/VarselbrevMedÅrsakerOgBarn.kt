package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

data class VarselbrevMedÅrsakerOgBarn(
    override val mal: Brevmal,
    override val data: VarselbrevMedÅrsakerOgBarnData
) : Brev {
    constructor(
        mal: Brevmal,
        navn: String,
        fødselsnummer: String,
        enhet: String,
        varselÅrsaker: List<String>,
        barnasFødselsdager: String
    ) : this(
        mal = mal,
        data = VarselbrevMedÅrsakerOgBarnData(
            delmalData = VarselbrevMedÅrsakerOgBarnData.DelmalData(signatur = SignaturDelmal(enhet = enhet)),
            flettefelter = VarselbrevMedÅrsakerOgBarnData.Flettefelter(
                navn = navn,
                fodselsnummer = fødselsnummer,
                varselÅrsaker = varselÅrsaker,
                barnasFødselsdager = barnasFødselsdager
            )
        )
    )
}

data class VarselbrevMedÅrsakerOgBarnData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter
) : BrevData {

    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val varselAarsaker: Flettefelt,
        val barnasFodselsdatoer: Flettefelt
    ) : FlettefelterForDokument {

        constructor(
            navn: String,
            fodselsnummer: String,
            varselÅrsaker: List<String>,
            barnasFødselsdager: String
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            varselAarsaker = flettefelt(varselÅrsaker),
            barnasFodselsdatoer = flettefelt(barnasFødselsdager)
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal
    )
}
