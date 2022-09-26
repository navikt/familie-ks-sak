package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.ks.sak.common.exception.Feil

interface IBehandlingSteg {

    fun utførSteg(behandlingId: Long) {
        throw Feil(message = "Implementasjon mangler, er i default method implementasjon for $behandlingId")
    }

    fun gjenopptaSteg(behandlingId: Long) {
        throw Feil(message = "Implementasjon mangler, er i default method implementasjon for $behandlingId")
    }

    fun getBehandlingssteg(): BehandlingSteg
}
