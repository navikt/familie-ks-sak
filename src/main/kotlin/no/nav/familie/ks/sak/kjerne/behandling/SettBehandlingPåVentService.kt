package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
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
    fun settBehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: VenteÅrsak) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)
        validerBehandlingKanSettesPåVent(behandling, frist)

        stegService.settBehandlingstegPåVent(behandling, frist, årsak)

        oppgaveService.forlengFristÅpneOppgaverPåBehandling(
            behandlingId = behandling.id,
            forlengelse = Period.between(LocalDate.now(), frist)
        )
    }

    @Transactional
    fun oppdaterFristOgÅrsak(behandlingId: Long, frist: LocalDate, årsak: VenteÅrsak) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)

        val gammelFrist = stegService.oppdaterBehandlingstegFristOgÅrsak(behandling, frist, årsak)

        oppgaveService.forlengFristÅpneOppgaverPåBehandling(
            behandlingId = behandlingId,
            forlengelse = Period.between(gammelFrist, frist)
        )
    }

    @Transactional
    fun gjenopptaBehandlingPåVent(behandlingId: Long) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)
        val behandlingStegTilstand = behandling.behandlingStegTilstand.single { it.behandlingSteg == behandling.steg }

        if (behandlingStegTilstand.behandlingStegStatus != BehandlingStegStatus.VENTER) {
            throw FunksjonellFeil("Behandlingen er ikke på vent og kan derfor ikke gjenopptas.")
        }

        stegService.utførSteg(behandling.id, behandling.steg)

        loggService.opprettBehandlingGjenopptattLogg(behandling)

        oppgaveService.settFristÅpneOppgaverPåBehandlingTil(
            behandlingId = behandlingId,
            nyFrist = LocalDate.now().plusDays(1)
        )
    }

    private fun validerBehandlingKanSettesPåVent(behandling: Behandling, frist: LocalDate) {
        when {
            behandling.behandlingStegTilstand.any { it.behandlingStegStatus == BehandlingStegStatus.VENTER } -> {
                throw FunksjonellFeil(
                    melding = "Behandlingen er allerede satt på vent."
                )
            }

            frist.isBefore(LocalDate.now()) -> {
                throw FunksjonellFeil(
                    melding = "Frist for å vente på behandling ${behandling.id} er satt før dagens dato.",
                    frontendFeilmelding = "Fristen er satt før dagens dato."
                )
            }

            behandling.status == BehandlingStatus.AVSLUTTET -> {
                throw FunksjonellFeil(
                    melding = "Behandling ${behandling.id} er avsluttet og kan ikke settes på vent.",
                    frontendFeilmelding = "Kan ikke sette en avsluttet behandling på vent."
                )
            }

            !behandling.aktiv -> {
                throw FunksjonellFeil(
                    "Behandling ${behandling.id} er ikke aktiv og kan ikke settes på vent."
                )
            }
        }
    }
}
