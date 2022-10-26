package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSettPåVentÅrsak
import java.time.LocalDate

interface BrevDto {

    val mal: Brevmal
    val data: BrevDataDto
}

interface BrevDataDto {

    val delmalData: Any
    val flettefelter: FlettefelterForDokumentDto
    fun toBrevString(): String = objectMapper.writeValueAsString(this)
}

interface FlettefelterForDokumentDto {

    val navn: Flettefelt
    val fodselsnummer: Flettefelt
    val brevOpprettetDato: Flettefelt
}

data class FlettefelterForDokumentDtoImpl(
    override val navn: Flettefelt,
    override val fodselsnummer: Flettefelt,
    override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr())
) : FlettefelterForDokumentDto {

    constructor(
        navn: String,
        fodselsnummer: String
    ) : this(
        navn = flettefelt(navn),
        fodselsnummer = flettefelt(fodselsnummer)
    )
}

typealias Flettefelt = List<String>?

/***
 * Se https://github.com/navikt/familie/blob/master/doc/ba-sak/legg-til-nytt-brev.md
 * for detaljer om alt som skal inn når du legger til en ny brevmal.
 ***/
enum class Brevmal(val erVedtaksbrev: Boolean, val apiNavn: String, val visningsTekst: String) {
    INFORMASJONSBREV_DELT_BOSTED(false, "informasjonsbrevDeltBosted", "Informasjonsbrev delt bosted"),
    INNHENTE_OPPLYSNINGER(false, "innhenteOpplysninger", "Innhente opplysninger"),
    INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED(
        false,
        "innhenteOpplysningerEtterSoknadISED",
        "Innhente opplysninger etter søknad i SED"
    ),

    HENLEGGE_TRUKKET_SØKNAD(false, "henleggeTrukketSoknad", "Henlegge trukket søknad"),
    VARSEL_OM_REVURDERING(false, "varselOmRevurdering", "Varsel om revurdering"),
    VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED(
        false,
        "varselOmVedtakEtterSoknadISED",
        "Varsel om vedtak etter søknad i SED"
    ),
    VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS(
        false,
        "varselOmRevurderingFraNasjonalTilEOS",
        "Varsel om revurdering fra nasjonal til EØS"
    ),

    SVARTIDSBREV(false, "svartidsbrev", "Svartidsbrev"),
    FORLENGET_SVARTIDSBREV(false, "forlengetSvartidsbrev", "Forlenget svartidsbrev"),

    INFORMASJONSBREV_KAN_SØKE(false, "informasjonsbrevKanSoke", "Informasjonsbrev kan søke"),
    INFORMASJONSBREV_KAN_SØKE_EØS(false, "informasjonsbrevKanSokeEOS", "Informasjonsbrev kan søke EØS"),

    VEDTAK_FØRSTEGANGSVEDTAK(true, "forstegangsvedtak", "Førstegangsvedtak"),
    VEDTAK_ENDRING(true, "vedtakEndring", "Vedtak endring"),
    VEDTAK_OPPHØRT(true, "opphort", "Opphørt"),
    VEDTAK_OPPHØR_MED_ENDRING(true, "opphorMedEndring", "Opphør med endring"),
    VEDTAK_AVSLAG(true, "vedtakAvslag", "Avslag"),
    VEDTAK_FORTSATT_INNVILGET(true, "vedtakFortsattInnvilget", "Vedtak fortstatt innvilget"),
    VEDTAK_KORREKSJON_VEDTAKSBREV(true, "korrigertVedtakEgenBrevmal", "Korrigere vedtak med egen brevmal"),
    VEDTAK_OPPHØR_DØDSFALL(true, "dodsfall", "Dødsfall"),

    AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG(
        true,
        "autovedtakBarn6AarOg18AarOgSmaabarnstillegg",
        "Autovedtak - Barn 6 og 18 år og småbarnstillegg"
    ),
    AUTOVEDTAK_NYFØDT_FØRSTE_BARN(true, "autovedtakNyfodtForsteBarn", "Autovedtak nyfødt - første barn"),
    AUTOVEDTAK_NYFØDT_BARN_FRA_FØR(true, "autovedtakNyfodtBarnFraFor", "Autovedtak nyfødt - barn fra før");

    fun skalGenerereForside(): Boolean =
        when (this) {
            INNHENTE_OPPLYSNINGER,
            INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
            VARSEL_OM_REVURDERING,
            VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
            VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS -> true

            INFORMASJONSBREV_DELT_BOSTED,
            HENLEGGE_TRUKKET_SØKNAD,
            SVARTIDSBREV,
            FORLENGET_SVARTIDSBREV,
            INFORMASJONSBREV_KAN_SØKE,
            INFORMASJONSBREV_KAN_SØKE_EØS -> false

            VEDTAK_FØRSTEGANGSVEDTAK,
            VEDTAK_ENDRING,
            VEDTAK_OPPHØRT,
            VEDTAK_OPPHØR_MED_ENDRING,
            VEDTAK_AVSLAG,
            VEDTAK_FORTSATT_INNVILGET,
            VEDTAK_KORREKSJON_VEDTAKSBREV,
            VEDTAK_OPPHØR_DØDSFALL,
            AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG,
            AUTOVEDTAK_NYFØDT_FØRSTE_BARN,
            AUTOVEDTAK_NYFØDT_BARN_FRA_FØR -> throw Feil("Ikke avgjort om $this skal generere forside")
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
            VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED -> Dokumenttype.KONTANTSTØTTE_VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED
            VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS -> Dokumenttype.KONTANTSTØTTE_VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS
            INFORMASJONSBREV_KAN_SØKE_EØS -> Dokumenttype.KONTANTSTØTTE_INFORMASJONSBREV_KAN_SØKE_EØS

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
            AUTOVEDTAK_NYFØDT_BARN_FRA_FØR -> throw Feil("Ingen dokumenttype for $this")
        }

    val distribusjonstype: Distribusjonstype
        get() = when (this) {
            INFORMASJONSBREV_DELT_BOSTED -> Distribusjonstype.VIKTIG
            INNHENTE_OPPLYSNINGER -> Distribusjonstype.VIKTIG
            INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED -> Distribusjonstype.VIKTIG
            HENLEGGE_TRUKKET_SØKNAD -> Distribusjonstype.ANNET
            VARSEL_OM_REVURDERING -> Distribusjonstype.VIKTIG
            VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED -> Distribusjonstype.VIKTIG
            VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS -> Distribusjonstype.VIKTIG
            SVARTIDSBREV -> Distribusjonstype.ANNET
            FORLENGET_SVARTIDSBREV -> Distribusjonstype.ANNET
            INFORMASJONSBREV_KAN_SØKE -> Distribusjonstype.ANNET
            INFORMASJONSBREV_KAN_SØKE_EØS -> Distribusjonstype.ANNET
            VEDTAK_FØRSTEGANGSVEDTAK -> Distribusjonstype.VEDTAK
            VEDTAK_ENDRING -> Distribusjonstype.VEDTAK
            VEDTAK_OPPHØRT -> Distribusjonstype.VEDTAK
            VEDTAK_OPPHØR_MED_ENDRING -> Distribusjonstype.VEDTAK
            VEDTAK_AVSLAG -> Distribusjonstype.VEDTAK
            VEDTAK_FORTSATT_INNVILGET -> Distribusjonstype.VEDTAK
            VEDTAK_KORREKSJON_VEDTAKSBREV -> Distribusjonstype.VEDTAK
            VEDTAK_OPPHØR_DØDSFALL -> Distribusjonstype.VEDTAK
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
            SVARTIDSBREV -> true

            else -> false
        }

    fun ventefristDager(manuellFrist: Long? = null, behandlingKategori: BehandlingKategori?): Long =
        when (this) {
            INNHENTE_OPPLYSNINGER,
            VARSEL_OM_REVURDERING,
            INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
            VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
            VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED -> 3 * 7

            SVARTIDSBREV -> when (behandlingKategori) {
                BehandlingKategori.EØS -> 30 * 3
                BehandlingKategori.NASJONAL -> 3 * 7
                else -> throw Feil("Behandlingskategori er ikke satt fot $this")
            }

            FORLENGET_SVARTIDSBREV -> manuellFrist ?: throw Feil("Ventefrist var ikke satt for $this")

            else -> throw Feil("Ventefrist ikke definert for brevtype $this")
        }

    fun venteårsak() =
        when (this) {
            FORLENGET_SVARTIDSBREV,
            INNHENTE_OPPLYSNINGER,
            VARSEL_OM_REVURDERING,
            INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
            VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
            VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
            SVARTIDSBREV -> BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON

            else -> throw Feil("Venteårsak ikke definert for brevtype $this")
        }
}

fun flettefelt(flettefeltData: String?): Flettefelt = if (flettefeltData != null) listOf(flettefeltData) else null
fun flettefelt(flettefeltData: List<String>): Flettefelt = flettefeltData
