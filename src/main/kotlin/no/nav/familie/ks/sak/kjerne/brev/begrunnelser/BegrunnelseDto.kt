package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import no.nav.familie.eksterne.kontrakter.AnnenForeldersAktivitet
import no.nav.familie.eksterne.kontrakter.SøkersAktivitet

sealed class BegrunnelseDto(
    open val type: BrevBegrunnelseType
) : Comparable<BegrunnelseDto> {

    override fun compareTo(other: BegrunnelseDto): Int {
        return when (this) {
            is FritekstBegrunnelseDto -> Int.MAX_VALUE
            is BegrunnelseDtoMedData -> when (other) {
                is FritekstBegrunnelseDto -> -Int.MAX_VALUE
                is BegrunnelseDtoMedData -> {
                    this.vedtakBegrunnelseType.sorteringsrekkefølge - other.vedtakBegrunnelseType.sorteringsrekkefølge
                }
            }
        }
    }
}

enum class BrevBegrunnelseType {
    BEGRUNNELSE,
    EØS_BEGRUNNELSE,
    FRITEKST
}

sealed class BegrunnelseDtoMedData(
    open val apiNavn: String,
    open val vedtakBegrunnelseType: BegrunnelseType,
    type: BrevBegrunnelseType
) : BegrunnelseDto(type)

data class BegrunnelseDataDto(
    override val vedtakBegrunnelseType: BegrunnelseType,
    override val apiNavn: String,

    val gjelderSoker: Boolean,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maanedOgAarBegrunnelsenGjelderFor: String? = null,
    val maalform: String,
    val belop: String,
    val antallTimerBarnehageplass: String
) : BegrunnelseDtoMedData(
    apiNavn = apiNavn,
    type = BrevBegrunnelseType.BEGRUNNELSE,
    vedtakBegrunnelseType = vedtakBegrunnelseType
)

data class FritekstBegrunnelseDto(
    val fritekst: String
) : BegrunnelseDto(type = BrevBegrunnelseType.FRITEKST)

data class EØSBegrunnelseDataDto(
    override val vedtakBegrunnelseType: BegrunnelseType,
    override val apiNavn: String,

    val annenForeldersAktivitet: AnnenForeldersAktivitet,
    val annenForeldersAktivitetsland: String?,
    val barnetsBostedsland: String,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maalform: String,
    val sokersAktivitet: SøkersAktivitet,
    val sokersAktivitetsland: String?
) : BegrunnelseDtoMedData(
    type = BrevBegrunnelseType.EØS_BEGRUNNELSE,
    apiNavn = apiNavn,
    vedtakBegrunnelseType = vedtakBegrunnelseType
)
