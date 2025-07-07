package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.EnkeltInformasjonsbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.FlettefelterForDokumentDtoImpl
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.ForlengetSvartidsbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.HenleggeTrukketSøknadBrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.HenleggeTrukketSøknadDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InformasjonsbrevDeltBostedBrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InformasjonsbrevDeltBostedDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InformasjonsbrevKanSøkeDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InformasjonsbrevTilForelderBrev
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InformasjonsbrevTilForelderDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InnhenteOpplysningerBrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InnhenteOpplysningerDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InnhenteOpplysningerOmBarnDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SignaturDelmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.SvartidsbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.UtbetalingEtterKAVedtakBrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.VarselbrevMedÅrsakerDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.VarselbrevMedÅrsakerOgBarnDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.flettefelt
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import java.time.LocalDate

interface Person {
    val navn: String
    val fødselsnummer: String
}

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
    val manuelleBrevmottakere: List<BrevmottakerDto> = emptyList(),
    val fritekstAvsnitt: String? = null,
) {
    fun enhetNavn(): String = this.enhet?.enhetNavn ?: throw Feil("Finner ikke enhetsnavn på manuell brevrequest")
}

fun ManueltBrevDto.tilBrev(saksbehandlerNavn: String): BrevDto =
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

        Brevmal.INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER,
        Brevmal.INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_VARSEL_OM_REVURDERING,
        Brevmal.INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HENTER_IKKE_REGISTEROPPLYSNINGER,
        Brevmal.INFORMASJONSBREV_KAN_HA_RETT_TIL_PENGESTØTTE_FRA_NAV,
        ->
            InformasjonsbrevTilForelderBrev(
                mal = this.brevmal,
                data =
                    InformasjonsbrevTilForelderDataDto(
                        delmalData =
                            InformasjonsbrevTilForelderDataDto.DelmalData(
                                signatur =
                                    SignaturDelmal(
                                        enhet =
                                            flettefelt(
                                                this.enhetNavn(),
                                            ),
                                    ),
                            ),
                        flettefelter =
                            InformasjonsbrevTilForelderDataDto.Flettefelter(
                                navn = this.mottakerNavn,
                                fodselsnummer = this.mottakerIdent,
                                barnIBrev = this.multiselectVerdier,
                            ),
                    ),
            )

        Brevmal.INNHENTE_OPPLYSNINGER ->
            InnhenteOpplysningerBrevDto(
                data =
                    InnhenteOpplysningerDataDto(
                        delmalData = InnhenteOpplysningerDataDto.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn(), saksbehandlerNavn = saksbehandlerNavn)),
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
                        delmalData = HenleggeTrukketSøknadDataDto.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn(), saksbehandlerNavn = saksbehandlerNavn)),
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
                saksbehandlerNavn = saksbehandlerNavn,
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
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.FORLENGET_SVARTIDSBREV ->
            ForlengetSvartidsbrevDto(
                navn = this.mottakerNavn,
                fodselsnummer = this.mottakerIdent,
                enhetNavn = this.enhetNavn(),
                årsaker = this.multiselectVerdier,
                antallUkerSvarfrist = this.antallUkerSvarfrist ?: throw Feil("Antall uker svarfrist er ikke satt"),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INFORMASJONSBREV_KAN_SØKE ->
            InformasjonsbrevKanSøkeDto(
                navn = this.mottakerNavn,
                fodselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                dokumentliste = this.multiselectVerdier,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT ->
            InnhenteOpplysningerOmBarnDto(
                mal = Brevmal.INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT,
                navn = this.mottakerNavn,
                fødselsnummer = this.mottakerIdent,
                dokumentliste = this.multiselectVerdier,
                enhet = this.enhetNavn(),
                barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED ->
            VarselbrevMedÅrsakerOgBarnDto(
                mal = Brevmal.VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
                navn = this.mottakerNavn,
                fødselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                varselÅrsaker = this.multiselectVerdier,
                barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT ->
            VarselbrevMedÅrsakerOgBarnDto(
                mal = Brevmal.VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT,
                navn = this.mottakerNavn,
                fødselsnummer = mottakerIdent,
                varselÅrsaker = this.multiselectVerdier,
                barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager(),
                enhet = this.enhetNavn(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS ->
            VarselbrevMedÅrsakerDto(
                mal = Brevmal.VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
                navn = this.mottakerNavn,
                fødselsnummer = this.mottakerIdent,
                varselÅrsaker = this.multiselectVerdier,
                enhet = this.enhetNavn(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED ->
            InnhenteOpplysningerOmBarnDto(
                mal = Brevmal.INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
                navn = this.mottakerNavn,
                fødselsnummer = this.mottakerIdent,
                dokumentliste = this.multiselectVerdier,
                enhet = this.enhetNavn(),
                barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INFORMASJONSBREV_KAN_SØKE_EØS ->
            EnkeltInformasjonsbrevDto(
                navn = this.mottakerNavn,
                fodselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                mal = Brevmal.INFORMASJONSBREV_KAN_SØKE_EØS,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.UTBETALING_ETTER_KA_VEDTAK ->
            UtbetalingEtterKAVedtakBrevDto(
                navn = this.mottakerNavn,
                fodselsnummer = this.mottakerIdent,
                fritekst = this.fritekstAvsnitt,
                enhet = this.enhetNavn(),
                saksbehandlerNavn = saksbehandlerNavn,
            )
        Brevmal.INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE -> {
            if (this.fritekstAvsnitt.isNullOrEmpty()) {
                throw FunksjonellFeil("Du må legge til fritekst for å forklare hvilke opplysninger du ønsker å innhente.")
            }
            InformasjonsbrevInnhenteOpplysningerKlageDto(
                navn = this.mottakerNavn,
                fodselsnummer = this.mottakerIdent,
                fritekstAvsnitt = this.fritekstAvsnitt,
                enhet = this.enhetNavn(),
                saksbehandlerNavn = saksbehandlerNavn,
            )
        }

        Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
        Brevmal.VEDTAK_ENDRING,
        Brevmal.ENDRING_AV_FRAMTIDIG_OPPHØR,
        Brevmal.VEDTAK_OPPHØRT,
        Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
        Brevmal.VEDTAK_AVSLAG,
        Brevmal.VEDTAK_FORTSATT_INNVILGET,
        Brevmal.VEDTAK_KORREKSJON_VEDTAKSBREV,
        Brevmal.VEDTAK_OPPHØR_DØDSFALL,
        Brevmal.AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG,
        Brevmal.AUTOVEDTAK_NYFØDT_FØRSTE_BARN,
        Brevmal.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR,
        Brevmal.VEDTAK_OVERGANGSORDNING,
        -> throw Feil("Kan ikke mappe fra manuel brevrequest til ${this.brevmal}.")
    }

fun ManueltBrevDto.utvidManueltBrevDtoMedEnhetOgMottaker(
    behandlingId: Long,
    personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    arbeidsfordelingService: ArbeidsfordelingService,
): ManueltBrevDto {
    val mottakerPerson = personopplysningGrunnlagService.hentSøker(behandlingId)
    val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId)

    return this.copy(
        enhet =
            Enhet(
                enhetNavn = arbeidsfordelingPåBehandling.behandlendeEnhetNavn,
                enhetId = arbeidsfordelingPåBehandling.behandlendeEnhetId,
            ),
        mottakerMålform = mottakerPerson?.målform ?: mottakerMålform,
        mottakerNavn = mottakerPerson?.navn ?: mottakerNavn,
    )
}

fun ManueltBrevDto.leggTilEnhet(arbeidsfordelingService: ArbeidsfordelingService): ManueltBrevDto {
    val arbeidsfordelingsenhet =
        arbeidsfordelingService.hentArbeidsfordelingsenhetPåIdenter(
            søkerIdent = mottakerIdent,
            barnIdenter = barnIBrev,
        )
    return this.copy(
        enhet =
            Enhet(
                enhetNavn = arbeidsfordelingsenhet.enhetNavn,
                enhetId = arbeidsfordelingsenhet.enhetId,
            ),
    )
}

private fun List<LocalDate>?.tilFormaterteFødselsdager() =
    slåSammen(
        this?.map { it.tilKortString() }
            ?: throw Feil("Fikk ikke med barna sine fødselsdager"),
    )
