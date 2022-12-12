package no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad

import no.nav.familie.ks.sak.api.dto.BehandlingStegDto
import no.nav.familie.ks.sak.api.dto.RegistrerSøknadDto
import no.nav.familie.ks.sak.api.dto.tilSøknadGrunnlag
import no.nav.familie.ks.sak.api.dto.writeValueAsString
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.common.EnvService
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.IBehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegistrereSøknadSteg(
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val infotrygdReplikaClient: InfotrygdReplikaClient,
    private val envService: EnvService,
    private val loggService: LoggService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val vedtaksperiodeRepository: VedtaksperiodeRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.REGISTRERE_SØKNAD

    @Transactional
    override fun utførSteg(behandlingId: Long, behandlingStegDto: BehandlingStegDto) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
        val registrerSøknadDto = behandlingStegDto as RegistrerSøknadDto

        logger.info("valider steg ${getBehandlingssteg().name}")
        validerRegistrerSøknadSteg(registrerSøknadDto)

        // Sjekk om det allerede finnes en registrert søknad tilknyttet behandlingen
        val aktivSøknadGrunnlag = søknadGrunnlagService.finnAktiv(behandlingId)
        val aktivSøknadGrunnlagFinnes = aktivSøknadGrunnlag != null

        if (aktivSøknadGrunnlagFinnes && aktivSøknadGrunnlag!!.søknad == registrerSøknadDto.søknad.writeValueAsString()) {
            logger.info("Det finnes allerede en identisk søknad, ingen endringer blir utført.")
            return
        }

        // Logg at vi registrerer ny søknad med info om det fantes en søknad fra før
        loggService.opprettRegistrertSøknadLogg(behandlingId, aktivSøknadGrunnlagFinnes)

        // Lagre ny søknad og deaktiver gammel
        val søknadGrunnlag =
            søknadGrunnlagService.lagreOgDeaktiverGammel(registrerSøknadDto.søknad.tilSøknadGrunnlag(behandlingId))

        // Oppdatere personopplysningsgrunnlag dersom det er lagt til barn som ikke fantes fra før
        val behandling = behandlingService.hentBehandling(behandlingId)

        val forrigeBehandlingSomErVedtatt = behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)

        personopplysningGrunnlagService.oppdaterPersonopplysningGrunnlag(
            behandling,
            forrigeBehandlingSomErVedtatt,
            søknadGrunnlag.tilSøknadDto()
        )

        vilkårsvurderingService.opprettVilkårsvurdering(behandling, forrigeBehandlingSomErVedtatt)

        if (forrigeBehandlingSomErVedtatt != null) {
            // TODO sette vilkårene til barna som er fjernet fra søknad til slik de var etter VilkårsvurderingService.opprettVilkårsvurdering()
        }

        // Vi sletter vedtaksperioder og tilkjentytelse hvis de tidligere har blitt generert
        val vedtak = vedtakService.hentAktivVedtakForBehandling(behandlingId)

        vedtaksperiodeRepository.slettVedtaksperioderForVedtak(vedtak)
        tilkjentYtelseRepository.slettTilkjentYtelseForBehandling(behandling)

        secureLogger.info("Data mottatt ${søknadGrunnlag.søknad}")
    }

    fun validerRegistrerSøknadSteg(registrerSøknadDto: RegistrerSøknadDto) {
        // Valider at det ikke finnes løpende kontantstøtte for barna i infotrygd, gjøres kun i preprod siden replika mangler i prod
        if (envService.erPreprod() && infotrygdReplikaClient.harKontantstøtteIInfotrygd(registrerSøknadDto.søknad.barnaMedOpplysninger)) {
            throw FunksjonellFeil(
                melding = "Kan ikke fortsette. Ett eller flere av barna har løpende kontantstøtte i infotrygd."
            )
        }
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(RegistrereSøknadSteg::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
