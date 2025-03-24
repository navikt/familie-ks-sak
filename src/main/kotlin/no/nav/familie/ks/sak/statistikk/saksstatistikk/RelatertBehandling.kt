package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import java.time.LocalDateTime

data class RelatertBehandling(
    val id: String,
    val vedtattTidspunkt: LocalDateTime,
    val fagsystem: Fagsystem,
) {
    enum class Fagsystem {
        KS,
        KLAGE,
    }

    companion object Fabrikk {
        fun fraKontantstøttebehandling(kontantstøttebehandling: Behandling) =
            RelatertBehandling(
                id = kontantstøttebehandling.id.toString(),
                vedtattTidspunkt = kontantstøttebehandling.aktivertTidspunkt,
                fagsystem = Fagsystem.KS,
            )

        fun fraKlagebehandling(klagebehandling: KlagebehandlingDto): RelatertBehandling {
            val vedtaksdato = klagebehandling.vedtaksdato
            if (vedtaksdato == null) {
                throw Feil("Forventer vedtaksdato for klagebehandling ${klagebehandling.id}")
            }
            return RelatertBehandling(
                id = klagebehandling.id.toString(),
                vedtattTidspunkt = vedtaksdato,
                fagsystem = Fagsystem.KLAGE,
            )
        }
    }
}
