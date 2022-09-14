package no.nav.familie.ks.sak.kjerne.logg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.RolleConfig
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.Arbeidsfordelingsenhet
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.logg.domene.Logg
import no.nav.familie.ks.sak.kjerne.logg.domene.LoggRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class LoggService(
    private val loggRepository: LoggRepository,
    private val rolleConfig: RolleConfig
) {

    private val metrikkPerLoggType: Map<LoggType, Counter> = LoggType.values().associateWith {
        Metrics.counter(
            "behandling.logg",
            "type",
            it.name,
            "beskrivelse",
            it.visningsnavn
        )
    }
    fun opprettAutovedtakTilManuellBehandling(behandling: Behandling, tekst: String) {
        lagre(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.AUTOVEDTAK_TIL_MANUELL_BEHANDLING,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER
                ),
                tekst = tekst
            )
        )
    }

    fun lagre(logg: Logg): Logg {
        metrikkPerLoggType[logg.type]?.increment()

        return loggRepository.save(logg)
    }

    fun hentLoggForBehandling(behandlingId: Long): List<Logg> {
        return loggRepository.hentLoggForBehandling(behandlingId)
    }

    companion object {

        private fun tilBehandlingstema(kategori: BehandlingKategori): String {
            return kategori.visningsnavn
        }
    }
}
