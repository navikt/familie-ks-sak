package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.EndreBehandlendeEnhetDto
import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.api.mapper.BehandlingMapper
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.oppgave.OpprettOppgaveTask
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.søknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class BehandlingService(
    private val personidentService: PersonidentService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val vedtakService: VedtakService,
    private val loggService: LoggService,
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val taskRepository: TaskRepository,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService
) {

    @Transactional
    fun opprettBehandling(opprettBehandlingRequest: OpprettBehandlingDto): Behandling {
        val aktør = personidentService.hentAktør(opprettBehandlingRequest.søkersIdent)
        val fagsak = fagsakRepository.finnFagsakForAktør(aktør)
            ?: throw FunksjonellFeil(
                melding = "Kan ikke lage behandling på person uten tilknyttet fagsak."
            )

        val aktivBehandling = behandlingRepository.findByFagsakAndAktiv(fagsak.id)
        val sisteVedtattBehandling = hentSisteBehandlingSomErVedtatt(fagsak.id)

        // Kan ikke opprette en behandling når det allerede finnes en behandling som ikke er avsluttet
        if (aktivBehandling != null && aktivBehandling.status != BehandlingStatus.AVSLUTTET) {
            throw FunksjonellFeil(
                melding = "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt."
            )
        }

        val behandlingKategori = bestemKategoriVedOpprettelse(
            overstyrtKategori = opprettBehandlingRequest.kategori,
            behandlingType = opprettBehandlingRequest.behandlingType,
            behandlingÅrsak = opprettBehandlingRequest.behandlingÅrsak,
            kategoriFraLøpendeBehandling = BehandlingKategori.NASJONAL // TODO EØS implementeres etter vilkårsvurdering
        )
        val behandling = Behandling(
            fagsak = fagsak,
            type = opprettBehandlingRequest.behandlingType,
            opprettetÅrsak = opprettBehandlingRequest.behandlingÅrsak,
            kategori = behandlingKategori,
            søknadMottattDato = opprettBehandlingRequest.søknadMottattDato?.atStartOfDay()
        ).initBehandlingStegTilstand() // oppretter behandling med initielt steg Registrer Persongrunnlag

        behandling.validerBehandlingstype(sisteVedtattBehandling)
        val lagretBehandling = lagreNyOgDeaktiverGammelBehandling(
            nyBehandling = behandling,
            aktivBehandling = aktivBehandling,
            sisteVedtattBehandling = sisteVedtattBehandling
        )
        vedtakService.opprettOgInitierNyttVedtakForBehandling(lagretBehandling) // initierer vedtak
        loggService.opprettBehandlingLogg(lagretBehandling) // lag historikkinnslag
        // Oppretter BehandleSak oppgave via task. Ruller tasken tilbake, hvis behandling opprettelse feiler
        if (lagretBehandling.opprettBehandleSakOppgave()) {
            taskRepository.save(
                OpprettOppgaveTask.opprettTask(
                    behandlingId = lagretBehandling.id,
                    oppgavetype = Oppgavetype.BehandleSak,
                    fristForFerdigstillelse = LocalDate.now(),
                    tilordnetRessurs = opprettBehandlingRequest.saksbehandlerIdent
                )
            )
        }
        return lagretBehandling
    }

    private fun lagreNyOgDeaktiverGammelBehandling(
        nyBehandling: Behandling,
        aktivBehandling: Behandling?,
        sisteVedtattBehandling: Behandling?
    ): Behandling {
        aktivBehandling?.let { behandlingRepository.saveAndFlush(aktivBehandling.also { it.aktiv = false }) }
        return lagreEllerOppdater(nyBehandling).also {
            arbeidsfordelingService.fastsettBehandledeEnhet(
                it,
                sisteVedtattBehandling
            )
        }
    }

    private fun lagreEllerOppdater(behandling: Behandling): Behandling {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter behandling $behandling")
        return behandlingRepository.save(behandling)
    }

    fun hentSisteBehandlingSomErVedtatt(fagsakId: Long): Behandling? {
        return behandlingRepository.finnBehandlinger(fagsakId)
            .filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.opprettetTidspunkt }
    }

    fun hentBehandling(behandlingId: Long): Behandling = behandlingRepository.hentBehandling(behandlingId)

    fun lagBehandlingRespons(behandlingId: Long): BehandlingResponsDto {
        val behandling = hentBehandling(behandlingId)
        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId)
        val personer =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlag(behandlingId)?.personer?.toList()
        val søknadsgrunnlag = søknadGrunnlagService.finnAktiv(behandlingId)?.tilSøknadDto()
        val personResultater =
            vilkårsvurderingService.hentAktivVilkårsvurdering(behandlingId)?.personResultater?.toList()

        return BehandlingMapper.lagBehandlingRespons(
            behandling,
            arbeidsfordelingPåBehandling,
            søknadsgrunnlag,
            personer,
            personResultater
        )
    }

    fun oppdaterBehandlendeEnhet(behandlingId: Long, endreBehandlendeEnhet: EndreBehandlendeEnhetDto) =
        arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(hentBehandling(behandlingId), endreBehandlendeEnhet)

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
