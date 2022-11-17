package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityEØSBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.tilTriggesAv
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.Vedtaksbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevEndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPerson
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPersonResultat
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevVedtaksPeriode
import no.nav.familie.ks.sak.kjerne.brev.domene.harPersonerSomManglerOpplysninger
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.slf4j.LoggerFactory
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

fun Standardbegrunnelse.triggesForPeriode(
    brevVedtaksPeriode: BrevVedtaksPeriode,
    brevPersonResultater: List<BrevPersonResultat>,
    brevPersoner: List<BrevPerson>,
    aktørIderMedUtbetaling: List<String>,
    brevEndredeUtbetalingAndeler: List<BrevEndretUtbetalingAndel> = emptyList(),
    sanityBegrunnelser: List<SanityBegrunnelse>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
    ytelserForrigePeriode: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
): Boolean {
    val triggesAv = this.tilSanityBegrunnelse(sanityBegrunnelser)?.tilTriggesAv() ?: return false

    val aktuellePersoner = brevPersoner
        .filter { person -> triggesAv.personTyper.contains(person.type) }
        .filter { person ->
            if (this.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET) {
                aktørIderMedUtbetaling.contains(person.aktørId) || person.type == PersonType.SØKER
            } else {
                true
            }
        }

    fun hentPersonerForUtgjørendeVilkår() = hentPersonerForAlleUtgjørendeVilkår(
        brevPersonResultater = brevPersonResultater,
        vedtaksperiode = Periode(
            fom = brevVedtaksPeriode.fom ?: TIDENES_MORGEN,
            tom = brevVedtaksPeriode.tom ?: TIDENES_ENDE
        ),
        oppdatertBegrunnelseType = this.vedtakBegrunnelseType,
        aktuellePersonerForVedtaksperiode = aktuellePersoner,
        triggesAv = triggesAv,
        erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak
    )

    return when {
        !triggesAv.valgbar -> false

        triggesAv.personerManglerOpplysninger -> brevPersonResultater.harPersonerSomManglerOpplysninger()

        triggesAv.etterEndretUtbetaling ->
            erEtterEndretPeriodeAvSammeÅrsak(
                brevEndredeUtbetalingAndeler,
                brevVedtaksPeriode,
                aktuellePersoner,
                triggesAv
            )

        triggesAv.erEndret() -> erEndretTriggerErOppfylt(
            triggesAv = triggesAv,
            brevEndretUtbetalingAndel = brevEndredeUtbetalingAndeler,
            brevVedtaksPeriode = brevVedtaksPeriode
        )

        triggesAv.gjelderFraInnvilgelsestidspunkt -> false

        triggesAv.barnDød -> dødeBarnForrigePeriode(
            ytelserForrigePeriode,
            brevPersoner.filter { it.type === PersonType.BARN }
        ).any()

        else -> hentPersonerForUtgjørendeVilkår().isNotEmpty()
    }
}

private fun erEtterEndretPeriodeAvSammeÅrsak(
    endretUtbetalingAndeler: List<BrevEndretUtbetalingAndel>,
    brevVedtaksPeriode: BrevVedtaksPeriode,
    aktuellePersoner: List<BrevPerson>,
    triggesAv: TriggesAv
) = endretUtbetalingAndeler.any { endretUtbetalingAndel ->
    endretUtbetalingAndel.månedPeriode().tom.sisteDagIInneværendeMåned()
        .erDagenFør(brevVedtaksPeriode.fom) &&
        aktuellePersoner.any { person -> person.aktørId == endretUtbetalingAndel.aktørId } &&
        triggesAv.endringsaarsaker.contains(endretUtbetalingAndel.årsak)
}

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

fun dødeBarnForrigePeriode(
    ytelserForrigePeriode: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    barnIBehandling: List<BrevPerson>
): List<String> =
    barnIBehandling.filter { barn ->
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

fun List<LocalDate>.tilBrevTekst(): String = slåSammen(this.sorted().map { it.tilKortString() })

private fun erEndretTriggerErOppfylt(
    triggesAv: TriggesAv,
    brevEndretUtbetalingAndel: List<BrevEndretUtbetalingAndel>,
    brevVedtaksPeriode: BrevVedtaksPeriode
): Boolean {
    val endredeAndelerSomOverlapperVedtaksperiode = brevVedtaksPeriode
        .finnEndredeAndelerISammePeriode(brevEndretUtbetalingAndel)

    return endredeAndelerSomOverlapperVedtaksperiode.any {
        triggesAv.erTriggereOppfyltForEndretUtbetaling(
            brevEndretUtbetalingAndel = it
        )
    }
}

fun TriggesAv.erTriggereOppfyltForEndretUtbetaling(
    brevEndretUtbetalingAndel: BrevEndretUtbetalingAndel
): Boolean {
    val hørerTilEtterEndretUtbetaling = this.etterEndretUtbetaling

    val oppfyllerSkalUtbetalesTrigger = brevEndretUtbetalingAndel.oppfyllerSkalUtbetalesTrigger(this)

    val erAvSammeÅrsak = this.endringsaarsaker.contains(brevEndretUtbetalingAndel.årsak)

    return !hørerTilEtterEndretUtbetaling &&
        oppfyllerSkalUtbetalesTrigger &&
        erAvSammeÅrsak
}
