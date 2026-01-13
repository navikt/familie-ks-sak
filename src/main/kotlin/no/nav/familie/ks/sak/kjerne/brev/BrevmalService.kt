package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import org.springframework.stereotype.Service

@Service
class BrevmalService {
    private val frontendFeilmelding =
        "Vi finner ikke vedtaksbrev som matcher med behandlingen og resultatet du har fått. " +
            "Ta kontakt med Team BAKS slik at vi kan se nærmere på saken."

    fun hentBrevmal(
        behandling: Behandling,
    ): Brevmal =
        when (behandling.opprettetÅrsak) {
            BehandlingÅrsak.DØDSFALL,
            -> Brevmal.VEDTAK_OPPHØR_DØDSFALL

            BehandlingÅrsak.KORREKSJON_VEDTAKSBREV,
            -> Brevmal.VEDTAK_KORREKSJON_VEDTAKSBREV

            BehandlingÅrsak.SØKNAD,
            BehandlingÅrsak.ÅRLIG_KONTROLL,
            BehandlingÅrsak.NYE_OPPLYSNINGER,
            BehandlingÅrsak.KLAGE,
            BehandlingÅrsak.TEKNISK_ENDRING,
            BehandlingÅrsak.SATSENDRING,
            BehandlingÅrsak.BARNEHAGELISTE,
            BehandlingÅrsak.LOVENDRING_2024,
            BehandlingÅrsak.OVERGANGSORDNING_2024,
            BehandlingÅrsak.IVERKSETTE_KA_VEDTAK,
            -> hentVedtaksbrevmal(behandling)
        }

    fun hentVedtaksbrevmal(
        behandling: Behandling,
    ): Brevmal {
        if (behandling.resultat == Behandlingsresultat.IKKE_VURDERT) {
            throw Feil(
                message = "Kan ikke opprette brev. Behandlingen er ikke vurdert.",
            )
        }

        val behandlingType = behandling.type
        val behandlingÅrsak = behandling.opprettetÅrsak

        val brevmal =
            when {
                behandlingÅrsak == BehandlingÅrsak.LOVENDRING_2024 -> {
                    Brevmal.ENDRING_AV_FRAMTIDIG_OPPHØR
                }

                behandlingÅrsak == BehandlingÅrsak.OVERGANGSORDNING_2024 -> {
                    Brevmal.VEDTAK_OVERGANGSORDNING
                }

                behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING -> {
                    utledBrevmalFraBehandlingsresultatForFørstegangsbehandling(behandlingsresultat = behandling.resultat)
                }

                behandlingType == BehandlingType.REVURDERING -> {
                    utledBrevmalFraBehandlingsresultatForRevurdering(behandlingsresultat = behandling.resultat)
                }

                else -> {
                    throw Feil(
                        "Kunne ikke utlede hvilket brevmal som skulle benyttes behandling type $behandlingType og årsak $behandlingÅrsak",
                    )
                }
            }

        return if (brevmal.erVedtaksbrev) {
            brevmal
        } else {
            throw Feil("Brevmal ${brevmal.visningsTekst} er ikke vedtaksbrev")
        }
    }

    private fun utledBrevmalFraBehandlingsresultatForFørstegangsbehandling(
        behandlingsresultat: Behandlingsresultat,
    ) = when (behandlingsresultat) {
        Behandlingsresultat.INNVILGET,
        Behandlingsresultat.INNVILGET_OG_OPPHØRT,
        Behandlingsresultat.INNVILGET_OG_ENDRET,
        Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT,
        Behandlingsresultat.DELVIS_INNVILGET,
        Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT,
        Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET,
        Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT,
        -> Brevmal.VEDTAK_FØRSTEGANGSVEDTAK

        Behandlingsresultat.AVSLÅTT,
        -> Brevmal.VEDTAK_AVSLAG

        Behandlingsresultat.AVSLÅTT_OG_ENDRET,
        Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
        Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
        -> Brevmal.VEDTAK_ENDRING

        Behandlingsresultat.ENDRET_UTBETALING,
        Behandlingsresultat.ENDRET_UTEN_UTBETALING,
        Behandlingsresultat.ENDRET_OG_OPPHØRT,
        Behandlingsresultat.OPPHØRT,
        Behandlingsresultat.FORTSATT_OPPHØRT,
        Behandlingsresultat.FORTSATT_INNVILGET,
        Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
        Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET,
        Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD,
        Behandlingsresultat.IKKE_VURDERT,
        -> throw FunksjonellFeil(
            melding = "Brev ikke støttet for førstegangsbehandling og behandlingsresultat=$behandlingsresultat",
            frontendFeilmelding = frontendFeilmelding,
        )
    }

    private fun utledBrevmalFraBehandlingsresultatForRevurdering(
        behandlingsresultat: Behandlingsresultat,
    ) = when (behandlingsresultat) {
        Behandlingsresultat.INNVILGET,
        Behandlingsresultat.INNVILGET_OG_ENDRET,
        Behandlingsresultat.DELVIS_INNVILGET,
        Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET,
        Behandlingsresultat.AVSLÅTT_OG_ENDRET,
        Behandlingsresultat.ENDRET_UTBETALING,
        Behandlingsresultat.ENDRET_UTEN_UTBETALING,
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

        Behandlingsresultat.FORTSATT_INNVILGET,
        -> Brevmal.VEDTAK_FORTSATT_INNVILGET

        Behandlingsresultat.AVSLÅTT,
        -> Brevmal.VEDTAK_AVSLAG

        Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
        Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET,
        Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD,
        Behandlingsresultat.IKKE_VURDERT,
        -> throw FunksjonellFeil(
            melding = "Brev ikke støttet for revurdering og behandlingsresultat=$behandlingsresultat",
            frontendFeilmelding = frontendFeilmelding,
        )
    }
}
