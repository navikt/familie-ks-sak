package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.EndreBehandlendeEnhetDto
import no.nav.familie.ks.sak.api.mapper.BehandlingMapper
import no.nav.familie.ks.sak.api.mapper.BehandlingMapper.lagPersonRespons
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSettPåVentÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.søknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.StatsborgerskapService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period

@Service
class BehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val statsborgerskapService: StatsborgerskapService,
    private val loggService: LoggService,
    private val stegService: StegService,
    private val oppgaveService: OppgaveService
) {

    fun hentBehandling(behandlingId: Long): Behandling = behandlingRepository.hentBehandling(behandlingId)

    fun hentSisteBehandlingSomErVedtatt(fagsakId: Long): Behandling? = behandlingRepository.finnBehandlinger(fagsakId)
        .filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
        .maxByOrNull { it.opprettetTidspunkt }

    fun lagreEllerOppdater(behandling: Behandling): Behandling {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter behandling $behandling")
        return behandlingRepository.save(behandling)
    }

    fun lagBehandlingRespons(behandlingId: Long): BehandlingResponsDto {
        val behandling = hentBehandling(behandlingId)
        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId)

        val personer =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlag(behandlingId)?.personer?.toList()
                ?: emptyList()

        val landKodeOgLandNavn = personer.flatMap { it.statsborgerskap }.toSet()
            .associate { it.landkode to statsborgerskapService.hentLand(it.landkode) }

        val personResponserDtoer = personer.map { lagPersonRespons(it, landKodeOgLandNavn) }

        val søknadsgrunnlag = søknadGrunnlagService.finnAktiv(behandlingId)?.tilSøknadDto()
        val personResultater =
            vilkårsvurderingService.finnAktivVilkårsvurdering(behandlingId)?.personResultater?.toList()
        return BehandlingMapper.lagBehandlingRespons(
            behandling,
            arbeidsfordelingPåBehandling,
            søknadsgrunnlag,
            personResponserDtoer,
            personResultater
        )
    }

    fun oppdaterBehandlendeEnhet(behandlingId: Long, endreBehandlendeEnhet: EndreBehandlendeEnhetDto) =
        arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(hentBehandling(behandlingId), endreBehandlendeEnhet)

    fun oppdaterBehandlingsresultat(behandlingId: Long, nyUtledetBehandlingsresultat: Behandlingsresultat): Behandling {
        val behandling = hentBehandling(behandlingId)
        logger.info(
            "${SikkerhetContext.hentSaksbehandlerNavn()} endrer resultat på behandling $behandlingId " +
                "fra ${behandling.resultat} til $nyUtledetBehandlingsresultat"
        )
        loggService.opprettVilkårsvurderingLogg(
            behandling = behandling,
            behandlingsForrigeResultat = behandling.resultat,
            behandlingsNyResultat = nyUtledetBehandlingsresultat
        )
        return lagreEllerOppdater(behandling.copy(resultat = nyUtledetBehandlingsresultat))
    }

    fun nullstillEndringstidspunkt(behandlingId: Long) =
        lagreEllerOppdater(hentBehandling(behandlingId).copy(overstyrtEndringstidspunkt = null))

    fun settBehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: BehandlingSettPåVentÅrsak) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)
        validerBehandlingKanSettesPåVent(behandling, frist)

        stegService.SettBehandlingstegTilstandPåVent(behandling, frist, årsak)

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

    fun validerBehandlingKanSettesPåVent(behandling: Behandling, frist: LocalDate) {
        when {
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
                throw Feil(
                    "Behandling ${behandling.id} er ikke aktiv og kan ikke settes på vent."
                )
            }
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
