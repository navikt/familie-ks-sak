package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import no.nav.familie.eksterne.kontrakter.AnnenForeldersAktivitet
import no.nav.familie.eksterne.kontrakter.SøkersAktivitet

interface BegrunnelseDto : Comparable<BegrunnelseDto> {
    val brevBegrunnelseType: BrevBegrunnelseType
    val begrunnelseType: BegrunnelseType?

    override fun compareTo(other: BegrunnelseDto): Int {
        return when {
            this.brevBegrunnelseType == BrevBegrunnelseType.FRITEKST -> Int.MAX_VALUE
            other.brevBegrunnelseType == BrevBegrunnelseType.FRITEKST -> -Int.MAX_VALUE
            this.begrunnelseType == null -> Int.MAX_VALUE
            other.begrunnelseType == null -> -Int.MAX_VALUE

            else -> this.begrunnelseType!!.sorteringsrekkefølge - other.begrunnelseType!!.sorteringsrekkefølge
        }
    }
}

interface BegrunnelseDtoMedData : BegrunnelseDto {
    val apiNavn: String
}

data class StandardBegrunnelseDataDto(
    override val begrunnelseType: BegrunnelseType,
    override val apiNavn: String,

    val gjelderSoker: Boolean,
    val barnasFodselsdatoer: String,
    val fodselsdatoerBarnOppfyllerTriggereOgHarUtbetaling: String,
    val fodselsdatoerBarnOppfyllerTriggereOgHarNullutbetaling: String,
    val antallBarn: Int,
    val antallBarnOppfyllerTriggereOgHarUtbetaling: Int,
    val antallBarnOppfyllerTriggereOgHarNullutbetaling: Int,
    val maanedOgAarBegrunnelsenGjelderFor: String?,
    val maalform: String,
    val belop: String,
    val soknadstidspunkt: String,
    val avtaletidspunktDeltBosted: String,
    val sokersRettTilUtvidet: String
) : BegrunnelseDtoMedData {
    override val brevBegrunnelseType: BrevBegrunnelseType = BrevBegrunnelseType.STANDARD_BEGRUNNELSE
}

data class FritekstBegrunnelseDto(
    val fritekst: String
) : BegrunnelseDto {
    override val begrunnelseType: BegrunnelseType? = null
    override val brevBegrunnelseType: BrevBegrunnelseType = BrevBegrunnelseType.FRITEKST
}

data class EØSBegrunnelseDataDto(
    override val begrunnelseType: BegrunnelseType,
    override val apiNavn: String,

    val annenForeldersAktivitet: AnnenForeldersAktivitet,
    val annenForeldersAktivitetsland: String?,
    val barnetsBostedsland: String,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maalform: String,
    val sokersAktivitet: SøkersAktivitet,
    val sokersAktivitetsland: String?
) : BegrunnelseDtoMedData {
    override val brevBegrunnelseType: BrevBegrunnelseType = BrevBegrunnelseType.EØS_BEGRUNNELSE
}
