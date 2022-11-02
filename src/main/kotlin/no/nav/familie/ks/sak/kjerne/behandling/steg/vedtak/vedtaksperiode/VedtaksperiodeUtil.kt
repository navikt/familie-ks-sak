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
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPerson
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBrevPersonResultat
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBrevPersoner
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevEndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevVedtaksPeriode
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBrevEndretUtbetalingAndel
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
            "Det ble sendt med både fritekst og begrunnelse. " + "Vedtaket skal enten ha fritekst eller bregrunnelse, men ikke begge deler."
        )
    }
}

/**
 * Brukes for opphør som har egen logikk dersom det er første periode.
 */
fun erFørsteVedtaksperiodePåFagsak(
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>, periodeFom: LocalDate?
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

//TODO: Legg inn EØS når vi kommer så langt

    return standardbegrunnelser
}

fun hentGyldigeStandardbegrunnelserForVedtaksperiode(
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    persongrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering,
    aktørIderMedUtbetaling: List<String>,
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
) = hentGyldigeBegrunnelserForVedtaksperiodeMinimert(
    brevVedtaksPeriode = utvidetVedtaksperiodeMedBegrunnelser.tilBrevVedtaksPeriode(),
    sanityBegrunnelser = sanityBegrunnelser,
    minimertePersoner = persongrunnlag.tilBrevPersoner(),
    minimertePersonresultater = vilkårsvurdering.personResultater
        .map { it.tilBrevPersonResultat() },
    aktørIderMedUtbetaling = aktørIderMedUtbetaling,
    minimerteEndredeUtbetalingAndeler = endretUtbetalingAndeler
        .map { it.tilBrevEndretUtbetalingAndel() },
    erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak(
        andelerTilkjentYtelse,
        utvidetVedtaksperiodeMedBegrunnelser.fom
    ),
    ytelserForSøkerForrigeMåned = hentYtelserForSøkerForrigeMåned(
        andelerTilkjentYtelse,
        utvidetVedtaksperiodeMedBegrunnelser
    ),
    ytelserForrigePerioder = andelerTilkjentYtelse.filter {
        ytelseErFraForrigePeriode(
            it,
            utvidetVedtaksperiodeMedBegrunnelser
        )
    }
)

fun hentGyldigeBegrunnelserForVedtaksperiodeMinimert(
    brevVedtaksPeriode: BrevVedtaksPeriode,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    minimertePersoner: List<BrevPerson>,
    minimertePersonresultater: List<BrevPersonResultat>,
    aktørIderMedUtbetaling: List<String>,
    minimerteEndredeUtbetalingAndeler: List<BrevEndretUtbetalingAndel>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
    ytelserForSøkerForrigeMåned: List<YtelseType>,
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
                minimertePersonresultater,
                minimertePersoner,
                aktørIderMedUtbetaling,
                minimerteEndredeUtbetalingAndeler,
                erFørsteVedtaksperiodePåFagsak,
                ytelserForSøkerForrigeMåned,
                ytelserForrigePerioder
            )
        }
    }
}

private fun velgUtbetalingsbegrunnelser(
    tillateBegrunnelserForVedtakstype: List<Standardbegrunnelse>,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    brevVedtaksPeriode: BrevVedtaksPeriode,
    minimertePersonresultater: List<BrevPersonResultat>,
    minimertePersoner: List<BrevPerson>,
    aktørIderMedUtbetaling: List<String>,
    minimerteEndredeUtbetalingAndeler: List<BrevEndretUtbetalingAndel>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
    ytelserForSøkerForrigeMåned: List<YtelseType>,
    ytelserForrigePeriode: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
): List<Standardbegrunnelse> {
    val standardbegrunnelser: MutableSet<Standardbegrunnelse> =
        tillateBegrunnelserForVedtakstype
            .filter { it.vedtakBegrunnelseType != VedtakBegrunnelseType.FORTSATT_INNVILGET }
            .filter { it.tilSanityBegrunnelse(sanityBegrunnelser)?.tilTriggesAv()?.valgbar ?: false }
            .fold(mutableSetOf()) { acc, standardBegrunnelse ->
                if (standardBegrunnelse.triggesForPeriode(
                        brevVedtaksPeriode = brevVedtaksPeriode,
                        minimertePersonResultater = minimertePersonresultater,
                        minimertePersoner = minimertePersoner,
                        aktørIderMedUtbetaling = aktørIderMedUtbetaling,
                        minimerteEndredeUtbetalingAndeler = minimerteEndredeUtbetalingAndeler,
                        sanityBegrunnelser = sanityBegrunnelser,
                        erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
                        ytelserForSøkerForrigeMåned = ytelserForSøkerForrigeMåned,
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

fun ytelseErFraForrigePeriode(
    ytelse: AndelTilkjentYtelseMedEndreteUtbetalinger,
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser
) = ytelse.stønadTom.sisteDagIInneværendeMåned().erDagenFør(utvidetVedtaksperiodeMedBegrunnelser.fom)


fun hentYtelserForSøkerForrigeMåned(
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser
) = andelerTilkjentYtelse.filter {
            ytelseErFraForrigePeriode(it, utvidetVedtaksperiodeMedBegrunnelser)
}.map { it.type }
