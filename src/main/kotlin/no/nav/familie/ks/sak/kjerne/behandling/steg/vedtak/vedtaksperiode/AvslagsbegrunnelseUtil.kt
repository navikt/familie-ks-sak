package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.common.util.NullablePeriode
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toLocalDate
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.Vedtaksbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.Begrunnelse

fun hentAvslagsperioderMedBegrunnelser(
    vedtak: Vedtak,
    endredeUtbetalinger: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>,
    vilkårsvurdering: Vilkårsvurdering,
    uregistrerteBarnFraSøknad: List<BarnMedOpplysningerDto>,
): List<VedtaksperiodeMedBegrunnelser> {
    val behandling = vedtak.behandling
    val avslagsperioderFraVilkårsvurdering =
        hentAvslagsperioderFraVilkårsvurdering(
            vedtak = vedtak,
            vilkårsvurdering = vilkårsvurdering,
        )
    val avslagsperioderFraEndretUtbetalinger =
        hentAvslagsperioderFraEndretUtbetalinger(
            vedtak,
            endredeUtbetalinger,
        )

    val uregistrerteBarn =
        if (behandling.erSøknad()) {
            uregistrerteBarnFraSøknad
        } else {
            emptyList()
        }

    return if (uregistrerteBarn.isNotEmpty()) {
        leggTilAvslagsbegrunnelseForUregistrertBarn(
            avslagsperioder = avslagsperioderFraVilkårsvurdering + avslagsperioderFraEndretUtbetalinger,
            vedtak = vedtak,
            uregistrerteBarn = uregistrerteBarn,
        )
    } else {
        avslagsperioderFraVilkårsvurdering + avslagsperioderFraEndretUtbetalinger
    }
}

private fun hentAvslagsperioderFraVilkårsvurdering(
    vedtak: Vedtak,
    vilkårsvurdering: Vilkårsvurdering,
): MutableList<VedtaksperiodeMedBegrunnelser> {
    val periodegrupperteAvslagsvilkår: Map<NullablePeriode, List<VilkårResultat>> =
        vilkårsvurdering.personResultater.flatMap { it.vilkårResultater }
            .filter { it.erEksplisittAvslagPåSøknad == true }
            .groupBy { NullablePeriode(it.periodeFom, it.periodeTom) }

    val avslagsperioder =
        periodegrupperteAvslagsvilkår.map { (fellesPeriode, vilkårResultater) ->

            val avslagsbegrunnelser = vilkårResultater.map { it.begrunnelser }.flatten().toSet().toList()

            lagVedtaksPeriodeMedBegrunnelser(vedtak, fellesPeriode, avslagsbegrunnelser)
        }.toMutableList()

    return avslagsperioder
}

private fun lagVedtaksPeriodeMedBegrunnelser(
    vedtak: Vedtak,
    periode: NullablePeriode,
    avslagsbegrunnelser: List<Begrunnelse>,
): VedtaksperiodeMedBegrunnelser =
    VedtaksperiodeMedBegrunnelser(
        vedtak = vedtak,
        fom = periode.fom,
        tom = periode.tom?.sisteDagIMåned(),
        type = Vedtaksperiodetype.AVSLAG,
    ).apply {
        begrunnelser.addAll(
            avslagsbegrunnelser.map { begrunnelse ->
                Vedtaksbegrunnelse(
                    vedtaksperiodeMedBegrunnelser = this,
                    begrunnelse = begrunnelse,
                )
            },
        )
    }

private fun hentAvslagsperioderFraEndretUtbetalinger(
    vedtak: Vedtak,
    endredeUtbetalinger: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>,
): List<VedtaksperiodeMedBegrunnelser> {
    val periodegrupperteAvslagEndreteUtbetalinger =
        endredeUtbetalinger.filter { it.erEksplisittAvslagPåSøknad == true }
            .groupBy { NullablePeriode(it.fom?.toLocalDate(), it.tom?.toLocalDate()) }

    val avslagsperioder =
        periodegrupperteAvslagEndreteUtbetalinger.map { (fellesPeriode, endretUtbetalinger) ->

            val avslagsbegrunnelser = endretUtbetalinger.map { it.begrunnelser }.flatten().toSet().toList()

            lagVedtaksPeriodeMedBegrunnelser(vedtak, fellesPeriode, avslagsbegrunnelser)
        }.toMutableList()

    return avslagsperioder
}

private fun leggTilAvslagsbegrunnelseForUregistrertBarn(
    avslagsperioder: List<VedtaksperiodeMedBegrunnelser>,
    vedtak: Vedtak,
    uregistrerteBarn: List<BarnMedOpplysningerDto>,
): List<VedtaksperiodeMedBegrunnelser> {
    val avslagsperioderMedTomPeriode =
        if (avslagsperioder.none { it.fom == null && it.tom == null }) {
            avslagsperioder +
                VedtaksperiodeMedBegrunnelser(
                    vedtak = vedtak,
                    fom = null,
                    tom = null,
                    type = Vedtaksperiodetype.AVSLAG,
                )
        } else {
            avslagsperioder
        }

    return avslagsperioderMedTomPeriode.map {
        if (it.fom == null && it.tom == null && uregistrerteBarn.isNotEmpty()) {
            it.apply {
                begrunnelser.add(
                    Vedtaksbegrunnelse(
                        vedtaksperiodeMedBegrunnelser = this,
                        begrunnelse = Begrunnelse.AVSLAG_UREGISTRERT_BARN,
                    ),
                )
            }
        } else {
            it
        }
    }.toList()
}
