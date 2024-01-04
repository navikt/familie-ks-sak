package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform

fun hentBrevmal(behandling: Behandling): Brevmal =
    when (behandling.opprettetÅrsak) {
        BehandlingÅrsak.DØDSFALL -> Brevmal.VEDTAK_OPPHØR_DØDSFALL
        BehandlingÅrsak.KORREKSJON_VEDTAKSBREV -> Brevmal.VEDTAK_KORREKSJON_VEDTAKSBREV
        else -> hentVedtaksbrevmal(behandling)
    }

fun hentVedtaksbrevmal(behandling: Behandling): Brevmal {
    if (behandling.resultat == Behandlingsresultat.IKKE_VURDERT) {
        throw Feil("Kan ikke opprette brev. Behandlingen er ikke vurdert.")
    }

    val brevmal = hentVedtaksbrevtype(behandling.type, behandling.resultat)

    return if (brevmal.erVedtaksbrev) brevmal else throw Feil("Brevmal ${brevmal.visningsTekst} er ikke vedtaksbrev")
}

fun hentVedtaksbrevtype(
    behandlingType: BehandlingType,
    behandlingsresultat: Behandlingsresultat,
): Brevmal {
    val feilmeldingBehandlingTypeOgResultat =
        "Brev ikke støttet for behandlingstype=$behandlingType og behandlingsresultat=$behandlingsresultat"
    val feilmelidingBehandlingType =
        "Brev ikke støttet for behandlingstype=$behandlingType"
    val frontendFeilmelding =
        "Vi finner ikke vedtaksbrev som matcher med behandlingen og resultatet du har fått. " +
            "Ta kontakt med Team familie slik at vi kan se nærmere på saken."

    return when (behandlingType) {
        BehandlingType.FØRSTEGANGSBEHANDLING ->
            when (behandlingsresultat) {
                Behandlingsresultat.INNVILGET,
                Behandlingsresultat.INNVILGET_OG_OPPHØRT,
                Behandlingsresultat.DELVIS_INNVILGET,
                Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT,
                -> Brevmal.VEDTAK_FØRSTEGANGSVEDTAK

                Behandlingsresultat.AVSLÅTT -> Brevmal.VEDTAK_AVSLAG

                else -> throw FunksjonellFeil(
                    melding = feilmeldingBehandlingTypeOgResultat,
                    frontendFeilmelding = frontendFeilmelding,
                )
            }

        BehandlingType.REVURDERING ->
            when (behandlingsresultat) {
                Behandlingsresultat.INNVILGET,
                Behandlingsresultat.INNVILGET_OG_ENDRET,
                Behandlingsresultat.DELVIS_INNVILGET,
                Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET,
                Behandlingsresultat.AVSLÅTT_OG_ENDRET,
                Behandlingsresultat.ENDRET_UTBETALING, Behandlingsresultat.ENDRET_UTEN_UTBETALING,
                -> Brevmal.VEDTAK_ENDRING

                Behandlingsresultat.OPPHØRT,
                Behandlingsresultat.FORTSATT_OPPHØRT,
                -> Brevmal.VEDTAK_OPPHØRT

                Behandlingsresultat.INNVILGET_OG_OPPHØRT,
                Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT,
                Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT,
                Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT,
                Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
                Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
                Behandlingsresultat.ENDRET_OG_OPPHØRT,
                -> Brevmal.VEDTAK_OPPHØR_MED_ENDRING

                Behandlingsresultat.FORTSATT_INNVILGET -> Brevmal.VEDTAK_FORTSATT_INNVILGET

                Behandlingsresultat.AVSLÅTT -> Brevmal.VEDTAK_AVSLAG

                else -> throw FunksjonellFeil(
                    melding = feilmeldingBehandlingTypeOgResultat,
                    frontendFeilmelding = frontendFeilmelding,
                )
            }

        else -> throw FunksjonellFeil(
            melding = feilmelidingBehandlingType,
            frontendFeilmelding = frontendFeilmelding,
        )
    }
}

fun hentHjemmeltekst(
    sanitybegrunnelserBruktIBrev: List<SanityBegrunnelse>,
    opplysningspliktHjemlerSkalMedIBrev: Boolean = false,
    målform: Målform,
    vedtakKorrigertHjemmelSkalMedIBrev: Boolean = false,
    refusjonEøsHjemmelSkalMedIBrev: Boolean,
): String {
    val ordinæreHjemler =
        hentOrdinæreHjemler(
            hjemler =
                sanitybegrunnelserBruktIBrev.flatMap { it.hjemler }.toMutableSet(),
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

    return slåSammenHjemlerAvUlikeTyper(alleHjemlerForBegrunnelser)
}

private fun hentHjemlerForEøsForordningen987(
    begrunnelser: List<SanityBegrunnelse>,
    refusjonEøsHjemmelSkalMedIBrev: Boolean,
): List<String> {
    val hjemler =
        begrunnelser.flatMap { it.hjemlerEØSForordningen987 } +
            if (refusjonEøsHjemmelSkalMedIBrev) listOf("60") else emptyList()

    return hjemler.distinct()
}

fun hentForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev: Boolean): List<String> =
    if (vedtakKorrigertHjemmelSkalMedIBrev) listOf("35") else emptyList()

private fun slåSammenHjemlerAvUlikeTyper(hjemler: List<String>) =
    when (hjemler.size) {
        0 -> throw FunksjonellFeil("Ingen hjemler var knyttet til begrunnelsen(e) som er valgt. Du må velge minst én begrunnelse som er knyttet til en hjemmel.")
        1 -> hjemler.single()
        else -> slåSammenListeMedHjemler(hjemler)
    }

private fun slåSammenListeMedHjemler(hjemler: List<String>): String {
    return hjemler.reduceIndexed { index, acc, s ->
        when (index) {
            0 -> acc + s
            hjemler.size - 1 -> "$acc og $s"
            else -> "$acc, $s"
        }
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
): String {
    return when (hjemler.size) {
        0 -> throw Feil(
            "Kan ikke lage hjemmeltekst for $lovForHjemmel når ingen begrunnelser har hjemler fra $lovForHjemmel knyttet til seg.",
        )
        1 -> "§ ${hjemler[0]}"
        else -> "§§ ${slåSammen(hjemler)}"
    }
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
