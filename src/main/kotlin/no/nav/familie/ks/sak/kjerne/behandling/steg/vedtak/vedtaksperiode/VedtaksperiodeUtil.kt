package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.tilTriggesAv
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.IVedtakBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.Standardbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakBegrunnelseType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.endretUtbetalingsperiodeBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.tilSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.triggesForPeriode
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevEndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPerson
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPersonResultat
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevVedtaksPeriode
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBrevEndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBrevPersonResultat
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBrevPersoner
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBrevVedtaksPeriode
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.time.LocalDate

fun validerVedtaksperiodeMedBegrunnelser(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser) {
    if ((vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.OPPHØR || vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.AVSLAG) && vedtaksperiodeMedBegrunnelser.harFriteksterUtenStandardbegrunnelser()) {
        val fritekstUtenStandardbegrunnelserFeilmelding =
            "Fritekst kan kun brukes i kombinasjon med en eller flere begrunnelser. " + "Legg først til en ny begrunnelse eller fjern friteksten(e)."
        throw FunksjonellFeil(
            melding = fritekstUtenStandardbegrunnelserFeilmelding,
            frontendFeilmelding = fritekstUtenStandardbegrunnelserFeilmelding
        )
    }

    if (vedtaksperiodeMedBegrunnelser.vedtak.behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET && vedtaksperiodeMedBegrunnelser.harFriteksterOgStandardbegrunnelser()) {
        throw FunksjonellFeil(
            "Det ble sendt med både fritekst og begrunnelse. " + "Vedtaket skal enten ha fritekst eller begrunnelse, men ikke begge deler."
        )
    }
}

/**
 * Brukes for opphør som har egen logikk dersom det er første periode.
 */
private fun erFørsteVedtaksperiodePåFagsak(
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    periodeFom: LocalDate?
): Boolean = !andelerTilkjentYtelse.any {
    it.stønadFom.isBefore(
        periodeFom?.toYearMonth() ?: TIDENES_MORGEN.toYearMonth()
    )
}

fun hentGyldigeBegrunnelserForPeriode(
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    persongrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering,
    aktørIderMedUtbetaling: List<String>,
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
): List<IVedtakBegrunnelse> {
    val standardbegrunnelser = hentGyldigeStandardbegrunnelserForVedtaksperiode(
        utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
        sanityBegrunnelser = sanityBegrunnelser,
        persongrunnlag = persongrunnlag,
        vilkårsvurdering = vilkårsvurdering,
        aktørIderMedUtbetaling = aktørIderMedUtbetaling,
        endretUtbetalingAndeler = endretUtbetalingAndeler,
        andelerTilkjentYtelse = andelerTilkjentYtelse
    )

// TODO: Legg inn EØS når vi kommer så langt

    return standardbegrunnelser
}

private fun hentGyldigeStandardbegrunnelserForVedtaksperiode(
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    persongrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering,
    aktørIderMedUtbetaling: List<String>,
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
) = hentGyldigeBegrunnelserForVedtaksperiode(
    brevVedtaksPeriode = utvidetVedtaksperiodeMedBegrunnelser.tilBrevVedtaksPeriode(),
    sanityBegrunnelser = sanityBegrunnelser,
    brevPersoner = persongrunnlag.tilBrevPersoner(),
    brevPersonResultater = vilkårsvurdering.personResultater
        .map { it.tilBrevPersonResultat() },
    aktørIderMedUtbetaling = aktørIderMedUtbetaling,
    brevEndretUtbetalingAndeler = endretUtbetalingAndeler
        .map { it.tilBrevEndretUtbetalingAndel() },
    erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak(
        andelerTilkjentYtelse,
        utvidetVedtaksperiodeMedBegrunnelser.fom
    ),
    ytelserForrigePerioder = andelerTilkjentYtelse.filter {
        ytelseErFraForrigePeriode(
            it,
            utvidetVedtaksperiodeMedBegrunnelser
        )
    }
)

private fun hentGyldigeBegrunnelserForVedtaksperiode(
    brevVedtaksPeriode: BrevVedtaksPeriode,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    brevPersoner: List<BrevPerson>,
    brevPersonResultater: List<BrevPersonResultat>,
    aktørIderMedUtbetaling: List<String>,
    brevEndretUtbetalingAndeler: List<BrevEndretUtbetalingAndel>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
    ytelserForrigePerioder: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
): List<Standardbegrunnelse> {
    val tillateBegrunnelserForVedtakstype = Standardbegrunnelse.values()
        .filter {
            brevVedtaksPeriode
                .type
                .tillatteBegrunnelsestyper
                .contains(it.vedtakBegrunnelseType)
        }.filter {
            if (it.vedtakBegrunnelseType == VedtakBegrunnelseType.ENDRET_UTBETALING) {
                endretUtbetalingsperiodeBegrunnelser.contains(it)
            } else {
                true
            }
        }

    return when (brevVedtaksPeriode.type) {
        Vedtaksperiodetype.FORTSATT_INNVILGET,
        Vedtaksperiodetype.AVSLAG -> tillateBegrunnelserForVedtakstype

        else -> {
            velgUtbetalingsbegrunnelser(
                tillateBegrunnelserForVedtakstype,
                sanityBegrunnelser,
                brevVedtaksPeriode,
                brevPersonResultater,
                brevPersoner,
                aktørIderMedUtbetaling,
                brevEndretUtbetalingAndeler,
                erFørsteVedtaksperiodePåFagsak,
                ytelserForrigePerioder
            )
        }
    }
}

private fun velgUtbetalingsbegrunnelser(
    tillateBegrunnelserForVedtakstype: List<Standardbegrunnelse>,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    brevVedtaksPeriode: BrevVedtaksPeriode,
    brevPersonResultater: List<BrevPersonResultat>,
    brevPersoner: List<BrevPerson>,
    aktørIderMedUtbetaling: List<String>,
    brevEndredeUtbetalingAndeler: List<BrevEndretUtbetalingAndel>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
    ytelserForrigePeriode: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
): List<Standardbegrunnelse> {
    val standardbegrunnelser: MutableSet<Standardbegrunnelse> =
        tillateBegrunnelserForVedtakstype
            .filter { it.vedtakBegrunnelseType != VedtakBegrunnelseType.FORTSATT_INNVILGET }
            .filter { it.tilSanityBegrunnelse(sanityBegrunnelser)?.tilTriggesAv()?.valgbar ?: false }
            .fold(mutableSetOf()) { acc, standardBegrunnelse ->
                if (standardBegrunnelse.triggesForPeriode(
                        brevVedtaksPeriode = brevVedtaksPeriode,
                        brevPersonResultater = brevPersonResultater,
                        brevPersoner = brevPersoner,
                        aktørIderMedUtbetaling = aktørIderMedUtbetaling,
                        brevEndredeUtbetalingAndeler = brevEndredeUtbetalingAndeler,
                        sanityBegrunnelser = sanityBegrunnelser,
                        erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
                        ytelserForrigePeriode = ytelserForrigePeriode
                    )
                ) {
                    acc.add(standardBegrunnelse)
                }

                acc
            }

    val fantIngenbegrunnelserOgSkalDerforBrukeFortsattInnvilget =
        brevVedtaksPeriode.type == Vedtaksperiodetype.UTBETALING &&
            standardbegrunnelser.isEmpty()

    return if (fantIngenbegrunnelserOgSkalDerforBrukeFortsattInnvilget) {
        tillateBegrunnelserForVedtakstype
            .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.FORTSATT_INNVILGET }
    } else {
        standardbegrunnelser.toList()
    }
}

private fun ytelseErFraForrigePeriode(
    ytelse: AndelTilkjentYtelseMedEndreteUtbetalinger,
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser
) = ytelse.stønadTom.sisteDagIInneværendeMåned().erDagenFør(utvidetVedtaksperiodeMedBegrunnelser.fom)
