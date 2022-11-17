package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakBegrunnelseType

interface Begrunnelse : Comparable<Begrunnelse> {

    val type: Begrunnelsetype
    val vedtakBegrunnelseType: VedtakBegrunnelseType?

    override fun compareTo(other: Begrunnelse): Int {
        return when {
            this.type == Begrunnelsetype.FRITEKST -> Int.MAX_VALUE
            other.type == Begrunnelsetype.FRITEKST -> -Int.MAX_VALUE
            this.vedtakBegrunnelseType == null -> Int.MAX_VALUE
            other.vedtakBegrunnelseType == null -> -Int.MAX_VALUE

            else -> this.vedtakBegrunnelseType!!.sorteringsrekkefølge - other.vedtakBegrunnelseType!!.sorteringsrekkefølge
        }
    }
}

enum class Begrunnelsetype {
    STANDARD_BEGRUNNELSE,
    EØS_BEGRUNNELSE,
    FRITEKST
}
