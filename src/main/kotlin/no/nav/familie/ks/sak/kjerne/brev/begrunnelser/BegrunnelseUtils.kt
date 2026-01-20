package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.NasjonalEllerFellesBegrunnelseDB
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import org.slf4j.LoggerFactory
import java.time.LocalDate

private val logger = LoggerFactory.getLogger(NasjonalEllerFellesBegrunnelse::class.java)

fun IBegrunnelse.tilSanityBegrunnelse(sanityBegrunnelser: List<SanityBegrunnelse>): SanityBegrunnelse? {
    val sanityBegrunnelse = sanityBegrunnelser.find { it.apiNavn == this.sanityApiNavn }
    if (sanityBegrunnelse == null) {
        logger.warn("Finner ikke begrunnelse med apinavn '${this.sanityApiNavn}' i Sanity")
    }
    return sanityBegrunnelse
}

fun NasjonalEllerFellesBegrunnelse.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): NasjonalEllerFellesBegrunnelseDB {
    if (!vedtaksperiodeMedBegrunnelser
            .type
            .tillatteBegrunnelsestyper
            .contains(this.begrunnelseType)
    ) {
        throw Feil(
            "Begrunnelsestype ${this.begrunnelseType} passer ikke med " +
                "typen '${vedtaksperiodeMedBegrunnelser.type}' som er satt på perioden.",
        )
    }

    return NasjonalEllerFellesBegrunnelseDB(
        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
        nasjonalEllerFellesBegrunnelse = this,
    )
}

fun List<LocalDate>.tilBrevTekst(): String = slåSammen(this.sorted().map { it.tilKortString() })
