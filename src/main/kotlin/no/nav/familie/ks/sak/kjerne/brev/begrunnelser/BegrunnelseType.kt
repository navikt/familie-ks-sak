package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

enum class BegrunnelseType(
    val sorteringsrekkefølge: Int,
) {
    REDUKSJON(1),
    EØS_REDUKSJON(1),
    INNVILGET(2),
    EØS_INNVILGET(2),
    AVSLAG(3),
    EØS_AVSLAG(3),
    OPPHØR(4),
    EØS_OPPHØR(4),
    FORTSATT_INNVILGET(5),
    ETTER_ENDRET_UTBETALING(6),
    ENDRET_UTBETALING(7),
}

fun BegrunnelseType.erAvslagEllerEøsAvslag(): Boolean = this == BegrunnelseType.AVSLAG || this == BegrunnelseType.EØS_AVSLAG
