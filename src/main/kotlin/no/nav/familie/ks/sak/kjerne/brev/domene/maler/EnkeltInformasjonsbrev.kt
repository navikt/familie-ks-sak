package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

data class EnkeltInformasjonsbrev(
    override val mal: Brevmal,
    override val data: EnkeltInformasjonsbrevData
) : Brev {

    constructor(
        navn: String,
        fodselsnummer: String,
        enhet: String,
        mal: Brevmal
    ) : this(
        mal = mal,
        data = EnkeltInformasjonsbrevData(
            flettefelter = EnkeltInformasjonsbrevData.Flettefelter(
                navn = navn,
                fodselsnummer = fodselsnummer
            ),
            delmalData = EnkeltInformasjonsbrevData.DelmalData(
                SignaturDelmal(
                    enhet = enhet
                )
            )
        )
    )
}

data class EnkeltInformasjonsbrevData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter
) : BrevData {

    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr())
    ) : FlettefelterForDokument {

        constructor(
            navn: String,
            fodselsnummer: String
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer)
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal
    )
}
