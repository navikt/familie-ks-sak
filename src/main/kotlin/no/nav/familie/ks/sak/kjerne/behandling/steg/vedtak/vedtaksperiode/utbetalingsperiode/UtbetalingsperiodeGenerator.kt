package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiode

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.tilForskjøvetOppfylteVilkårResultatTidslinjeMap
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.springframework.stereotype.Component

@Component
class UtbetalingsperiodeGenerator(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val kompetanseService: KompetanseService,
    private val adopsjonService: AdopsjonService,
) {
    fun genererUtbetalingsperioder(
        vedtak: Vedtak,
    ): List<VedtaksperiodeMedBegrunnelser> {
        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(vedtak.behandling.id)

        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = vedtak.behandling.id)
        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = vedtak.behandling.id)

        val adopsjonerIBehandling = adopsjonService.hentAlleAdopsjonerForBehandling(BehandlingId(vedtak.behandling.id))

        val forskjøvetVilkårResultatTidslinjeMap =
            vilkårsvurdering.personResultater.tilForskjøvetOppfylteVilkårResultatTidslinjeMap(
                personopplysningGrunnlag = personopplysningGrunnlag,
                adopsjonerIBehandling = adopsjonerIBehandling,
            )

        return hentPerioderMedUtbetaling(
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            vedtak = vedtak,
            forskjøvetVilkårResultatTidslinjeMap = forskjøvetVilkårResultatTidslinjeMap,
            kompetanser = kompetanseService.hentKompetanser(vedtak.behandling.behandlingId),
        )
    }
}
