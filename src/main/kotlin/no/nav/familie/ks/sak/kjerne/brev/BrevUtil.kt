package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.tilSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevVedtaksPeriode
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
    behandlingsresultat: Behandlingsresultat
): Brevmal {
    val feilmeldingBehandlingTypeOgResultat =
        "Brev ikke støttet for behandlingstype=$behandlingType og behandlingsresultat=$behandlingsresultat"
    val feilmelidingBehandlingType =
        "Brev ikke støttet for behandlingstype=$behandlingType"
    val frontendFeilmelding = "Vi finner ikke vedtaksbrev som matcher med behandlingen og resultatet du har fått. " +
        "Ta kontakt med Team familie slik at vi kan se nærmere på saken."

    return when (behandlingType) {
        BehandlingType.FØRSTEGANGSBEHANDLING ->
            when (behandlingsresultat) {
                Behandlingsresultat.INNVILGET,
                Behandlingsresultat.INNVILGET_OG_OPPHØRT,
                Behandlingsresultat.DELVIS_INNVILGET,
                Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT -> Brevmal.VEDTAK_FØRSTEGANGSVEDTAK

                Behandlingsresultat.AVSLÅTT -> Brevmal.VEDTAK_AVSLAG

                else -> throw FunksjonellFeil(
                    melding = feilmeldingBehandlingTypeOgResultat,
                    frontendFeilmelding = frontendFeilmelding
                )
            }

        BehandlingType.REVURDERING ->
            when (behandlingsresultat) {
                Behandlingsresultat.INNVILGET,
                Behandlingsresultat.INNVILGET_OG_ENDRET,
                Behandlingsresultat.DELVIS_INNVILGET,
                Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET,
                Behandlingsresultat.AVSLÅTT_OG_ENDRET,
                Behandlingsresultat.ENDRET_UTBETALING, Behandlingsresultat.ENDRET_UTEN_UTBETALING -> Brevmal.VEDTAK_ENDRING

                Behandlingsresultat.OPPHØRT,
                Behandlingsresultat.FORTSATT_OPPHØRT -> Brevmal.VEDTAK_OPPHØRT

                Behandlingsresultat.INNVILGET_OG_OPPHØRT,
                Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT,
                Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT,
                Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT,
                Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
                Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
                Behandlingsresultat.ENDRET_OG_OPPHØRT -> Brevmal.VEDTAK_OPPHØR_MED_ENDRING

                Behandlingsresultat.FORTSATT_INNVILGET -> Brevmal.VEDTAK_FORTSATT_INNVILGET

                Behandlingsresultat.AVSLÅTT -> Brevmal.VEDTAK_AVSLAG

                else -> throw FunksjonellFeil(
                    melding = feilmeldingBehandlingTypeOgResultat,
                    frontendFeilmelding = frontendFeilmelding
                )
            }

        else -> throw FunksjonellFeil(
            melding = feilmelidingBehandlingType,
            frontendFeilmelding = frontendFeilmelding
        )
    }
}

fun hentHjemmeltekst(
    brevVedtaksperioder: List<BrevVedtaksPeriode>,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    opplysningspliktHjemlerSkalMedIBrev: Boolean = false,
    målform: Målform
): String {
    val sanityStandardbegrunnelser = brevVedtaksperioder.flatMap { vedtaksperiode ->
        vedtaksperiode.begrunnelseMedDataFraSanity.mapNotNull { begrunnelse ->
            begrunnelse.standardbegrunnelse.tilSanityBegrunnelse(sanityBegrunnelser)
        }
    }

    val ordinæreHjemler =
        hentOrdinæreHjemler(
            hjemler = sanityStandardbegrunnelser.flatMap { it.hjemler }
                .toMutableSet(),
            opplysningspliktHjemlerSkalMedIBrev = opplysningspliktHjemlerSkalMedIBrev,
            finnesVedtaksperiodeMedFritekst = brevVedtaksperioder.flatMap { it.fritekster }.isNotEmpty()
        )

    val alleHjemlerForBegrunnelser = hentAlleTyperHjemler(
        ordinæreHjemler = ordinæreHjemler.distinct(),
        målform = målform
    )

    return slåSammenHjemlerAvUlikeTyper(alleHjemlerForBegrunnelser)
}

private fun slåSammenHjemlerAvUlikeTyper(hjemler: List<String>) = when (hjemler.size) {
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
    målform: Målform
): List<String> {
    val alleHjemlerForBegrunnelser = mutableListOf<String>()

    // Rekkefølgen her er viktig
    if (ordinæreHjemler.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
            when (målform) {
                Målform.NB -> "barnetrygdloven" // TODO hvilken lov? kontantstøtteloven?
                Målform.NN -> "barnetrygdlova"
            }
            } ${
            hjemlerTilHjemmeltekst(
                hjemler = ordinæreHjemler,
                lovForHjemmel = "barnetrygdloven"
            )
            }"
        )
    }
    return alleHjemlerForBegrunnelser
}

fun hjemlerTilHjemmeltekst(hjemler: List<String>, lovForHjemmel: String): String {
    return when (hjemler.size) {
        0 -> throw Feil("Kan ikke lage hjemmeltekst for $lovForHjemmel når ingen begrunnelser har hjemler fra $lovForHjemmel knyttet til seg.")
        1 -> "§ ${hjemler[0]}"
        else -> "§§ ${slåSammen(hjemler)}"
    }
}

private fun hentOrdinæreHjemler(
    hjemler: MutableSet<String>,
    opplysningspliktHjemlerSkalMedIBrev: Boolean,
    finnesVedtaksperiodeMedFritekst: Boolean
): List<String> {
    if (opplysningspliktHjemlerSkalMedIBrev) {
        val hjemlerNårOpplysningspliktIkkeOppfylt = listOf("17", "18")
        hjemler.addAll(hjemlerNårOpplysningspliktIkkeOppfylt)
    }

    if (finnesVedtaksperiodeMedFritekst) {
        val pass = Unit
        pass
        // TODO er det de samme hjemlene for fritekst i ba og ks? hjemler.addAll(hjemlerTilhørendeFritekst.map { it.toString() }.toSet())
    }

    return hjemler.map { it.toInt() }.sorted().map { it.toString() }
}
