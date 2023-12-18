package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.tilFørskjøvetOppfylteVilkårResultatTidslinjeMap
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.springframework.stereotype.Service

@Service
class UtbetalingsperiodeMedBegrunnelserService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val kompetanseService: KompetanseService,
) {
    fun hentUtbetalingsperioder(
        vedtak: Vedtak,
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        vilkårsvurdering: Vilkårsvurdering,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        kompetanser: List<Kompetanse>,
    ): List<VedtaksperiodeMedBegrunnelser> {
        val forskjøvetVilkårResultatTidslinjeMap =
            vilkårsvurdering.personResultater.tilFørskjøvetOppfylteVilkårResultatTidslinjeMap(personopplysningGrunnlag)

        return hentPerioderMedUtbetaling(
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            vedtak = vedtak,
            forskjøvetVilkårResultatTidslinjeMap = forskjøvetVilkårResultatTidslinjeMap,
            kompetanser = kompetanser,
        )
    }
}
