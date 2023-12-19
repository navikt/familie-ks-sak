package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.erSammeEllerEtter
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
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
    endringstidspunkt: LocalDate,
): List<VedtaksperiodeMedBegrunnelser> {
    return vedtaksperioderMedBegrunnelser.filter {
        (it.tom ?: TIDENES_ENDE).erSammeEllerEtter(endringstidspunkt)
    }
}
