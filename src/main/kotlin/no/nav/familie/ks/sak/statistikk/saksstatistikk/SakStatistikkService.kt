package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.eksterne.kontrakter.saksstatistikk.AktørDVH
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.api.dto.BehandlingPåVentDto
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
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
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Properties
import java.util.UUID

@Service
class SakStatistikkService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val taskService: TaskRepositoryWrapper,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val fagsakService: FagsakService,
    private val personopplysningService: PersonopplysningerService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val relatertBehandlingUtleder: RelatertBehandlingUtleder,
) {
    fun opprettSendingAvBehandlingensTilstand(
        behandlingId: Long,
        behandlingSteg: BehandlingSteg,
    ) {
        val behandlingStegTilstand =
            behandlingRepository.hentBehandling(behandlingId).behandlingStegTilstand.singleOrNull { it.behandlingSteg == behandlingSteg }
        val hendelsesbeskrivelse =
            "Ny behandlingsstegstilstand " +
                "${behandlingStegTilstand?.behandlingSteg}:${behandlingStegTilstand?.behandlingStegStatus} " +
                "for behandling $behandlingId"

        val tilstand = hentBehandlingensTilstandV2(behandlingId, false)
        opprettProsessTask(behandlingId, tilstand, hendelsesbeskrivelse, SendBehandlinghendelseTilDvhV2Task.TASK_TYPE)
    }

    fun sendMeldingOmEndringAvBehandlingkategori(
        behandlingId: Long,
        nyBehandlingKategori: BehandlingKategori,
    ) {
        val hendelsesbeskrivelse = "Endrer behandlingskategori til $nyBehandlingKategori for behandling $behandlingId"

        val tilstand = hentBehandlingensTilstandV2(behandlingId, false)
        opprettProsessTask(behandlingId, tilstand, hendelsesbeskrivelse, SendBehandlinghendelseTilDvhV2Task.TASK_TYPE)
    }

    private fun opprettProsessTask(
        behandlingId: Long,
        behandlingTilstand: BehandlingStatistikkV2Dto,
        hendelsesbeskrivelse: String,
        type: String,
    ) {
        val task =
            Task(
                type = type,
                payload = objectMapper.writeValueAsString(behandlingTilstand),
                Properties().apply {
                    setProperty("behandlingId", behandlingId.toString())
                    setProperty("beskrivelse", hendelsesbeskrivelse)
                },
            )
        taskService.save(task)
    }

    fun sendMeldingOmManuellEndringAvBehandlendeEnhet(
        behandlingId: Long,
    ) {
        val hendelsesbeskrivelse =
            "Endrer behandlende enhet manuelt for behandling $behandlingId"

        val tilstand = hentBehandlingensTilstandV2(behandlingId, false)
        opprettProsessTask(behandlingId, tilstand, hendelsesbeskrivelse, SendBehandlinghendelseTilDvhV2Task.TASK_TYPE)
    }

    fun hentBehandlingensTilstandV2(
        behandlingId: Long,
        brukEndretTidspunktSomFunksjonellTidspunkt: Boolean,
    ): BehandlingStatistikkV2Dto {
        val behandling = behandlingRepository.hentBehandling(behandlingId)
        val ansvarligEnhet = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId).behandlendeEnhetId
        val totrinnskontroll = totrinnskontrollService.finnAktivForBehandling(behandlingId)
        val behandlingPåVent =
            behandling.behandlingStegTilstand
                .singleOrNull { it.behandlingStegStatus == BehandlingStegStatus.VENTER }
                ?.let { BehandlingPåVentDto(it.frist!!, it.årsak!!) }

        val mottattTid = behandling.søknadMottattDato ?: behandling.opprettetTidspunkt

        val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(behandling)

        return BehandlingStatistikkV2Dto(
            saksnummer = behandling.fagsak.id,
            behandlingID = behandling.id,
            mottattTid = mottattTid.atZone(TIMEZONE),
            registrertTid = behandling.opprettetTidspunkt.atZone(TIMEZONE),
            behandlingType = behandling.type,
            utenlandstilsnitt = behandling.kategori.name,
            behandlingStatus = behandling.status,
            behandlingsResultat = behandling.resultat,
            ansvarligEnhet = ansvarligEnhet,
            ansvarligBeslutter = totrinnskontroll?.beslutterId,
            ansvarligSaksbehandler = totrinnskontroll?.saksbehandlerId ?: behandling.endretAv,
            // TODO er alltid det frem til vi kobler på søknadsdialogen
            behandlingErManueltOpprettet = true,
            funksjoneltTidspunkt = if (brukEndretTidspunktSomFunksjonellTidspunkt) behandling.endretTidspunkt.atZone(TIMEZONE) else ZonedDateTime.now(),
            automatiskBehandlet = behandling.skalBehandlesAutomatisk(),
            sattPaaVent =
                behandlingPåVent?.årsak?.name?.let {
                    SattPåVent(
                        frist =
                            ZonedDateTime.of(
                                behandlingPåVent.frist,
                                java.time.LocalTime.now(),
                                TIMEZONE,
                            ),
                        aarsak = it,
                    )
                },
            behandlingOpprettetÅrsak = behandling.opprettetÅrsak,
            relatertBehandlingId = relatertBehandling?.id,
            relatertBehandlingFagsystem = relatertBehandling?.fagsystem?.name,
        )
    }

    fun sendAlleBehandlingerTilDVH() {
        fagsakRepository.findAll().forEach { fagsak ->
            behandlingRepository.finnBehandlinger(fagsakId = fagsak.id).forEach { behandling ->
                val tilstand = hentBehandlingensTilstandV2(behandling.id, true)
                opprettProsessTask(
                    tilstand.behandlingID,
                    tilstand,
                    "Sender siste tilstand på nytt",
                    SendSisteBehandlingstilstandTilDvhTask.TASK_TYPE,
                )
            }
        }
    }

    fun mapTilSakDvh(fagsakId: Long): SakStatistikkDto {
        val fagsak = fagsakService.hentFagsak(fagsakId)
        val aktivBehandling = behandlingRepository.findByFagsakAndAktiv(fagsakId = fagsak.id)

        val aktørDVHer =
            if (aktivBehandling != null) {
                personopplysningGrunnlagService
                    .finnAktivPersonopplysningGrunnlag(behandlingId = aktivBehandling.id)
                    ?.personer
                    ?.map {
                        AktørDVH(
                            it.aktør.aktørId.toLong(),
                            it.type.name,
                        )
                    } ?: emptyList()
            } else {
                listOf(AktørDVH(fagsak.aktør.aktørId.toLong(), PersonType.SØKER.name))
            }

        val now = ZonedDateTime.now()
        return SakStatistikkDto(
            funksjonellTid = now,
            tekniskTid = now,
            opprettetDato = LocalDate.now(),
            funksjonellId = UUID.randomUUID().toString(),
            sakId = fagsakId.toString(),
            aktorId = fagsak.aktør.aktørId.toLong(),
            aktorer = aktørDVHer,
            sakStatus = fagsak.status.name,
            avsender = "familie-ks-sak",
            bostedsland = hentLandkode(fagsak.aktør),
        )
    }

    private fun hentLandkode(aktør: Aktør): String {
        val personInfo = personopplysningService.hentPersoninfoEnkel(aktør)

        return if (personInfo.bostedsadresser.isNotEmpty()) {
            "NO"
        } else {
            personopplysningService.hentLandkodeAlpha2UtenlandskBostedsadresse(
                aktør,
            )
        }
    }

    companion object {
        val TIMEZONE: ZoneId = ZoneId.systemDefault()
    }
}
