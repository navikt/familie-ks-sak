package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.IBehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandlingsresultatSteg(
    private val behandlingService: BehandlingService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val beregningService: BeregningService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val simuleringService: SimuleringService
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.BEHANDLINGSRESULTAT

    @Transactional
    override fun utførSteg(behandlingId: Long) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
        val behandling = behandlingService.hentBehandling(behandlingId)
        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandlingId)
        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId)
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId)
        val endretUtbetalingMedAndeler = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId)

        BehandlingsresultatUtils.validerAtBehandlingsresultatKanUtføres(
            vilkårsvurdering,
            personopplysningGrunnlag,
            tilkjentYtelse,
            endretUtbetalingMedAndeler
        )

        val resultat = behandlingsresultatService.utledBehandlingsresultat(behandling)

        // valider om behandlingsresultat samsvarer med Behandlingstype
        BehandlingsresultatUtils.validerUtledetBehandlingsresultat(behandling, resultat)
        val behandlingMedOppdatertResultat = behandlingService.oppdaterBehandlingsresultat(behandlingId, resultat)

        if (behandlingMedOppdatertResultat.skalSendeVedtaksbrev()) {
            behandlingService.nullstillEndringstidspunkt(behandlingId)
            // TODO oppdater vedtak med vedtaksperioder
        }
        simuleringService.oppdaterSimuleringPåBehandling(behandlingMedOppdatertResultat)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BehandlingsresultatSteg::class.java)
    }
}
