package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.VenteÅrsak
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Period

@Service
class SettBehandlingPåVentService(
    private val behandlingRepository: BehandlingRepository,
    private val stegService: StegService,
    private val loggService: LoggService,
    private val oppgaveService: OppgaveService
) {

    @Transactional
    fun settBehandlingPåVent(behandlingId: Long, frist: LocalDate) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)

        stegService.settBehandlingstegPåVent(behandling, frist)

        loggService.opprettSettPåVentLogg(behandling, VenteÅrsak.AVVENTER_DOKUMENTASJON.visningsnavn)

        oppgaveService.forlengFristÅpneOppgaverPåBehandling(
            behandlingId = behandling.id,
            forlengelse = Period.between(LocalDate.now(), frist)
        )
    }

    @Transactional
    fun oppdaterFrist(
        behandlingId: Long,
        frist: LocalDate
    ) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)

        val gammelFrist =
            stegService.oppdaterBehandlingstegFrist(behandling, frist)

        loggService.opprettOppdaterVentingLogg(
            behandling = behandling,
            endretFrist = if (frist != gammelFrist) frist else null
        )

        oppgaveService.forlengFristÅpneOppgaverPåBehandling(
            behandlingId = behandlingId,
            forlengelse = Period.between(gammelFrist, frist)
        )
    }

    @Transactional
    fun gjenopptaBehandlingPåVent(behandlingId: Long) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)

        stegService.gjenopptaBehandlingsteg(behandling)

        loggService.opprettBehandlingGjenopptattLogg(behandling)

        oppgaveService.settFristÅpneOppgaverPåBehandlingTil(
            behandlingId = behandlingId,
            nyFrist = LocalDate.now().plusDays(1)
        )
    }
}
