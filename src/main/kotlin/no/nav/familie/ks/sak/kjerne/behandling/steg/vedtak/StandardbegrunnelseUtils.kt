package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.common.util.nbLocale
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.sanity.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityEØSBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.tilTriggesAv
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.MinimertEndretAndel
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.MinimertPerson
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.MinimertRestPersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.MinimertVedtaksperiode
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.Vedtaksbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.harPersonerSomManglerOpplysninger
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.brev.domene.MinimertUtbetalingsperiodeDetalj
import no.nav.familie.ks.sak.kjerne.brev.domene.tilMinimertUtbetalingsperiodeDetalj
import no.nav.familie.ks.sak.kjerne.brev.hentPersonerForAlleUtgjørendeVilkår
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.slf4j.LoggerFactory
import java.text.NumberFormat
import java.time.LocalDate

private val logger = LoggerFactory.getLogger(Standardbegrunnelse::class.java)

fun Standardbegrunnelse.tilSanityBegrunnelse(
    sanityBegrunnelser: List<SanityBegrunnelse>
): SanityBegrunnelse? {
    val sanityBegrunnelse = sanityBegrunnelser.find { it.apiNavn == this.sanityApiNavn }
    if (sanityBegrunnelse == null) {
        logger.warn("Finner ikke begrunnelse med apinavn '${this.sanityApiNavn}' på '${this.name}' i Sanity")
    }
    return sanityBegrunnelse
}

fun EØSStandardbegrunnelse.tilSanityEØSBegrunnelse(
    eøsSanityBegrunnelser: List<SanityEØSBegrunnelse>
): SanityEØSBegrunnelse? {
    val sanityBegrunnelse = eøsSanityBegrunnelser.find { it.apiNavn == this.sanityApiNavn }
    if (sanityBegrunnelse == null) {
        logger.warn("Finner ikke begrunnelse med apinavn '${this.sanityApiNavn}' på '${this.name}' i Sanity")
    }
    return sanityBegrunnelse
}

fun List<LocalDate>.tilBrevTekst(): String = slåSammen(this.sorted().map { it.tilKortString() })

fun formaterBeløp(beløp: Int): String = NumberFormat.getNumberInstance(nbLocale).format(beløp)

fun Standardbegrunnelse.tilVedtaksbegrunnelse(
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser
): Vedtaksbegrunnelse {
    if (!vedtaksperiodeMedBegrunnelser
            .type
            .tillatteBegrunnelsestyper
            .contains(this.vedtakBegrunnelseType)
    ) {
        throw Feil(
            "Begrunnelsestype ${this.vedtakBegrunnelseType} passer ikke med " +
                    "typen '${vedtaksperiodeMedBegrunnelser.type}' som er satt på perioden."
        )
    }

    return Vedtaksbegrunnelse(
        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
        standardbegrunnelse = this
    )
}

fun Standardbegrunnelse.triggesForPeriode(
    minimertVedtaksperiode: MinimertVedtaksperiode,
    minimertePersonResultater: List<MinimertRestPersonResultat>,
    minimertePersoner: List<MinimertPerson>,
    aktørIderMedUtbetaling: List<String>,
    minimerteEndredeUtbetalingAndeler: List<MinimertEndretAndel> = emptyList(),
    sanityBegrunnelser: List<SanityBegrunnelse>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
    ytelserForSøkerForrigeMåned: List<YtelseType>,
    ytelserForrigePeriode: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
): Boolean {
    val triggesAv = this.tilSanityBegrunnelse(sanityBegrunnelser)?.tilTriggesAv() ?: return false

    val aktuellePersoner = minimertePersoner
        .filter { person -> triggesAv.personTyper.contains(person.type) }
        .filter { person ->
            if (this.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET) {
                aktørIderMedUtbetaling.contains(person.aktørId) || person.type == PersonType.SØKER
            } else {
                true
            }
        }

    val ytelseTyperForPeriode = minimertVedtaksperiode.ytelseTyperForPeriode

    fun hentPersonerForUtgjørendeVilkår() = hentPersonerForAlleUtgjørendeVilkår(
        minimertePersonResultater = minimertePersonResultater,
        vedtaksperiode = Periode(
            fom = minimertVedtaksperiode.fom ?: TIDENES_MORGEN,
            tom = minimertVedtaksperiode.tom ?: TIDENES_ENDE
        ),
        oppdatertBegrunnelseType = this.vedtakBegrunnelseType,
        aktuellePersonerForVedtaksperiode = aktuellePersoner.map { it.tilMinimertRestPerson() },
        triggesAv = triggesAv,
        erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak
    )

    return when {
        !triggesAv.valgbar -> false

        triggesAv.personerManglerOpplysninger -> minimertePersonResultater.harPersonerSomManglerOpplysninger()

        triggesAv.etterEndretUtbetaling ->
            erEtterEndretPeriodeAvSammeÅrsak(
                minimerteEndredeUtbetalingAndeler,
                minimertVedtaksperiode,
                aktuellePersoner,
                triggesAv
            )

        triggesAv.erEndret() && !triggesAv.etterEndretUtbetaling -> erEndretTriggerErOppfylt(
            triggesAv = triggesAv,
            minimerteEndredeUtbetalingAndeler = minimerteEndredeUtbetalingAndeler,
            minimertVedtaksperiode = minimertVedtaksperiode
        )

        triggesAv.gjelderFraInnvilgelsestidspunkt -> false

        triggesAv.barnDød -> dødeBarnForrigePeriode(
            ytelserForrigePeriode,
            minimertePersoner.filter { it.type === PersonType.BARN }
        ).any()
        else -> hentPersonerForUtgjørendeVilkår().isNotEmpty()
    }
}

private fun erEndretTriggerErOppfylt(
    triggesAv: TriggesAv,
    minimerteEndredeUtbetalingAndeler: List<MinimertEndretAndel>,
    minimertVedtaksperiode: MinimertVedtaksperiode
): Boolean {
    val endredeAndelerSomOverlapperVedtaksperiode = minimertVedtaksperiode
        .finnEndredeAndelerISammePeriode(minimerteEndredeUtbetalingAndeler)

    return endredeAndelerSomOverlapperVedtaksperiode.any { minimertEndretAndel ->
        triggesAv.erTriggereOppfyltForEndretUtbetaling(
            minimertEndretAndel = minimertEndretAndel,
            minimerteUtbetalingsperiodeDetaljer = minimertVedtaksperiode
                .utbetalingsperioder.map { it.tilMinimertUtbetalingsperiodeDetalj() }
        )
    }
}

fun TriggesAv.erTriggereOppfyltForEndretUtbetaling(
    minimertEndretAndel: MinimertEndretAndel,
    minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj>
): Boolean {
    val hørerTilEtterEndretUtbetaling = this.etterEndretUtbetaling

    val oppfyllerSkalUtbetalesTrigger = minimertEndretAndel.oppfyllerSkalUtbetalesTrigger(this)


    val erAvSammeÅrsak = this.endringsaarsaker.contains(minimertEndretAndel.årsak)

    return !hørerTilEtterEndretUtbetaling &&
            oppfyllerSkalUtbetalesTrigger && erAvSammeÅrsak
}

private fun erEtterEndretPeriodeAvSammeÅrsak(
    endretUtbetalingAndeler: List<MinimertEndretAndel>,
    minimertVedtaksperiode: MinimertVedtaksperiode,
    aktuellePersoner: List<MinimertPerson>,
    triggesAv: TriggesAv
) = endretUtbetalingAndeler.any { endretUtbetalingAndel ->
    endretUtbetalingAndel.månedPeriode().tom.sisteDagIInneværendeMåned()
        .erDagenFør(minimertVedtaksperiode.fom) &&
            aktuellePersoner.any { person -> person.aktørId == endretUtbetalingAndel.aktørId } &&
            triggesAv.endringsaarsaker.contains(endretUtbetalingAndel.årsak)
}

fun dødeBarnForrigePeriode(
    ytelserForrigePeriode: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    barnIBehandling: List<MinimertPerson>
): List<String> {
    return barnIBehandling.filter { barn ->
        val ytelserForrigePeriodeForBarn = ytelserForrigePeriode.filter {
            it.aktør.aktivFødselsnummer() == barn.aktivPersonIdent
        }
        var barnDødeForrigePeriode = false
        if (barn.erDød() && ytelserForrigePeriodeForBarn.isNotEmpty()) {
            val fom =
                ytelserForrigePeriodeForBarn.minOf { it.stønadFom }
            val tom =
                ytelserForrigePeriodeForBarn.maxOf { it.stønadTom }
            val fomFørDødsfall = fom <= barn.dødsfallsdato!!.toYearMonth()
            val tomEtterDødsfall = tom >= barn.dødsfallsdato.toYearMonth()
            barnDødeForrigePeriode = fomFørDødsfall && tomEtterDødsfall
        }
        barnDødeForrigePeriode
    }.map { it.aktivPersonIdent }
}