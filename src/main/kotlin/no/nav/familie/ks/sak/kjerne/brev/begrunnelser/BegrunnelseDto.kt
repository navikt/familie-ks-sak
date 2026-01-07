package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseType
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet

sealed class BegrunnelseDto(
    open val type: BrevBegrunnelseType,
) : Comparable<BegrunnelseDto> {
    override fun compareTo(other: BegrunnelseDto): Int =
        when (this) {
            is FritekstBegrunnelseDto -> {
                Int.MAX_VALUE
            }

            is BegrunnelseDtoMedData -> {
                when (other) {
                    is FritekstBegrunnelseDto -> {
                        this.vedtakBegrunnelseType.sorteringsrekkefølge
                    }

                    is BegrunnelseDtoMedData -> {
                        when (this.sanityBegrunnelseType) {
                            SanityBegrunnelseType.STANDARD -> {
                                if (other.sanityBegrunnelseType == SanityBegrunnelseType.STANDARD) {
                                    this.vedtakBegrunnelseType.sorteringsrekkefølge - other.vedtakBegrunnelseType.sorteringsrekkefølge
                                } else {
                                    -Int.MAX_VALUE
                                }
                            }

                            else -> {
                                if (other.sanityBegrunnelseType == SanityBegrunnelseType.STANDARD) {
                                    Int.MAX_VALUE
                                } else {
                                    this.vedtakBegrunnelseType.sorteringsrekkefølge - other.vedtakBegrunnelseType.sorteringsrekkefølge
                                }
                            }
                        }
                    }
                }
            }
        }
}

enum class BrevBegrunnelseType {
    BEGRUNNELSE,
    EØS_BEGRUNNELSE,
    FRITEKST,
}

sealed class BegrunnelseDtoMedData(
    open val apiNavn: String,
    open val vedtakBegrunnelseType: BegrunnelseType,
    open val sanityBegrunnelseType: SanityBegrunnelseType,
    override val type: BrevBegrunnelseType,
) : BegrunnelseDto(type)

data class NasjonalOgFellesBegrunnelseDataDto(
    override val vedtakBegrunnelseType: BegrunnelseType,
    override val apiNavn: String,
    override val sanityBegrunnelseType: SanityBegrunnelseType,
    val gjelderSoker: Boolean,
    val gjelderAndreForelder: Boolean,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maanedOgAarBegrunnelsenGjelderFor: String? = null,
    val maalform: String,
    val belop: String,
    val antallTimerBarnehageplass: String,
    val soknadstidspunkt: String,
    val maanedOgAarFoerVedtaksperiode: String?,
) : BegrunnelseDtoMedData(
        apiNavn = apiNavn,
        type = BrevBegrunnelseType.BEGRUNNELSE,
        vedtakBegrunnelseType = vedtakBegrunnelseType,
        sanityBegrunnelseType = sanityBegrunnelseType,
    )

data class FritekstBegrunnelseDto(
    val fritekst: String,
) : BegrunnelseDto(type = BrevBegrunnelseType.FRITEKST)

sealed class EØSBegrunnelseDto(
    override val apiNavn: String,
    override val vedtakBegrunnelseType: BegrunnelseType,
    override val sanityBegrunnelseType: SanityBegrunnelseType,
    override val type: BrevBegrunnelseType,
) : BegrunnelseDtoMedData(
        apiNavn = apiNavn,
        vedtakBegrunnelseType = vedtakBegrunnelseType,
        sanityBegrunnelseType = sanityBegrunnelseType,
        type = type,
    )

data class EØSBegrunnelseMedKompetanseDto(
    override val vedtakBegrunnelseType: BegrunnelseType,
    override val apiNavn: String,
    override val sanityBegrunnelseType: SanityBegrunnelseType,
    val annenForeldersAktivitet: KompetanseAktivitet,
    val annenForeldersAktivitetsland: String?,
    val barnetsBostedsland: String,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maalform: String,
    val sokersAktivitet: KompetanseAktivitet,
    val sokersAktivitetsland: String?,
    val antallTimerBarnehageplass: String,
    val erAnnenForelderOmfattetAvNorskLovgivning: Boolean,
) : EØSBegrunnelseDto(
        type = BrevBegrunnelseType.EØS_BEGRUNNELSE,
        apiNavn = apiNavn,
        vedtakBegrunnelseType = vedtakBegrunnelseType,
        sanityBegrunnelseType = sanityBegrunnelseType,
    )

data class EØSBegrunnelseUtenKompetanseDto(
    override val vedtakBegrunnelseType: BegrunnelseType,
    override val apiNavn: String,
    override val sanityBegrunnelseType: SanityBegrunnelseType,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maalform: String,
    val gjelderSoker: Boolean,
    val antallTimerBarnehageplass: String,
) : EØSBegrunnelseDto(
        type = BrevBegrunnelseType.EØS_BEGRUNNELSE,
        apiNavn = apiNavn,
        vedtakBegrunnelseType = vedtakBegrunnelseType,
        sanityBegrunnelseType = sanityBegrunnelseType,
    )
