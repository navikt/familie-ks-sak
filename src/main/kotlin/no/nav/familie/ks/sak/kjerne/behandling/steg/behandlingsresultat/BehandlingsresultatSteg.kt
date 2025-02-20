package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.IBehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelService
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelValidator
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandlingsresultatSteg(
    private val behandlingService: BehandlingService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val beregningService: BeregningService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val simuleringService: SimuleringService,
    private val vedtakRepository: VedtakRepository,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val overgangsordningAndelService: OvergangsordningAndelService,
    private val unleashNextMedContextService: UnleashNextMedContextService,
    private val adopsjonService: AdopsjonService,
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.BEHANDLINGSRESULTAT

    @Transactional
    override fun utførSteg(behandlingId: Long) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
        val behandling = behandlingService.hentBehandling(behandlingId)
        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId)
        val tilkjentYtelseNåværendeBehandling = beregningService.hentTilkjentYtelseForBehandling(behandlingId)
        val endretUtbetalingMedAndeler =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId)

        val personResultaterForBarn = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId).personResultater.filter { !it.erSøkersResultater() }

        BehandlingsresultatValideringUtils.validerAtBehandlingsresultatKanUtføres(
            personopplysningGrunnlag = personopplysningGrunnlag,
            tilkjentYtelse = tilkjentYtelseNåværendeBehandling,
            endretUtbetalingMedAndeler = endretUtbetalingMedAndeler,
            personResultaterForBarn = personResultaterForBarn,
            adopsjonerIBehandling = adopsjonService.hentAlleAdopsjonerForBehandling(BehandlingId(behandlingId)),
        )

        if (behandling.erOvergangsordning()) {
            val overgangsordningAndeler = overgangsordningAndelService.hentOvergangsordningAndeler(behandlingId)
            val sisteVedtatteBehandling =
                behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)
                    ?: throw Feil("Fant ingen iverksatt behandling for fagsak ${behandling.fagsak.id}")
            val tilkjentYtelseForrigeBehandling = beregningService.hentTilkjentYtelseForBehandling(sisteVedtatteBehandling.id)

            OvergangsordningAndelValidator.validerOvergangsordningAndeler(
                overgangsordningAndeler = overgangsordningAndeler,
                andelerTilkjentYtelseNåværendeBehandling = tilkjentYtelseNåværendeBehandling.andelerTilkjentYtelse,
                andelerTilkjentYtelseForrigeBehandling = tilkjentYtelseForrigeBehandling.andelerTilkjentYtelse,
                personResultaterForBarn = personResultaterForBarn,
                barna = personopplysningGrunnlag.barna,
            )
        }

        val resultat = behandlingsresultatService.utledBehandlingsresultat(behandling.id)

        val behandlingMedOppdatertResultat = behandlingService.oppdaterBehandlingsresultat(behandlingId, resultat)

        val erLovendringOgFremtidigOpphørOgNyAndelIAugust2024 = behandlingService.erLovendringOgFremtidigOpphørOgHarFlereAndeler(behandling)

        if (behandlingMedOppdatertResultat.skalSendeVedtaksbrev(erLovendringOgFremtidigOpphørOgNyAndelIAugust2024)) {
            behandlingService.nullstillEndringstidspunkt(behandlingId)
            vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(
                vedtak =
                    vedtakRepository.findByBehandlingAndAktiv(
                        behandlingId = behandling.id,
                    ),
            )
        }

        if (!behandling.skalBehandlesAutomatisk() || behandling.skalSendeVedtaksbrev(erLovendringOgFremtidigOpphørOgNyAndelIAugust2024)) {
            simuleringService.oppdaterSimuleringPåBehandling(behandlingId)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BehandlingsresultatSteg::class.java)
    }
}
