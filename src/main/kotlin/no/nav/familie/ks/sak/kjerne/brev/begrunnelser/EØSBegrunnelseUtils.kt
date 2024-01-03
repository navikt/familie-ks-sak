package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.EØSBegrunnelseDB
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser

fun EØSBegrunnelse.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): EØSBegrunnelseDB {
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

    return EØSBegrunnelseDB(
        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
        begrunnelse = this,
    )
}
