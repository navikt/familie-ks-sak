package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.EndreBehandlendeEnhetDto
import no.nav.familie.ks.sak.api.dto.PersonResponsDto
import no.nav.familie.ks.sak.api.mapper.BehandlingMapper
import no.nav.familie.ks.sak.api.mapper.BehandlingMapper.lagPersonRespons
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.common.util.storForbokstav
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.søknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.StatsborgerskapService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val statsborgerskapService: StatsborgerskapService,
    private val loggService: LoggService
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

        val personResponserDtoer = personer.map { lagPersonRespons(it) }

        oppdaterPersonResponsMedStatsborgerskapLand(personResponserDtoer)

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

    private fun oppdaterPersonResponsMedStatsborgerskapLand(personResponserDtoer: List<PersonResponsDto>) {
        personResponserDtoer.forEach { person ->
            person.registerhistorikk?.statsborgerskap?.forEach { statsborgerskap ->
                val landkode = statsborgerskap.verdi
                val land = statsborgerskapService.hentLand(landkode)

                val nyVerdi = if (land.equals("uoppgitt", true)) {
                    "$land ($landkode)"
                } else {
                    land.storForbokstav()
                }

                statsborgerskap.verdi = nyVerdi
            }
        }
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

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
