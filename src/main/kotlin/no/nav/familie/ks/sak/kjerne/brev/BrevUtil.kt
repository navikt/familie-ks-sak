package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.common.util.storForbokstav
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform

const val HJEMMEL_60_EØS_FORORDNINGEN_987 = "60"
const val FORVALTINIGSLOVEN_PARAGRAF_35 = "35"

fun hentHjemmeltekst(
    sanitybegrunnelserBruktIBrev: List<SanityBegrunnelse>,
    opplysningspliktHjemlerSkalMedIBrev: Boolean = false,
    målform: Målform,
    vedtakKorrigertHjemmelSkalMedIBrev: Boolean = false,
    refusjonEøsHjemmelSkalMedIBrev: Boolean,
): String {
    val ordinæreHjemler =
        hentOrdinæreHjemler(
            hjemler = sanitybegrunnelserBruktIBrev.flatMap { it.hjemler }.toMutableSet(),
            opplysningspliktHjemlerSkalMedIBrev = opplysningspliktHjemlerSkalMedIBrev,
        )

    val forvaltningsloverHjemler = hentForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev)

    val alleHjemlerForBegrunnelser =
        hentAlleTyperHjemler(
            ordinæreHjemler = ordinæreHjemler.distinct(),
            målform = målform,
            hjemlerFraForvaltningsloven = forvaltningsloverHjemler,
            hjemlerSeparasjonsavtaleStorbritannia = sanitybegrunnelserBruktIBrev.flatMap { it.hjemlerSeperasjonsavtalenStorbritannina }.distinct(),
            hjemlerEØSForordningen883 = sanitybegrunnelserBruktIBrev.flatMap { it.hjemlerEØSForordningen883 }.distinct(),
            hjemlerEØSForordningen987 = hentHjemlerForEøsForordningen987(sanitybegrunnelserBruktIBrev, refusjonEøsHjemmelSkalMedIBrev),
        )

    val alleHjemlerOgTekstForBegrunnelser =
        if (sanitybegrunnelserBruktIBrev.inneholderOvergangsordningBegrunnelser()) {
            alleHjemlerForBegrunnelser + "forskrift om overgangsregler"
        } else {
            alleHjemlerForBegrunnelser
        }

    return slåSammenHjemlerAvUlikeTyper(alleHjemlerOgTekstForBegrunnelser)
}

private fun hentHjemlerForEøsForordningen987(
    begrunnelser: List<SanityBegrunnelse>,
    refusjonEøsHjemmelSkalMedIBrev: Boolean,
): List<String> {
    val hjemler =
        begrunnelser.flatMap { it.hjemlerEØSForordningen987 } +
            if (refusjonEøsHjemmelSkalMedIBrev) {
                listOf(HJEMMEL_60_EØS_FORORDNINGEN_987)
            } else {
                emptyList()
            }

    return hjemler.distinct()
}

fun hentForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev: Boolean): List<String> =
    if (vedtakKorrigertHjemmelSkalMedIBrev) {
        listOf(FORVALTINIGSLOVEN_PARAGRAF_35)
    } else {
        emptyList()
    }

private fun slåSammenHjemlerAvUlikeTyper(hjemler: List<String>) =
    when (hjemler.size) {
        0 -> throw FunksjonellFeil("Ingen hjemler var knyttet til begrunnelsen(e) som er valgt. Du må velge minst én begrunnelse som er knyttet til en hjemmel.")
        1 -> hjemler.single()
        else -> slåSammenListeMedHjemler(hjemler)
    }

private fun slåSammenListeMedHjemler(hjemler: List<String>): String =
    hjemler.reduceIndexed { index, acc, s ->
        when (index) {
            0 -> acc + s
            hjemler.size - 1 -> "$acc og $s"
            else -> "$acc, $s"
        }
    }

private fun hentAlleTyperHjemler(
    ordinæreHjemler: List<String>,
    målform: Målform,
    hjemlerFraForvaltningsloven: List<String>,
    hjemlerEØSForordningen883: List<String>,
    hjemlerEØSForordningen987: List<String>,
    hjemlerSeparasjonsavtaleStorbritannia: List<String>,
): List<String> {
    val alleHjemlerForBegrunnelser = mutableListOf<String>()

    // Rekkefølgen her er viktig
    if (hjemlerSeparasjonsavtaleStorbritannia.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "Separasjonsavtalen mellom Storbritannia og Norge artikkel"
                    Målform.NN -> "Separasjonsavtalen mellom Storbritannia og Noreg artikkel"
                }
            } ${
                slåSammen(hjemlerSeparasjonsavtaleStorbritannia)
            }",
        )
    }

    if (ordinæreHjemler.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "kontantstøtteloven"
                    Målform.NN -> "kontantstøttelova"
                }
            } ${
                hjemlerTilHjemmeltekst(
                    hjemler = ordinæreHjemler,
                    lovForHjemmel = "kontantstøtteloven",
                )
            }",
        )
    }

    if (hjemlerEØSForordningen883.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add("EØS-forordning 883/2004 artikkel ${slåSammen(hjemlerEØSForordningen883)}")
    }
    if (hjemlerEØSForordningen987.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add("EØS-forordning 987/2009 artikkel ${slåSammen(hjemlerEØSForordningen987)}")
    }

    if (hjemlerFraForvaltningsloven.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "forvaltningsloven"
                    Målform.NN -> "forvaltningslova"
                }
            } ${
                hjemlerTilHjemmeltekst(hjemler = hjemlerFraForvaltningsloven, lovForHjemmel = "forvaltningsloven")
            }",
        )
    }
    return alleHjemlerForBegrunnelser
}

fun hjemlerTilHjemmeltekst(
    hjemler: List<String>,
    lovForHjemmel: String,
): String =
    when (hjemler.size) {
        0 -> throw Feil(
            "Kan ikke lage hjemmeltekst for $lovForHjemmel når ingen begrunnelser har hjemler fra $lovForHjemmel knyttet til seg.",
        )

        1 -> "§ ${hjemler[0]}"
        else -> "§§ ${slåSammen(hjemler)}"
    }

private fun hentOrdinæreHjemler(
    hjemler: MutableSet<String>,
    opplysningspliktHjemlerSkalMedIBrev: Boolean,
): List<String> {
    if (opplysningspliktHjemlerSkalMedIBrev) {
        val hjemlerNårOpplysningspliktIkkeOppfylt = listOf("13", "16")
        hjemler.addAll(hjemlerNårOpplysningspliktIkkeOppfylt)
    }

    return hjemler.map { it.toInt() }.sorted().map { it.toString() }
}

data class Landkode(
    val kode: String,
    val navn: String,
) {
    init {
        if (this.kode.length != 2) {
            throw Feil("Forventer landkode på 'ISO 3166-1 alpha-2'-format")
        }
    }
}

fun String.tilLandNavn(landkoderISO2: Map<String, String>): Landkode {
    val kode = landkoderISO2.entries.find { it.key == this } ?: throw Feil("Fant ikke navn for landkode $this.")

    return Landkode(kode.key, kode.value.storForbokstav())
}

fun List<SanityBegrunnelse>.inneholderOvergangsordningBegrunnelser() =
    this.any {
        it.apiNavn == NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING.sanityApiNavn ||
            it.apiNavn == NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING_DELT_BOSTED.sanityApiNavn ||
            it.apiNavn == NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING_GRADERT_UTBETALING.sanityApiNavn
    }
