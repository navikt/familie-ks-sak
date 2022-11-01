package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.forrigeMåned
import no.nav.familie.ks.sak.common.util.tilMånedÅr

enum class VedtakBegrunnelseType(val sorteringsrekkefølge: Int) {
    INNVILGET(2),
    EØS_INNVILGET(2),
    REDUKSJON(1),
    AVSLAG(3),
    OPPHØR(4),
    EØS_OPPHØR(4),
    FORTSATT_INNVILGET(5),
    ENDRET_UTBETALING(7),
    ETTER_ENDRET_UTBETALING(6)
}

fun VedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(periode: Periode) = when (this) {
    VedtakBegrunnelseType.AVSLAG ->
        if (periode.fom == TIDENES_MORGEN && periode.tom == TIDENES_ENDE) {
            ""
        } else if (periode.tom == TIDENES_ENDE) {
            periode.fom.tilMånedÅr()
        } else {
            "${periode.fom.tilMånedÅr()} til ${periode.tom.tilMånedÅr()}"
        }
    else ->
        if (periode.fom == TIDENES_MORGEN) {
            throw Feil("Prøver å finne fom-dato for begrunnelse, men fikk \"TIDENES_MORGEN\".")
        } else {
            periode.fom.forrigeMåned().tilMånedÅr()
        }
}