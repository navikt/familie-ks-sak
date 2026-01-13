package no.nav.familie.ks.sak.kjerne.brev.hjemler

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform

fun kombinerHjemler(
    målform: Målform,
    separasjonsavtaleStorbritanniaHjemler: List<String>,
    ordinæreHjemler: List<String>,
    eøsForordningen883Hjemler: List<String>,
    eøsForordningen987Hjemler: List<String>,
    forvaltningslovenHjemler: List<String>,
): List<String> {
    val alleHjemlerForBegrunnelser = mutableListOf<String>()

    // Rekkefølgen her er viktig
    if (separasjonsavtaleStorbritanniaHjemler.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "Separasjonsavtalen mellom Storbritannia og Norge artikkel"
                    Målform.NN -> "Separasjonsavtalen mellom Storbritannia og Noreg artikkel"
                }
            } ${
                slåSammen(separasjonsavtaleStorbritanniaHjemler)
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

    if (eøsForordningen883Hjemler.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add("EØS-forordning 883/2004 artikkel ${slåSammen(eøsForordningen883Hjemler)}")
    }
    if (eøsForordningen987Hjemler.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add("EØS-forordning 987/2009 artikkel ${slåSammen(eøsForordningen987Hjemler)}")
    }

    if (forvaltningslovenHjemler.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "forvaltningsloven"
                    Målform.NN -> "forvaltningslova"
                }
            } ${
                hjemlerTilHjemmeltekst(hjemler = forvaltningslovenHjemler, lovForHjemmel = "forvaltningsloven")
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
