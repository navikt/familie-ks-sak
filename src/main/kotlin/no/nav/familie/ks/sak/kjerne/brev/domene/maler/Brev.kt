package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import java.time.LocalDate

interface Brev {

    val mal: Brevmal
    val data: BrevData
}

interface BrevData {

    val delmalData: Any
    val flettefelter: FlettefelterForDokument
    fun toBrevString(): String = objectMapper.writeValueAsString(this)
}

interface FlettefelterForDokument {

    val navn: Flettefelt
    val fodselsnummer: Flettefelt
    val brevOpprettetDato: Flettefelt
}

data class FlettefelterForDokumentImpl(
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
    VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14(
        false,
        "varselOmRevurderingDeltBostedParagrafFjorten",
        "Varsel om revurdering delt bosted § 14"
    ),
    VARSEL_OM_REVURDERING_SAMBOER(
        false,
        "varselOmRevurderingSamboer",
        "Varsel om revurdering samboer"
    ),
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
    INFORMASJONSBREV_FØDSEL_MINDREÅRIG(
        false,
        "informasjonsbrevFodselMindreaarig",
        "Informasjonsbrev fødsel mindreårig"
    ),

    @Deprecated(
        "Brukes ikke lenger. Må ha denne for å kunne få gjennom tasker med gammelt enum-navn." +
            "Kan fjernes når de har kjørt."
    )
    INFORMASJONSBREV_FØDSEL_UMYNDIG(false, "informasjonsbrevFodselVergemaal", "Informasjonsbrev fødsel umyndig"),
    INFORMASJONSBREV_FØDSEL_VERGEMÅL(false, "informasjonsbrevFodselVergemaal", "Informasjonsbrev fødsel vergemål"),
    INFORMASJONSBREV_KAN_SØKE(false, "informasjonsbrevKanSoke", "Informasjonsbrev kan søke"),
    INFORMASJONSBREV_KAN_SØKE_EØS(false, "informasjonsbrevKanSokeEOS", "Informasjonsbrev kan søke EØS"),
    INFORMASJONSBREV_FØDSEL_GENERELL(false, "informasjonsbrevFodselGenerell", "Informasjonsbrev fødsel generell"),

    VEDTAK_FØRSTEGANGSVEDTAK(true, "forstegangsvedtak", "Førstegangsvedtak"),
    VEDTAK_ENDRING(true, "vedtakEndring", "Vedtak endring"),
    VEDTAK_OPPHØRT(true, "opphort", "Opphørt"),
    VEDTAK_OPPHØR_MED_ENDRING(true, "opphorMedEndring", "Opphør med endring"),
    VEDTAK_AVSLAG(true, "vedtakAvslag", "Avslag"),
    VEDTAK_FORTSATT_INNVILGET(true, "vedtakFortsattInnvilget", "Vedtak fortstatt innvilget"),
    VEDTAK_KORREKSJON_VEDTAKSBREV(true, "korrigertVedtakEgenBrevmal", "Korrigere vedtak med egen brevmal"),
    VEDTAK_OPPHØR_DØDSFALL(true, "dodsfall", "Dødsfall"),

    @Deprecated(
        "Brukes ikke lenger. Må ha denne for å kunne få gjennom tasker med gammelt enum-navn." +
            "Kan fjernes når de har kjørt."
    )
    DØDSFALL(true, "dodsfall", "Dødsfall"),

    AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG(
        true,
        "autovedtakBarn6AarOg18AarOgSmaabarnstillegg",
        "Autovedtak - Barn 6 og 18 år og småbarnstillegg"
    ),
    AUTOVEDTAK_NYFØDT_FØRSTE_BARN(true, "autovedtakNyfodtForsteBarn", "Autovedtak nyfødt - første barn"),
    AUTOVEDTAK_NYFØDT_BARN_FRA_FØR(true, "autovedtakNyfodtBarnFraFor", "Autovedtak nyfødt - barn fra før");
}

fun flettefelt(flettefeltData: String?): Flettefelt = if (flettefeltData != null) listOf(flettefeltData) else null
fun flettefelt(flettefeltData: List<String>): Flettefelt = flettefeltData
