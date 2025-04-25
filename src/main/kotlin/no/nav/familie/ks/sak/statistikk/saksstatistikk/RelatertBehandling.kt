package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.EksternBehandlingRelasjon

data class RelatertBehandling(
    val id: String,
    val fagsystem: Fagsystem,
) {
    enum class Fagsystem {
        KS,
        KLAGE,
        TILBAKEKREVING,
    }

    companion object Factory {
        fun fraKontantstøttebehandling(kontantstøttebehandling: Behandling) =
            RelatertBehandling(
                id = kontantstøttebehandling.id.toString(),
                fagsystem = Fagsystem.KS,
            )

        fun fraEksternBehandlingRelasjon(eksternBehandlingRelasjon: EksternBehandlingRelasjon): RelatertBehandling =
            RelatertBehandling(
                id = eksternBehandlingRelasjon.eksternBehandlingId,
                fagsystem =
                    when (eksternBehandlingRelasjon.eksternBehandlingFagsystem) {
                        EksternBehandlingRelasjon.Fagsystem.KLAGE -> Fagsystem.KLAGE
                        EksternBehandlingRelasjon.Fagsystem.TILBAKEKREVING -> Fagsystem.TILBAKEKREVING
                    },
            )
    }
}
