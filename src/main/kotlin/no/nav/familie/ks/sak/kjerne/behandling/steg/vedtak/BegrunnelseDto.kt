package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

import no.nav.familie.eksterne.kontrakter.AnnenForeldersAktivitet
import no.nav.familie.eksterne.kontrakter.SøkersAktivitet
import no.nav.familie.ks.sak.kjerne.vedtak.VedtakBegrunnelseType

enum class Begrunnelsetype {
    STANDARD_BEGRUNNELSE,
    EØS_BEGRUNNELSE,
    FRITEKST
}

interface BegrunnelseDto : Comparable<BegrunnelseDto> {
    val type: Begrunnelsetype
    val vedtakBegrunnelseType: VedtakBegrunnelseType?

    override fun compareTo(other: BegrunnelseDto): Int {
        return when {
            this.type == Begrunnelsetype.FRITEKST -> Int.MAX_VALUE
            other.type == Begrunnelsetype.FRITEKST -> -Int.MAX_VALUE
            this.vedtakBegrunnelseType == null -> Int.MAX_VALUE
            other.vedtakBegrunnelseType == null -> -Int.MAX_VALUE

            else -> this.vedtakBegrunnelseType!!.sorteringsrekkefølge - other.vedtakBegrunnelseType!!.sorteringsrekkefølge
        }
    }
}

interface BegrunnelseDtoMedData : BegrunnelseDto {
    val apiNavn: String
}

data class StandardBegrunnelseDataDto(
    override val vedtakBegrunnelseType: VedtakBegrunnelseType,
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
    override val type: Begrunnelsetype = Begrunnelsetype.STANDARD_BEGRUNNELSE
}

data class FritekstBegrunnelseDto(
    val fritekst: String
) : BegrunnelseDto {
    override val vedtakBegrunnelseType: VedtakBegrunnelseType? = null
    override val type: Begrunnelsetype = Begrunnelsetype.FRITEKST
}

data class EØSBegrunnelseDataDto(
    override val vedtakBegrunnelseType: VedtakBegrunnelseType,
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
    override val type: Begrunnelsetype = Begrunnelsetype.EØS_BEGRUNNELSE
}
