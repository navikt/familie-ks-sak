package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.ks.sak.api.dto.BehandlingStegDto
import no.nav.familie.ks.sak.api.dto.RegistrerSøknadDto
import no.nav.familie.ks.sak.api.dto.tilSøknadGrunnlag
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.søknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RegistrereSøknadSteg(
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val loggService: LoggService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val behandlingService: BehandlingService,
    private val vilkårsvurderingService: VilkårsvurderingService
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.REGISTRERE_SØKNAD

    override fun utførSteg(behandlingId: Long, behandlingStegDto: BehandlingStegDto) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
        val registrerSøknadDto = behandlingStegDto as RegistrerSøknadDto

        // Sjekk om det allerede finnes en registrert søknad tilknyttet behandlingen
        val aktivSøknadGrunnlagFinnes = søknadGrunnlagService.finnAktiv(behandlingId) != null

        // Logg at vi registrerer ny søknad med info om det fantes en søknad fra før
        loggService.opprettRegistrertSøknadLogg(behandlingId, aktivSøknadGrunnlagFinnes)

        // Lagre ny søknad og deaktiver gammel
        val søknadGrunnlag =
            søknadGrunnlagService.lagreOgDeaktiverGammel(registrerSøknadDto.søknad.tilSøknadGrunnlag(behandlingId))

        // Oppdatere personopplysningsgrunnlag dersom det er lagt til barn som ikke fantes fra før
        val behandling = behandlingService.hentBehandling(behandlingId)
        personopplysningGrunnlagService.oppdaterPersonopplysningGrunnlag(behandling, søknadGrunnlag.tilSøknadDto())

        val forrigeBehandlingSomErVedtatt = behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)

        vilkårsvurderingService.opprettVilkårsvurdering(behandling, forrigeBehandlingSomErVedtatt)

        secureLogger.info("Data mottatt ${søknadGrunnlag.søknad}")
    }

    override fun gjenopptaSteg(behandlingId: Long) {
        logger.info("Gjenopptar steg ${getBehandlingssteg().name} for behandling $behandlingId")
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RegistrereSøknadSteg::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
