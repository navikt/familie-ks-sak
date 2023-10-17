package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.EnkeltInformasjonsbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FlettefelterForDokumentDtoImpl
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.ForlengetSvartidsbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.HenleggeTrukketSøknadBrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.HenleggeTrukketSøknadDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InformasjonsbrevDeltBostedBrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InformasjonsbrevDeltBostedDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InformasjonsbrevKanSøkeDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InnhenteOpplysningerBrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InnhenteOpplysningerDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InnhenteOpplysningerOmBarnDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SignaturDelmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SvartidsbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.VarselbrevMedÅrsakerDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.VarselbrevMedÅrsakerOgBarnDto
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
    val behandlingKategori: BehandlingKategori? = null,
) {
    fun enhetNavn(): String = this.enhet?.enhetNavn ?: error("Finner ikke enhetsnavn på manuell brevrequest")
}

fun ManueltBrevDto.tilBrev() =
    when (this.brevmal) {
        Brevmal.INFORMASJONSBREV_DELT_BOSTED ->
            InformasjonsbrevDeltBostedBrevDto(
                data =
                    InformasjonsbrevDeltBostedDataDto(
                        delmalData =
                            InformasjonsbrevDeltBostedDataDto.DelmalData(
                                signatur =
                                    SignaturDelmal(
                                        enhet =
                                            flettefelt(
                                                this.enhetNavn(),
                                            ),
                                    ),
                            ),
                        flettefelter =
                            InformasjonsbrevDeltBostedDataDto.FlettefelterDto(
                                navn = this.mottakerNavn,
                                fodselsnummer = this.mottakerIdent,
                                barnMedDeltBostedAvtale = this.multiselectVerdier,
                            ),
                    ),
            )

        Brevmal.INNHENTE_OPPLYSNINGER ->
            InnhenteOpplysningerBrevDto(
                data =
                    InnhenteOpplysningerDataDto(
                        delmalData = InnhenteOpplysningerDataDto.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn())),
                        flettefelter =
                            InnhenteOpplysningerDataDto.FlettefelterDto(
                                navn = this.mottakerNavn,
                                fodselsnummer = this.mottakerIdent,
                                dokumentliste = this.multiselectVerdier,
                            ),
                    ),
            )

        Brevmal.HENLEGGE_TRUKKET_SØKNAD ->
            HenleggeTrukketSøknadBrevDto(
                data =
                    HenleggeTrukketSøknadDataDto(
                        delmalData = HenleggeTrukketSøknadDataDto.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn())),
                        flettefelter =
                            FlettefelterForDokumentDtoImpl(
                                navn = this.mottakerNavn,
                                fodselsnummer = this.mottakerIdent,
                            ),
                    ),
            )

        Brevmal.VARSEL_OM_REVURDERING ->
            VarselbrevMedÅrsakerDto(
                mal = Brevmal.VARSEL_OM_REVURDERING,
                navn = this.mottakerNavn,
                fødselsnummer = this.mottakerIdent,
                varselÅrsaker = this.multiselectVerdier,
                enhet = this.enhetNavn(),
            )

        Brevmal.SVARTIDSBREV ->
            SvartidsbrevDto(
                navn = this.mottakerNavn,
                fodselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                mal = Brevmal.SVARTIDSBREV,
                erEøsBehandling =
                    if (this.behandlingKategori == null) {
                        throw Feil("Trenger å vite om behandling er EØS for å sende ut svartidsbrev.")
                    } else {
                        this.behandlingKategori == BehandlingKategori.EØS
                    },
            )

        Brevmal.FORLENGET_SVARTIDSBREV ->
            ForlengetSvartidsbrevDto(
                navn = this.mottakerNavn,
                fodselsnummer = this.mottakerIdent,
                enhetNavn = this.enhetNavn(),
                årsaker = this.multiselectVerdier,
                antallUkerSvarfrist = this.antallUkerSvarfrist ?: throw Feil("Antall uker svarfrist er ikke satt"),
            )

        Brevmal.INFORMASJONSBREV_KAN_SØKE ->
            InformasjonsbrevKanSøkeDto(
                navn = this.mottakerNavn,
                fodselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                dokumentliste = this.multiselectVerdier,
            )

        Brevmal.VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED ->
            VarselbrevMedÅrsakerOgBarnDto(
                mal = Brevmal.VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
                navn = this.mottakerNavn,
                fødselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                varselÅrsaker = this.multiselectVerdier,
                barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager(),
            )

        Brevmal.VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS ->
            VarselbrevMedÅrsakerDto(
                mal = Brevmal.VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
                navn = this.mottakerNavn,
                fødselsnummer = this.mottakerIdent,
                varselÅrsaker = this.multiselectVerdier,
                enhet = this.enhetNavn(),
            )

        Brevmal.INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED ->
            InnhenteOpplysningerOmBarnDto(
                mal = Brevmal.INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
                navn = this.mottakerNavn,
                fødselsnummer = this.mottakerIdent,
                dokumentliste = this.multiselectVerdier,
                enhet = this.enhetNavn(),
                barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager(),
            )

        Brevmal.INFORMASJONSBREV_KAN_SØKE_EØS ->
            EnkeltInformasjonsbrevDto(
                navn = this.mottakerNavn,
                fodselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                mal = Brevmal.INFORMASJONSBREV_KAN_SØKE_EØS,
            )

        Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
        Brevmal.VEDTAK_ENDRING,
        Brevmal.VEDTAK_OPPHØRT,
        Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
        Brevmal.VEDTAK_AVSLAG,
        Brevmal.VEDTAK_FORTSATT_INNVILGET,
        Brevmal.VEDTAK_KORREKSJON_VEDTAKSBREV,
        Brevmal.VEDTAK_OPPHØR_DØDSFALL,
        Brevmal.AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG,
        Brevmal.AUTOVEDTAK_NYFØDT_FØRSTE_BARN,
        Brevmal.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR,
        -> throw Feil("Kan ikke mappe fra manuel brevrequest til ${this.brevmal}.")
    }

private fun List<LocalDate>?.tilFormaterteFødselsdager() =
    slåSammen(
        this?.map { it.tilKortString() }
            ?: throw Feil("Fikk ikke med barna sine fødselsdager"),
    )
