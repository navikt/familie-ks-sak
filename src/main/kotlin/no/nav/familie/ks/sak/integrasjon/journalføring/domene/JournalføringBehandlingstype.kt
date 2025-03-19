package no.nav.familie.ks.sak.integrasjon.journalføring.domene

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType

enum class JournalføringBehandlingstype {
    FØRSTEGANGSBEHANDLING,
    REVURDERING,
    TEKNISK_ENDRING,
    KLAGE,
    TILBAKEKREVING,
    ;

    fun skalBehandlesIEksternApplikasjon() = this == KLAGE || this == TILBAKEKREVING

    fun tilBehandingType(): BehandlingType =
        when (this) {
            FØRSTEGANGSBEHANDLING -> BehandlingType.FØRSTEGANGSBEHANDLING
            REVURDERING -> BehandlingType.REVURDERING
            TEKNISK_ENDRING -> BehandlingType.TEKNISK_ENDRING
            KLAGE -> throw Feil("Klage finnes ikke i ${BehandlingType::class.simpleName}. Behandles i ekstern applikasjon.")
            TILBAKEKREVING -> throw Feil("Tilbakekreving finnes ikke i ${BehandlingType::class.simpleName}. Behandles i ekstern applikasjon.")
        }
}