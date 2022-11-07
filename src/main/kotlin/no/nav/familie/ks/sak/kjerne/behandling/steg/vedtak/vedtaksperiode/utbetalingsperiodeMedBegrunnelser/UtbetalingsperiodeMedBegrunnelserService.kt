package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.tilFørskjøvetVilkårResultatTidslinjeMap
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import org.springframework.stereotype.Service

@Service
class UtbetalingsperiodeMedBegrunnelserService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService
) {
    fun hentUtbetalingsperioder(
        vedtak: Vedtak,
        opphørsperioder: List<VedtaksperiodeMedBegrunnelser>
    ): List<VedtaksperiodeMedBegrunnelser> {
        val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(vedtak.behandling.id)

        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = vedtak.behandling.id)

        val forskjøvetVilkårResultatTidslinjeMap =
            vilkårsvurdering.personResultater.tilFørskjøvetVilkårResultatTidslinjeMap()

        return hentPerioderMedUtbetaling(
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            vedtak = vedtak,
            forskjøvetVilkårResultatTidslinjeMap = forskjøvetVilkårResultatTidslinjeMap
        )
    }
}
