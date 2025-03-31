package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Flettefelt
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FlettefelterForDokumentDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FritekstAvsnitt
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SignaturDelmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.flettefelt
import java.time.LocalDate

data class InformasjonsbrevInnhenteOpplysningerKlageDto(
    override val mal: Brevmal = Brevmal.INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE,
    override val data: InformasjonsbrevInnhenteOpplysningerKlageDataDto,
) : BrevDto {
    constructor(
        navn: String,
        fodselsnummer: String,
        fritekstAvsnitt: String,
        enhet: String,
        saksbehandlerNavn: String,
    ) : this(
        data =
            InformasjonsbrevInnhenteOpplysningerKlageDataDto(
                flettefelter =
                    InformasjonsbrevInnhenteOpplysningerKlageDataDto.FlettefelterDto(
                        navn = navn,
                        fodselsnummer = fodselsnummer,
                    ),
                delmalData =
                    InformasjonsbrevInnhenteOpplysningerKlageDataDto.DelmalData(
                        SignaturDelmal(
                            enhet = enhet,
                            saksbehandlerNavn = saksbehandlerNavn,
                        ),
                        fritekstAvsnitt = FritekstAvsnitt(fritekstAvsnitt),
                    ),
            ),
    )
}

data class InformasjonsbrevInnhenteOpplysningerKlageDataDto(
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
        val fritekstAvsnitt: FritekstAvsnitt,
    )
}
