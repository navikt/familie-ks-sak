package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.eksterne.kontrakter.saksstatistikk.AktørDVH
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.api.dto.BehandlingPåVentResponsDto
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Properties
import java.util.UUID

@Service
class SakStatistikkService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val taskService: TaskService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val fagsakService: FagsakService,
    private val personopplysningService: PersonOpplysningerService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService
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
            ansvarligSaksbehandler = totrinnskontroll?.saksbehandlerId ?: behandling.endretAv,
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

    fun mapTilSakDvh(fagsakId: Long): SakStatistikkDto {
        val fagsak = fagsakService.hentFagsak(fagsakId)
        val aktivBehandling = behandlingRepository.findByFagsakAndAktiv(fagsakId = fagsak.id)

        val aktørDVHer = if (aktivBehandling != null) {
            personopplysningGrunnlagService
                .finnAktivPersonopplysningGrunnlag(behandlingId = aktivBehandling.id)
                ?.personer?.map {
                    AktørDVH(
                        it.aktør.aktørId.toLong(),
                        it.type.name
                    )
                } ?: emptyList()
        } else {
            listOf(AktørDVH(fagsak.aktør.aktørId.toLong(), PersonType.SØKER.name))
        }

        return SakStatistikkDto(
            funksjonellTid = ZonedDateTime.now(),
            tekniskTid = ZonedDateTime.now(),
            opprettetDato = LocalDate.now(),
            funksjonellId = UUID.randomUUID().toString(),
            sakId = fagsakId.toString(),
            aktorId = fagsak.aktør.aktørId.toLong(),
            aktorer = aktørDVHer,
            sakStatus = fagsak.status.name,
            avsender = "familie-ks-sak",
            bostedsland = hentLandkode(fagsak.aktør)
        )
    }

    private fun hentLandkode(aktør: Aktør): String {
        val personInfo = personopplysningService.hentPersoninfoEnkel(aktør)

        return if (personInfo.bostedsadresser.isNotEmpty()) {
            "NO"
        } else {
            personopplysningService.hentLandkodeAlpha2UtenlandskBostedsadresse(
                aktør
            )
        }
    }
}
