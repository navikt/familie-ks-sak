package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser

import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleConfig
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.tilForskjøvetOppfylteVilkårResultatTidslinjeMap
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.springframework.stereotype.Service

@Service
class UtbetalingsperiodeMedBegrunnelserService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val kompetanseService: KompetanseService,
    private val unleashNextMedContextService: UnleashNextMedContextService,
) {
    fun hentUtbetalingsperioder(
        vedtak: Vedtak,
    ): List<VedtaksperiodeMedBegrunnelser> {
        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(vedtak.behandling.id)

        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = vedtak.behandling.id)
        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = vedtak.behandling.id)
        val erToggleForLovendringAugust2024På = unleashNextMedContextService.isEnabled(FeatureToggleConfig.LOV_ENDRING_7_MND_NYE_BEHANDLINGER)

        val forskjøvetVilkårResultatTidslinjeMap =
            vilkårsvurdering.personResultater.tilForskjøvetOppfylteVilkårResultatTidslinjeMap(personopplysningGrunnlag, erToggleForLovendringAugust2024På)

        return hentPerioderMedUtbetaling(
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            vedtak = vedtak,
            forskjøvetVilkårResultatTidslinjeMap = forskjøvetVilkårResultatTidslinjeMap,
            kompetanser = kompetanseService.hentKompetanser(vedtak.behandling.behandlingId),
        )
    }
}
