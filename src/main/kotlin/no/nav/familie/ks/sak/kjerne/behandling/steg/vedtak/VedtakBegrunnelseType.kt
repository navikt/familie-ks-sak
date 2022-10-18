package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

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
