package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityEØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.Vedtaksbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevPerson
import org.slf4j.LoggerFactory
import java.time.LocalDate

private val logger = LoggerFactory.getLogger(Begrunnelse::class.java)

fun Begrunnelse.tilSanityBegrunnelse(
    sanityBegrunnelser: List<SanityBegrunnelse>
): SanityBegrunnelse? {
    val sanityBegrunnelse = sanityBegrunnelser.find { it.apiNavn == this.sanityApiNavn }
    if (sanityBegrunnelse == null) {
        logger.warn("Finner ikke begrunnelse med apinavn '${this.sanityApiNavn}' på '${this.name}' i Sanity")
    }
    return sanityBegrunnelse
}

fun EØSBegrunnelse.tilSanityEØSBegrunnelse(
    eøsSanityBegrunnelser: List<SanityEØSBegrunnelse>
): SanityEØSBegrunnelse? {
    val sanityBegrunnelse = eøsSanityBegrunnelser.find { it.apiNavn == this.sanityApiNavn }
    if (sanityBegrunnelse == null) {
        logger.warn("Finner ikke begrunnelse med apinavn '${this.sanityApiNavn}' på '${this.name}' i Sanity")
    }
    return sanityBegrunnelse
}

fun Begrunnelse.tilVedtaksbegrunnelse(
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser
): Vedtaksbegrunnelse {
    if (!vedtaksperiodeMedBegrunnelser
        .type
        .tillatteBegrunnelsestyper
        .contains(this.begrunnelseType)
    ) {
        throw Feil(
            "Begrunnelsestype ${this.begrunnelseType} passer ikke med " +
                "typen '${vedtaksperiodeMedBegrunnelser.type}' som er satt på perioden."
        )
    }

    return Vedtaksbegrunnelse(
        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
        begrunnelse = this
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
