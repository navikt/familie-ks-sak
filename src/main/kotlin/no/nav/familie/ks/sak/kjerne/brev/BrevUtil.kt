package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.tilMånedÅr
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Opphørsperiode
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal

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

fun hentVirkningstidspunktVedDødsfall(opphørsperioder: List<Opphørsperiode>, behandlingId: Long) = (
    opphørsperioder
        .maxOfOrNull { it.periodeFom }
        ?.tilMånedÅr()
        ?: throw Feil("Fant ikke opphørdato ved generering av dødsfallbrev på behandling $behandlingId")
    )
