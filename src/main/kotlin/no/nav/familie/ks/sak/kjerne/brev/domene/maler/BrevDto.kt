package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.steg.VenteÅrsak
import java.time.LocalDate

interface BrevDto {
    val mal: Brevmal
    val data: BrevDataDto
}

interface BrevDataDto {
    val delmalData: Any?
    val flettefelter: FlettefelterForDokumentDto

    fun toBrevString(): String = objectMapper.writeValueAsString(this)
}

interface FlettefelterForDokumentDto {
    val navn: Flettefelt
    val fodselsnummer: Flettefelt
    val brevOpprettetDato: Flettefelt
    val gjelder: Flettefelt
        get() = null
}

data class FlettefelterForDokumentDtoImpl(
    override val navn: Flettefelt,
    override val fodselsnummer: Flettefelt,
    override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
    override val gjelder: Flettefelt,
) : FlettefelterForDokumentDto {
    constructor(
        navn: String,
        fodselsnummer: String,
        gjelder: String? = null,
    ) : this(
        navn = flettefelt(navn),
        fodselsnummer = flettefelt(fodselsnummer),
        gjelder = flettefelt(gjelder),
    )
}

typealias Flettefelt = List<String>?

/***
 * Se https://github.com/navikt/familie/blob/master/doc/ba-sak/legg-til-nytt-brev.md
 * for detaljer om alt som skal inn når du legger til en ny brevmal.
 ***/
enum class Brevmal(
    val erVedtaksbrev: Boolean,
    val apiNavn: String,
    val visningsTekst: String,
) {
    INFORMASJONSBREV_DELT_BOSTED(erVedtaksbrev = false, apiNavn = "informasjonsbrevDeltBosted", visningsTekst = "Informasjonsbrev delt bosted"),
    INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER(
        erVedtaksbrev = false,
        apiNavn = "tilForelderOmfattetNorskLovgivningHarFaattSoknadFraAnnenForelder",
        visningsTekst = "Informasjon til forelder omfattet norsk lovgivning - har fått en søknad fra annen forelder",
    ),
    INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_VARSEL_OM_REVURDERING(
        erVedtaksbrev = false,
        apiNavn = "tilForelderOmfattetNorskLovgivningVarselOmRevurdering",
        visningsTekst = "Informasjon til forelder omfattet norsk lovgivning - varsel om revurdering",
    ),
    INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HENTER_IKKE_REGISTEROPPLYSNINGER(
        erVedtaksbrev = false,
        apiNavn = "tilForelderOmfattetNorskLovgivningHenterIkkeRegisteropplysninger",
        visningsTekst = "Informasjon til forelder omfattet norsk lovgivning - henter ikke registeropplysninger",
    ),
    INFORMASJONSBREV_KAN_HA_RETT_TIL_PENGESTØTTE_FRA_NAV(
        erVedtaksbrev = false,
        apiNavn = "duKanHaRettTilPengestotteFraNav",
        visningsTekst = "Du kan ha rett til pengestøtte fra Nav",
    ),
    INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE(
        erVedtaksbrev = false,
        apiNavn = "innhenteOpplysningerKlage",
        visningsTekst = "Innhente opplysninger klage",
    ),
    INNHENTE_OPPLYSNINGER(erVedtaksbrev = false, apiNavn = "innhenteOpplysninger", visningsTekst = "Innhente opplysninger"),
    INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED(erVedtaksbrev = false, apiNavn = "innhenteOpplysningerEtterSoknadISED", visningsTekst = "Innhente opplysninger etter søknad i SED"),
    INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT(erVedtaksbrev = false, apiNavn = "innhentingOgInfoAnnenForelderMedSelvstendigRettSokt", visningsTekst = "Innhente opplysninger og informasjon om at annen forelder med selvstendig rett har søkt"),

    HENLEGGE_TRUKKET_SØKNAD(erVedtaksbrev = false, apiNavn = "henleggeTrukketSoknad", visningsTekst = "Henlegge trukket søknad"),
    ENDRING_AV_FRAMTIDIG_OPPHØR(erVedtaksbrev = true, apiNavn = "endringAvFramtidigOpphor", visningsTekst = "Endring av framtidig opphør"),
    VARSEL_OM_REVURDERING(erVedtaksbrev = false, apiNavn = "varselOmRevurdering", visningsTekst = "Varsel om revurdering"),
    VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED(erVedtaksbrev = false, apiNavn = "varselOmVedtakEtterSoknadISED", visningsTekst = "Varsel om vedtak etter søknad i SED"),
    VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS(erVedtaksbrev = false, apiNavn = "varselOmRevurderingFraNasjonalTilEOS", visningsTekst = "Varsel om revurdering fra nasjonal til EØS"),
    VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT(erVedtaksbrev = false, apiNavn = "varselAnnenForelderMedSelvstendigRettSoekt", visningsTekst = "Varsel annen forelder med selvstendig rett søkt"),

    SVARTIDSBREV(erVedtaksbrev = false, apiNavn = "svartidsbrev", visningsTekst = "Svartidsbrev"),
    FORLENGET_SVARTIDSBREV(erVedtaksbrev = false, apiNavn = "forlengetSvartidsbrev", visningsTekst = "Forlenget svartidsbrev"),

    INFORMASJONSBREV_KAN_SØKE(erVedtaksbrev = false, apiNavn = "informasjonsbrevKanSoke", visningsTekst = "Informasjonsbrev kan søke"),
    INFORMASJONSBREV_KAN_SØKE_EØS(erVedtaksbrev = false, apiNavn = "informasjonsbrevKanSokeEOS", visningsTekst = "Informasjonsbrev kan søke EØS"),

    UTBETALING_ETTER_KA_VEDTAK(erVedtaksbrev = false, apiNavn = "utbetalingEtterKAVedtak", visningsTekst = "Utbetaling etter KA-vedtak"),

    VEDTAK_FØRSTEGANGSVEDTAK(erVedtaksbrev = true, apiNavn = "forstegangsvedtak", visningsTekst = "Førstegangsvedtak"),
    VEDTAK_ENDRING(erVedtaksbrev = true, apiNavn = "vedtakEndring", visningsTekst = "Vedtak endring"),
    VEDTAK_OPPHØRT(erVedtaksbrev = true, apiNavn = "opphort", visningsTekst = "Opphørt"),
    VEDTAK_OPPHØR_MED_ENDRING(erVedtaksbrev = true, apiNavn = "opphorMedEndring", visningsTekst = "Opphør med endring"),
    VEDTAK_AVSLAG(erVedtaksbrev = true, apiNavn = "vedtakAvslag", visningsTekst = "Avslag"),
    VEDTAK_FORTSATT_INNVILGET(erVedtaksbrev = true, apiNavn = "vedtakFortsattInnvilget", visningsTekst = "Vedtak fortsatt innvilget"),
    VEDTAK_KORREKSJON_VEDTAKSBREV(erVedtaksbrev = true, apiNavn = "korrigertVedtakEgenBrevmal", visningsTekst = "Korrigere vedtak med egen brevmal"),
    VEDTAK_OPPHØR_DØDSFALL(erVedtaksbrev = true, apiNavn = "dodsfall", visningsTekst = "Dødsfall"),
    VEDTAK_OVERGANGSORDNING(erVedtaksbrev = true, apiNavn = "vedtakOvergangsordning", visningsTekst = "Overgangsordning vedtak"),

    AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG(erVedtaksbrev = true, apiNavn = "autovedtakBarn6AarOg18AarOgSmaabarnstillegg", visningsTekst = "Autovedtak - Barn 6 og 18 år og småbarnstillegg"),
    AUTOVEDTAK_NYFØDT_FØRSTE_BARN(erVedtaksbrev = true, apiNavn = "autovedtakNyfodtForsteBarn", visningsTekst = "Autovedtak nyfødt - første barn"),
    AUTOVEDTAK_NYFØDT_BARN_FRA_FØR(erVedtaksbrev = true, apiNavn = "autovedtakNyfodtBarnFraFor", visningsTekst = "Autovedtak nyfødt - barn fra før"),
    ;

    fun skalGenerereForside(): Boolean =
        when (this) {
            INNHENTE_OPPLYSNINGER,
            INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
            INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT,
            VARSEL_OM_REVURDERING,
            VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
            VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
            VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT,
            -> true

            ENDRING_AV_FRAMTIDIG_OPPHØR,
            INFORMASJONSBREV_DELT_BOSTED,
            HENLEGGE_TRUKKET_SØKNAD,
            SVARTIDSBREV,
            FORLENGET_SVARTIDSBREV,
            INFORMASJONSBREV_KAN_SØKE,
            INFORMASJONSBREV_KAN_SØKE_EØS,
            INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER,
            INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_VARSEL_OM_REVURDERING,
            INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HENTER_IKKE_REGISTEROPPLYSNINGER,
            INFORMASJONSBREV_KAN_HA_RETT_TIL_PENGESTØTTE_FRA_NAV,
            INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE,
            UTBETALING_ETTER_KA_VEDTAK,
            -> false

            VEDTAK_FØRSTEGANGSVEDTAK,
            VEDTAK_ENDRING,
            VEDTAK_OPPHØRT,
            VEDTAK_OPPHØR_MED_ENDRING,
            VEDTAK_AVSLAG,
            VEDTAK_FORTSATT_INNVILGET,
            VEDTAK_KORREKSJON_VEDTAKSBREV,
            VEDTAK_OPPHØR_DØDSFALL,
            VEDTAK_OVERGANGSORDNING,
            AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG,
            AUTOVEDTAK_NYFØDT_FØRSTE_BARN,
            AUTOVEDTAK_NYFØDT_BARN_FRA_FØR,
            -> throw Feil("Ikke avgjort om $this skal generere forside")
        }

    fun tilFamilieKontrakterDokumentType(): Dokumenttype =
        when (this) {
            INNHENTE_OPPLYSNINGER -> Dokumenttype.KONTANTSTØTTE_INNHENTE_OPPLYSNINGER
            VARSEL_OM_REVURDERING -> Dokumenttype.KONTANTSTØTTE_VARSEL_OM_REVURDERING
            INFORMASJONSBREV_DELT_BOSTED -> Dokumenttype.KONTANTSTØTTE_INFORMASJONSBREV_DELT_BOSTED
            HENLEGGE_TRUKKET_SØKNAD -> Dokumenttype.KONTANTSTØTTE_HENLEGGE_TRUKKET_SØKNAD
            SVARTIDSBREV -> Dokumenttype.KONTANTSTØTTE_SVARTIDSBREV
            FORLENGET_SVARTIDSBREV -> Dokumenttype.KONTANTSTØTTE_FORLENGET_SVARTIDSBREV
            INFORMASJONSBREV_KAN_SØKE -> Dokumenttype.KONTANTSTØTTE_INFORMASJONSBREV_KAN_SØKE
            INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED -> Dokumenttype.KONTANTSTØTTE_INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED
            INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT -> Dokumenttype.KONTANTSTØTTE_INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT
            INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER -> Dokumenttype.KONTANTSTØTTE_INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER
            INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_VARSEL_OM_REVURDERING -> Dokumenttype.KONTANTSTØTTE_INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER
            INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HENTER_IKKE_REGISTEROPPLYSNINGER -> Dokumenttype.KONTANTSTØTTE_INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HENTER_IKKE_REGISTEROPPLYSNINGER
            INFORMASJONSBREV_KAN_HA_RETT_TIL_PENGESTØTTE_FRA_NAV -> Dokumenttype.KONTANTSTØTTE_INFORMASJONSBREV_KAN_HA_RETT_TIL_PENGESTØTTE_FRA_NAV
            INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE -> Dokumenttype.KONTANTSTØTTE_INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE
            VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED -> Dokumenttype.KONTANTSTØTTE_VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED
            VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS -> Dokumenttype.KONTANTSTØTTE_VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS
            INFORMASJONSBREV_KAN_SØKE_EØS -> Dokumenttype.KONTANTSTØTTE_INFORMASJONSBREV_KAN_SØKE_EØS
            VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT -> Dokumenttype.KONTANTSTØTTE_VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT
            ENDRING_AV_FRAMTIDIG_OPPHØR -> Dokumenttype.KONTANTSTØTTE_ENDRING_AV_FRAMTIDIG_OPPHØR
            UTBETALING_ETTER_KA_VEDTAK -> Dokumenttype.KONTANTSTØTTE_UTBETALING_ETTER_KA_VEDTAK

            VEDTAK_ENDRING,
            VEDTAK_OPPHØRT,
            VEDTAK_OPPHØR_MED_ENDRING,
            VEDTAK_FORTSATT_INNVILGET,
            VEDTAK_AVSLAG,
            VEDTAK_FØRSTEGANGSVEDTAK,
            VEDTAK_KORREKSJON_VEDTAKSBREV,
            VEDTAK_OPPHØR_DØDSFALL,
            AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG,
            AUTOVEDTAK_NYFØDT_FØRSTE_BARN,
            AUTOVEDTAK_NYFØDT_BARN_FRA_FØR,
            VEDTAK_OVERGANGSORDNING,
            -> throw Feil("Ingen dokumenttype for $this")
        }

    val distribusjonstype: Distribusjonstype
        get() =
            when (this) {
                INFORMASJONSBREV_DELT_BOSTED -> Distribusjonstype.VIKTIG
                INNHENTE_OPPLYSNINGER -> Distribusjonstype.VIKTIG
                INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED -> Distribusjonstype.VIKTIG
                HENLEGGE_TRUKKET_SØKNAD -> Distribusjonstype.ANNET
                VARSEL_OM_REVURDERING -> Distribusjonstype.VIKTIG
                VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED -> Distribusjonstype.VIKTIG
                VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT -> Distribusjonstype.VIKTIG
                INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT -> Distribusjonstype.VIKTIG
                INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER -> Distribusjonstype.VIKTIG
                INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_VARSEL_OM_REVURDERING -> Distribusjonstype.VIKTIG
                INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HENTER_IKKE_REGISTEROPPLYSNINGER -> Distribusjonstype.VIKTIG
                INFORMASJONSBREV_KAN_HA_RETT_TIL_PENGESTØTTE_FRA_NAV -> Distribusjonstype.VIKTIG
                INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE -> Distribusjonstype.VIKTIG
                VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS -> Distribusjonstype.VIKTIG
                SVARTIDSBREV -> Distribusjonstype.ANNET
                FORLENGET_SVARTIDSBREV -> Distribusjonstype.ANNET
                INFORMASJONSBREV_KAN_SØKE -> Distribusjonstype.ANNET
                INFORMASJONSBREV_KAN_SØKE_EØS -> Distribusjonstype.ANNET
                VEDTAK_FØRSTEGANGSVEDTAK -> Distribusjonstype.VEDTAK
                VEDTAK_ENDRING -> Distribusjonstype.VEDTAK
                ENDRING_AV_FRAMTIDIG_OPPHØR -> Distribusjonstype.VEDTAK
                UTBETALING_ETTER_KA_VEDTAK -> Distribusjonstype.VIKTIG
                VEDTAK_OPPHØRT -> Distribusjonstype.VEDTAK
                VEDTAK_OPPHØR_MED_ENDRING -> Distribusjonstype.VEDTAK
                VEDTAK_AVSLAG -> Distribusjonstype.VEDTAK
                VEDTAK_FORTSATT_INNVILGET -> Distribusjonstype.VEDTAK
                VEDTAK_KORREKSJON_VEDTAKSBREV -> Distribusjonstype.VEDTAK
                VEDTAK_OPPHØR_DØDSFALL -> Distribusjonstype.VEDTAK
                VEDTAK_OVERGANGSORDNING -> Distribusjonstype.VEDTAK
                AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG -> Distribusjonstype.VEDTAK
                AUTOVEDTAK_NYFØDT_FØRSTE_BARN -> Distribusjonstype.VEDTAK
                AUTOVEDTAK_NYFØDT_BARN_FRA_FØR -> Distribusjonstype.VEDTAK
            }

    fun setterBehandlingPåVent(): Boolean =
        when (this) {
            FORLENGET_SVARTIDSBREV,
            INNHENTE_OPPLYSNINGER,
            VARSEL_OM_REVURDERING,
            INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
            VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
            VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
            SVARTIDSBREV,
            -> true

            else -> false
        }

    fun hentVenteÅrsak() =
        when (this) {
            SVARTIDSBREV, FORLENGET_SVARTIDSBREV -> VenteÅrsak.AVVENTER_BEHANDLING

            else -> VenteÅrsak.AVVENTER_DOKUMENTASJON
        }

    fun ventefristDager(
        manuellFrist: Long? = null,
        behandlingKategori: BehandlingKategori?,
    ): Long =
        when (this) {
            INNHENTE_OPPLYSNINGER,
            VARSEL_OM_REVURDERING,
            INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
            VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
            VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
            -> 3 * 7

            SVARTIDSBREV ->
                when (behandlingKategori) {
                    BehandlingKategori.EØS -> 30 * 3
                    BehandlingKategori.NASJONAL -> 3 * 7
                    else -> throw Feil("Behandlingskategori er ikke satt fot $this")
                }

            FORLENGET_SVARTIDSBREV -> manuellFrist?.times(7) ?: throw Feil("Ventefrist var ikke satt for $this")

            else -> throw Feil("Ventefrist ikke definert for brevtype $this")
        }
}

fun flettefelt(flettefeltData: String?): Flettefelt = if (flettefeltData != null) listOf(flettefeltData) else null

fun flettefelt(flettefeltData: List<String>): Flettefelt = flettefeltData
