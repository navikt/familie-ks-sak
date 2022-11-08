package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.api.dto.BehandlingPåVentResponsDto
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Properties

@Service
class SakStatistikkService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val taskService: TaskService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val arbeidsfordelingService: ArbeidsfordelingService
) {

    fun opprettSendingAvBehandlingensTilstand(behandlingId: Long, behandlingSteg: BehandlingSteg) {
        val behandlingStegTilstand =
            behandlingRepository.hentBehandling(behandlingId).behandlingStegTilstand.singleOrNull { it.behandlingSteg == behandlingSteg }
        val hendelsesbeskrivelse = "Ny behandlingsstegstilstand " +
            "${behandlingStegTilstand?.behandlingSteg}:${behandlingStegTilstand?.behandlingStegStatus} " +
            "for behandling $behandlingId"

        val tilstand = hentBehandlingensTilstand(behandlingId)
        opprettProsessTask(behandlingId, tilstand, hendelsesbeskrivelse, SendBehandlinghendelseTilDvhTask.TASK_TYPE)
    }

    private fun opprettProsessTask(
        behandlingId: Long,
        behandlingTilstand: BehandlingStatistikkDto,
        hendelsesbeskrivelse: String,
        type: String
    ) {
        val task = Task(
            type = type,
            payload = objectMapper.writeValueAsString(behandlingTilstand),
            Properties().apply {
                setProperty("behandlingId", behandlingId.toString())
                setProperty("beskrivelse", hendelsesbeskrivelse)
            }
        )
        taskService.save(task)
    }

    fun hentBehandlingensTilstand(behandlingId: Long): BehandlingStatistikkDto {
        val behandling = behandlingRepository.hentBehandling(behandlingId)
        val ansvarligEnhet = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId).behandlendeEnhetId
        val totrinnskontroll = totrinnskontrollService.finnAktivForBehandling(behandlingId)
        val behandlingPåVent =
            behandling.behandlingStegTilstand.singleOrNull { it.behandlingStegStatus == BehandlingStegStatus.VENTER }
                ?.let { BehandlingPåVentResponsDto(it.frist!!, it.årsak!!) }

        return BehandlingStatistikkDto(
            saksnummer = behandling.fagsak.id,
            behandlingID = behandling.id,
            mottattTid = behandling.søknadMottattDato?.tilOffset(),
            behandlingType = behandling.type,
            behandlingStatus = behandling.status,
            behandlingsResultat = behandling.resultat,
            ansvarligEnhet = ansvarligEnhet,
            ansvarligBeslutter = totrinnskontroll?.beslutterId,
            ansvarligSaksbehandler = totrinnskontroll?.let { it.saksbehandlerId } ?: behandling.endretAv,
            behandlingErManueltOpprettet = true, // TODO er alltid det frem til vi kobler på søknadsdialogen
            funksjoneltTidspunkt = behandling.endretTidspunkt.tilOffset(),
            sattPaaVent = behandlingPåVent?.årsak?.name?.let {
                SattPåVent(
                    frist = OffsetDateTime.of(
                        behandlingPåVent.frist,
                        java.time.LocalTime.now(),
                        ZoneOffset.UTC
                    ),
                    aarsak = it
                )
            },
            behandlingOpprettetÅrsak = behandling.opprettetÅrsak
        )
    }

    fun sendAlleBehandlingerTilDVH() {
        fagsakRepository.findAll().forEach { fagsak ->
            behandlingRepository.finnBehandlinger(fagsakId = fagsak.id).forEach { behandling ->
                val tilstand = hentBehandlingensTilstand(behandling.id)
                opprettProsessTask(
                    tilstand.behandlingID,
                    tilstand,
                    "Sender siste tilstand på nytt",
                    SendSisteBehandlingstilstandTilDvhTask.TASK_TYPE
                )
            }
        }
    }

    fun LocalDateTime.tilOffset(): OffsetDateTime {
        return OffsetDateTime.of(
            this.toLocalDate(),
            java.time.LocalTime.now(),
            ZoneOffset.UTC
        )
    }
}
