package no.nav.familie.ks.sak.kjerne.behandling.domene

import java.util.UUID

data class NyEksternBehandlingRelasjon(
    val eksternBehandlingId: String,
    val eksternBehandlingFagsystem: EksternBehandlingRelasjon.Fagsystem,
) {
    companion object Factory {
        fun opprettForKlagebehandling(
            klagebehandlingId: UUID,
        ): NyEksternBehandlingRelasjon =
            NyEksternBehandlingRelasjon(
                eksternBehandlingId = klagebehandlingId.toString(),
                eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
            )
    }
}
