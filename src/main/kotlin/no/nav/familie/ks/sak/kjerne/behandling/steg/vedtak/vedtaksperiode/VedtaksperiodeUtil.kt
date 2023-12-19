package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erSammeEllerEtter
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import java.time.LocalDate

fun validerVedtaksperiodeMedBegrunnelser(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser) {
    if ((vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.OPPHØR || vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.AVSLAG) && vedtaksperiodeMedBegrunnelser.harFriteksterUtenStandardbegrunnelser()) {
        val fritekstUtenStandardbegrunnelserFeilmelding =
            "Fritekst kan kun brukes i kombinasjon med en eller flere begrunnelser. " + "Legg først til en ny begrunnelse eller fjern friteksten(e)."
        throw FunksjonellFeil(
            melding = fritekstUtenStandardbegrunnelserFeilmelding,
            frontendFeilmelding = fritekstUtenStandardbegrunnelserFeilmelding,
        )
    }

    if (vedtaksperiodeMedBegrunnelser.vedtak.behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET && vedtaksperiodeMedBegrunnelser.harFriteksterOgStandardbegrunnelser()) {
        throw FunksjonellFeil(
            "Det ble sendt med både fritekst og begrunnelse. " + "Vedtaket skal enten ha fritekst eller begrunnelse, men ikke begge deler.",
        )
    }
}

fun filtrerUtUtbetalingsperioderMedSammeDatoSomAvslagsperioder(
    utbetalingsperioder: List<VedtaksperiodeMedBegrunnelser>,
    avslagsperioder: List<VedtaksperiodeMedBegrunnelser>,
) = utbetalingsperioder.filter { utbetalingsperiode ->
    avslagsperioder.none { avslagsperiode ->
        avslagsperiode.fom == utbetalingsperiode.fom &&
            avslagsperiode.tom == utbetalingsperiode.tom &&
            avslagsperiode.begrunnelser.isNotEmpty()
    }
}

fun filtrerUtPerioderBasertPåEndringstidspunkt(
    vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
    manueltOverstyrtEndringstidspunkt: LocalDate?,
    gjelderFortsattInnvilget: Boolean,
    sisteVedtatteBehandling: Behandling?,
    andelerTilkjentYtelseForBehandling: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    andelerTilkjentYtelseForForrigeBehandling: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
): List<VedtaksperiodeMedBegrunnelser> {
    val endringstidspunkt = (
        manueltOverstyrtEndringstidspunkt
            ?: if (!gjelderFortsattInnvilget) {
                finnEndringstidspunktForBehandling(
                    sisteVedtatteBehandling = sisteVedtatteBehandling,
                    andelerTilkjentYtelseForBehandling = andelerTilkjentYtelseForBehandling,
                    andelerTilkjentYtelseForForrigeBehandling = andelerTilkjentYtelseForForrigeBehandling,
                )
            } else {
                TIDENES_MORGEN
            }
    )

    return vedtaksperioderMedBegrunnelser.filter {
        (it.tom ?: TIDENES_ENDE).erSammeEllerEtter(endringstidspunkt)
    }
}

fun finnEndringstidspunktForBehandling(
    sisteVedtatteBehandling: Behandling?,
    andelerTilkjentYtelseForBehandling: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    andelerTilkjentYtelseForForrigeBehandling: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
): LocalDate {
    if (sisteVedtatteBehandling == null) return TIDENES_MORGEN

    if (andelerTilkjentYtelseForBehandling.isEmpty()) return TIDENES_MORGEN

    val førsteEndringstidspunktFraAndelTilkjentYtelse =
        andelerTilkjentYtelseForBehandling.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = andelerTilkjentYtelseForForrigeBehandling,
        ) ?: TIDENES_ENDE

    // TODO EØS

    return førsteEndringstidspunktFraAndelTilkjentYtelse
}
