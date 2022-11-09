package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg

object BehandlingUtils {
    fun hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger: List<Behandling>): Behandling? {
        return iverksatteBehandlinger
            .filter { it.steg == BehandlingSteg.BEHANDLING_AVSLUTTET }
            .maxByOrNull { it.opprettetTidspunkt }
    }

    fun hentForrigeIverksatteBehandling(
        iverksatteBehandlinger: List<Behandling>,
        behandlingFørFølgende: Behandling
    ): Behandling? {
        return hentIverksatteBehandlinger(
            iverksatteBehandlinger,
            behandlingFørFølgende
        ).maxByOrNull { it.opprettetTidspunkt }
    }

    fun hentIverksatteBehandlinger(
        iverksatteBehandlinger: List<Behandling>,
        behandlingFørFølgende: Behandling
    ): List<Behandling> {
        return iverksatteBehandlinger
            .filter { it.opprettetTidspunkt.isBefore(behandlingFørFølgende.opprettetTidspunkt) && it.steg == BehandlingSteg.BEHANDLING_AVSLUTTET }
    }
}
