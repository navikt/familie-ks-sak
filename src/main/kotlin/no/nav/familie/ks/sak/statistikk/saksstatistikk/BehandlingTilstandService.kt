package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStegTilstand
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Properties

@Service
class BehandlingTilstandService(
    private val behandlingRepository: BehandlingRepository,
    private val taskService: TaskService,
    private val arbeidsfordelingService: ArbeidsfordelingService
) {

    fun opprettSendingAvBehandlingensTilstand(behandlingId: Long, stegTilstand: BehandlingStegTilstand) {
        val hendelsesbeskrivelse = "Ny behandlingsstegstilstand " +
            "${stegTilstand.behandlingSteg}:${stegTilstand.behandlingStegStatus} " +
            "for behandling $behandlingId"

        val tilstand = hentBehandlingensTilstand(behandlingId)
        opprettProsessTask(behandlingId, tilstand, hendelsesbeskrivelse)
    }

    private fun opprettProsessTask(
        behandlingId: Long,
        behandlingTilstand: Behandlingtilstand,
        hendelsesbeskrivelse: String
    ) {
        val task = Task(
            type = SendBehandlinghendelseTilDvhTask.TASK_TYPE,
            payload = objectMapper.writeValueAsString(behandlingTilstand),
            Properties().apply {
                setProperty("behandlingId", behandlingId.toString())
                setProperty("beskrivelse", hendelsesbeskrivelse)
            }
        )
        taskService.save(task)
    }

    fun hentBehandlingensTilstand(behandlingId: Long): Behandlingtilstand {
        val behandling: Behandling = behandlingRepository.hentBehandling(behandlingId)
        val ansvarligEnhet = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId).behandlendeEnhetId

        return Behandlingtilstand(
            saksnummer = behandling.fagsak.id,
            behandlingID = behandling.id,
            behandlingType = behandling.type,
            behandlingStatus = behandling.status,
            behandlingResultat = behandling.resultat,
            ansvarligEnhet = ansvarligEnhet,
            ansvarligBeslutter = null, // TODO finn beslutter når vi har 2-trinnskontroll
            ansvarligSaksbehandler = behandling.endretAv,
            behandlingErManueltOpprettet = true, // TODO hvordan utlede?
            funksjoneltTidspunkt = OffsetDateTime.now(ZoneOffset.UTC),
            sattPaaVent = null, // TODO legg til når klart
            behandlingOpprettetÅrsak = behandling.opprettetÅrsak
        )
    }
}
