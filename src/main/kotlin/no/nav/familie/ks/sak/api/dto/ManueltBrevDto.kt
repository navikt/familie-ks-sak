package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.EnkeltInformasjonsbrev
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FlettefelterForDokumentImpl
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.ForlengetSvartidsbrev
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.HenleggeTrukketSøknadBrev
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.HenleggeTrukketSøknadData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InformasjonsbrevDeltBostedBrev
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InformasjonsbrevDeltBostedData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InformasjonsbrevKanSøke
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InnhenteOpplysningerBrev
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InnhenteOpplysningerData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InnhenteOpplysningerOmBarn
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SignaturDelmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Svartidsbrev
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.VarselOmRevurderingDeltBostedParagraf14Brev
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.VarselOmRevurderingDeltBostedParagraf14Data
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.VarselOmRevurderingSamboerBrev
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.VarselOmRevurderingSamboerData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.VarselbrevMedÅrsaker
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.VarselbrevMedÅrsakerOgBarn
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.flettefelt
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import java.time.LocalDate

data class ManueltBrevDto(
    val brevmal: Brevmal,
    val multiselectVerdier: List<String> = emptyList(),
    val mottakerIdent: String,
    val barnIBrev: List<String> = emptyList(),
    val datoAvtale: String? = null,
    // Settes av backend ved utsending fra behandling
    val mottakerMålform: Målform = Målform.NB,
    val mottakerNavn: String = "",
    val enhet: Enhet? = null,
    val antallUkerSvarfrist: Int? = null,
    val barnasFødselsdager: List<LocalDate>? = null,
    val behandlingKategori: BehandlingKategori? = null
) {
    fun enhetNavn(): String = this.enhet?.enhetNavn ?: error("Finner ikke enhetsnavn på manuell brevrequest")
}

fun ManueltBrevDto.tilBrev() = when (this.brevmal) {
    Brevmal.INFORMASJONSBREV_DELT_BOSTED ->
        InformasjonsbrevDeltBostedBrev(
            data = InformasjonsbrevDeltBostedData(
                delmalData = InformasjonsbrevDeltBostedData.DelmalData(
                    signatur = SignaturDelmal(
                        enhet = flettefelt(
                            this.enhetNavn()
                        )
                    )
                ),
                flettefelter = InformasjonsbrevDeltBostedData.Flettefelter(
                    navn = this.mottakerNavn,
                    fodselsnummer = this.mottakerIdent,
                    barnMedDeltBostedAvtale = this.multiselectVerdier
                )
            )
        )

    Brevmal.INNHENTE_OPPLYSNINGER ->
        InnhenteOpplysningerBrev(
            data = InnhenteOpplysningerData(
                delmalData = InnhenteOpplysningerData.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn())),
                flettefelter = InnhenteOpplysningerData.Flettefelter(
                    navn = this.mottakerNavn,
                    fodselsnummer = this.mottakerIdent,
                    dokumentliste = this.multiselectVerdier
                )
            )
        )

    Brevmal.HENLEGGE_TRUKKET_SØKNAD ->
        HenleggeTrukketSøknadBrev(
            data = HenleggeTrukketSøknadData(
                delmalData = HenleggeTrukketSøknadData.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn())),
                flettefelter = FlettefelterForDokumentImpl(
                    navn = this.mottakerNavn,
                    fodselsnummer = this.mottakerIdent
                )
            )
        )

    Brevmal.VARSEL_OM_REVURDERING ->
        VarselbrevMedÅrsaker(
            mal = Brevmal.VARSEL_OM_REVURDERING,
            navn = this.mottakerNavn,
            fødselsnummer = this.mottakerIdent,
            varselÅrsaker = this.multiselectVerdier,
            enhet = this.enhetNavn()
        )

    Brevmal.VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14 ->
        VarselOmRevurderingDeltBostedParagraf14Brev(
            data = VarselOmRevurderingDeltBostedParagraf14Data(
                delmalData = VarselOmRevurderingDeltBostedParagraf14Data.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn())),
                flettefelter = VarselOmRevurderingDeltBostedParagraf14Data.Flettefelter(
                    navn = this.mottakerNavn,
                    fodselsnummer = this.mottakerIdent,
                    barnMedDeltBostedAvtale = this.multiselectVerdier
                )
            )
        )

    Brevmal.VARSEL_OM_REVURDERING_SAMBOER ->
        if (this.datoAvtale == null) {
            throw FunksjonellFeil(
                frontendFeilmelding = "Du må sette dato for samboerskap for å sende dette brevet.",
                melding = "Dato er ikke satt for brevtype 'varsel om revurdering samboer'"
            )
        } else {
            VarselOmRevurderingSamboerBrev(
                data = VarselOmRevurderingSamboerData(
                    delmalData = VarselOmRevurderingSamboerData.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn())),
                    flettefelter = VarselOmRevurderingSamboerData.Flettefelter(
                        navn = this.mottakerNavn,
                        fodselsnummer = this.mottakerIdent,
                        datoAvtale = LocalDate.parse(this.datoAvtale).tilDagMånedÅr()
                    )
                )
            )
        }

    Brevmal.SVARTIDSBREV ->
        Svartidsbrev(
            navn = this.mottakerNavn,
            fodselsnummer = this.mottakerIdent,
            enhet = this.enhetNavn(),
            mal = Brevmal.SVARTIDSBREV,
            erEøsBehandling = if (this.behandlingKategori == null) {
                throw Feil("Trenger å vite om behandling er EØS for å sende ut svartidsbrev.")
            } else {
                this.behandlingKategori == BehandlingKategori.EØS
            }
        )

    Brevmal.FORLENGET_SVARTIDSBREV ->
        ForlengetSvartidsbrev(
            navn = this.mottakerNavn,
            fodselsnummer = this.mottakerIdent,
            enhetNavn = this.enhetNavn(),
            årsaker = this.multiselectVerdier,
            antallUkerSvarfrist = this.antallUkerSvarfrist ?: throw Feil("Antall uker svarfrist er ikke satt")
        )

    Brevmal.INFORMASJONSBREV_FØDSEL_MINDREÅRIG ->
        EnkeltInformasjonsbrev(
            navn = this.mottakerNavn,
            fodselsnummer = this.mottakerIdent,
            enhet = this.enhetNavn(),
            mal = Brevmal.INFORMASJONSBREV_FØDSEL_MINDREÅRIG
        )

    Brevmal.INFORMASJONSBREV_FØDSEL_UMYNDIG,
    Brevmal.INFORMASJONSBREV_FØDSEL_VERGEMÅL ->
        EnkeltInformasjonsbrev(
            navn = this.mottakerNavn,
            fodselsnummer = this.mottakerIdent,
            enhet = this.enhetNavn(),
            mal = Brevmal.INFORMASJONSBREV_FØDSEL_VERGEMÅL
        )

    Brevmal.INFORMASJONSBREV_FØDSEL_GENERELL ->
        EnkeltInformasjonsbrev(
            navn = this.mottakerNavn,
            fodselsnummer = this.mottakerIdent,
            enhet = this.enhetNavn(),
            mal = Brevmal.INFORMASJONSBREV_FØDSEL_GENERELL
        )

    Brevmal.INFORMASJONSBREV_KAN_SØKE ->
        InformasjonsbrevKanSøke(
            navn = this.mottakerNavn,
            fodselsnummer = this.mottakerIdent,
            enhet = this.enhetNavn(),
            dokumentliste = this.multiselectVerdier
        )

    Brevmal.VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED ->
        VarselbrevMedÅrsakerOgBarn(
            mal = Brevmal.VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
            navn = this.mottakerNavn,
            fødselsnummer = this.mottakerIdent,
            enhet = this.enhetNavn(),
            varselÅrsaker = this.multiselectVerdier,
            barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager()
        )

    Brevmal.VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS ->
        VarselbrevMedÅrsaker(
            mal = Brevmal.VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
            navn = this.mottakerNavn,
            fødselsnummer = this.mottakerIdent,
            varselÅrsaker = this.multiselectVerdier,
            enhet = this.enhetNavn()
        )

    Brevmal.INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED ->
        InnhenteOpplysningerOmBarn(
            mal = Brevmal.INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
            navn = this.mottakerNavn,
            fødselsnummer = this.mottakerIdent,
            dokumentliste = this.multiselectVerdier,
            enhet = this.enhetNavn(),
            barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager()
        )

    Brevmal.INFORMASJONSBREV_KAN_SØKE_EØS ->
        EnkeltInformasjonsbrev(
            navn = this.mottakerNavn,
            fodselsnummer = this.mottakerIdent,
            enhet = this.enhetNavn(),
            mal = Brevmal.INFORMASJONSBREV_KAN_SØKE_EØS
        )

    Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
    Brevmal.VEDTAK_ENDRING,
    Brevmal.VEDTAK_OPPHØRT,
    Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
    Brevmal.VEDTAK_AVSLAG,
    Brevmal.VEDTAK_FORTSATT_INNVILGET,
    Brevmal.VEDTAK_KORREKSJON_VEDTAKSBREV,
    Brevmal.VEDTAK_OPPHØR_DØDSFALL,
    Brevmal.DØDSFALL,
    Brevmal.AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG,
    Brevmal.AUTOVEDTAK_NYFØDT_FØRSTE_BARN,
    Brevmal.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR -> throw Feil("Kan ikke mappe fra manuel brevrequest til ${this.brevmal}.")
}

private fun List<LocalDate>?.tilFormaterteFødselsdager() = slåSammen(
    this?.map { it.tilKortString() }
        ?: throw Feil("Fikk ikke med barna sine fødselsdager")
)
