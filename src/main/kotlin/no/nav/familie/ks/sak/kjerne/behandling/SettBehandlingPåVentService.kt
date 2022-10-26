package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSettPåVentÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period

@Service
class SettBehandlingPåVentService(
    private val behandlingRepository: BehandlingRepository,
    private val stegService: StegService,
    private val loggService: LoggService,
    private val oppgaveService: OppgaveService
) {

    fun settBehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: BehandlingSettPåVentÅrsak) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)

        stegService.settBehandlingstegTilstandPåVent(behandling, frist, årsak)

        loggService.opprettSettPåVentLogg(behandling, årsak.visningsnavn)

        oppgaveService.forlengFristÅpneOppgaverPåBehandling(
            behandlingId = behandling.id,
            forlengelse = Period.between(LocalDate.now(), frist)
        )
    }

    fun oppdaterBehandlingPåVent(
        behandlingId: Long,
        frist: LocalDate,
        årsak: BehandlingSettPåVentÅrsak
    ) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)

        val gammelFristOgÅrsak =
            stegService.oppdaterBehandlingstegTilstandPåVent(behandling, frist, årsak)

        loggService.opprettOppdaterVentingLogg(
            behandling = behandling,
            endretFrist = if (frist != gammelFristOgÅrsak.first) frist else null,
            endretÅrsak = if (årsak != gammelFristOgÅrsak.second) årsak.visningsnavn else null
        )

        oppgaveService.forlengFristÅpneOppgaverPåBehandling(
            behandlingId = behandlingId,
            forlengelse = Period.between(gammelFristOgÅrsak.first, frist)
        )
    }

    fun gjenopptaBehandlingPåVent(behandlingId: Long) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)

        stegService.gjenopptaBehandlingstegTilstandPåVent(behandling)

        loggService.opprettBehandlingGjenopptattLogg(behandling)

        oppgaveService.settFristÅpneOppgaverPåBehandlingTil(
            behandlingId = behandlingId,
            nyFrist = LocalDate.now().plusDays(1)
        )
    }
}
