package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

interface BrevBegrunnelse : Comparable<BrevBegrunnelse> {

    val type: BrevBegrunnelseType
    val begrunnelseType: BegrunnelseType?

    override fun compareTo(other: BrevBegrunnelse): Int {
        return when {
            this.type == BrevBegrunnelseType.FRITEKST -> Int.MAX_VALUE
            other.type == BrevBegrunnelseType.FRITEKST -> -Int.MAX_VALUE
            this.begrunnelseType == null -> Int.MAX_VALUE
            other.begrunnelseType == null -> -Int.MAX_VALUE

            else -> this.begrunnelseType!!.sorteringsrekkefølge - other.begrunnelseType!!.sorteringsrekkefølge
        }
    }
}

enum class BrevBegrunnelseType {
    STANDARD_BEGRUNNELSE,
    EØS_BEGRUNNELSE,
    FRITEKST
}
