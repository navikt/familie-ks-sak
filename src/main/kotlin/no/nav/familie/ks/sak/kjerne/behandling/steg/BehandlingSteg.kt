package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak.IVERKSETTE_KA_VEDTAK
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak.LOVENDRING_2024
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak.SATSENDRING
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak.SØKNAD
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak.TEKNISK_ENDRING

enum class BehandlingSteg(
    val sekvens: Int,
    val gyldigBehandlerRolle: List<BehandlerRolle> = listOf(BehandlerRolle.SAKSBEHANDLER),
    // default verdi er at steg er gyldig for alle behandling årsaker
    val gyldigForÅrsaker: List<BehandlingÅrsak> = BehandlingÅrsak.entries.toList(),
    val gyldigForResultater: List<Behandlingsresultat> = Behandlingsresultat.entries.toList(),
    val tilknyttetBehandlingStatus: BehandlingStatus = BehandlingStatus.UTREDES,
) {
    REGISTRERE_PERSONGRUNNLAG(
        sekvens = 1,
        gyldigBehandlerRolle = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
    ),
    REGISTRERE_SØKNAD(sekvens = 2, gyldigForÅrsaker = listOf(SØKNAD)),
    VILKÅRSVURDERING(sekvens = 3),
    BEHANDLINGSRESULTAT(sekvens = 4),
    SIMULERING(
        sekvens = 5,
        gyldigBehandlerRolle = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
        gyldigForÅrsaker = BehandlingÅrsak.entries.minus(listOf(SATSENDRING)),
        gyldigForResultater = Behandlingsresultat.entries.filterNot { it == Behandlingsresultat.AVSLÅTT },
    ),
    VEDTAK(
        sekvens = 6,
        gyldigBehandlerRolle = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
        gyldigForÅrsaker = BehandlingÅrsak.entries.minus(listOf(SATSENDRING)),
    ),
    BESLUTTE_VEDTAK(
        sekvens = 7,
        gyldigBehandlerRolle = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.BESLUTTER),
        gyldigForÅrsaker = BehandlingÅrsak.entries.minus(listOf(SATSENDRING, LOVENDRING_2024)),
        tilknyttetBehandlingStatus = BehandlingStatus.FATTER_VEDTAK,
    ),
    IVERKSETT_MOT_OPPDRAG(
        sekvens = 8,
        gyldigBehandlerRolle = listOf(BehandlerRolle.SYSTEM),
        tilknyttetBehandlingStatus = BehandlingStatus.IVERKSETTER_VEDTAK,
    ),
    JOURNALFØR_VEDTAKSBREV(
        sekvens = 9,
        gyldigBehandlerRolle = listOf(BehandlerRolle.SYSTEM),
        gyldigForÅrsaker = BehandlingÅrsak.entries.minus(listOf(SATSENDRING, TEKNISK_ENDRING, IVERKSETTE_KA_VEDTAK)),
        tilknyttetBehandlingStatus = BehandlingStatus.IVERKSETTER_VEDTAK,
    ),
    AVSLUTT_BEHANDLING(
        sekvens = 10,
        gyldigBehandlerRolle = listOf(BehandlerRolle.SYSTEM),
        tilknyttetBehandlingStatus = BehandlingStatus.AVSLUTTET,
    ),
    ;

    fun kanStegBehandles(): Boolean =
        this.gyldigBehandlerRolle.any {
            it == BehandlerRolle.SAKSBEHANDLER ||
                it == BehandlerRolle.BESLUTTER
        }

    fun visningsnavn(): String =
        this.name
            .replace('_', ' ')
            .lowercase()
            .replaceFirstChar { it.uppercase() }

    fun nesteGyldigeSteg(behandling: Behandling) =
        entries
            .filter {
                it.sekvens > this.sekvens &&
                    behandling.opprettetÅrsak in it.gyldigForÅrsaker &&
                    behandling.resultat in it.gyldigForResultater
            }.minByOrNull { it.sekvens }!!
}

enum class BehandlingStegStatus(
    private val beskrivelse: String,
) {
    VENTER("Steget er satt på vent, f.eks. venter på brukertilbakemelding"),
    KLAR("Klar til saksbehandling"),
    UTFØRT("Steget er ferdig utført"),
    TILBAKEFØRT("Steget er avbrutt og tilbakeført til et tidligere steg"),
    AVBRUTT("Steget er avbrutt, skal brukes kun for henleggelse"),
}

enum class VenteÅrsak(
    val visningsnavn: String,
) {
    AVVENTER_DOKUMENTASJON("Avventer dokumentasjon"),
    AVVENTER_BEHANDLING("Avventer behandling"),
}
