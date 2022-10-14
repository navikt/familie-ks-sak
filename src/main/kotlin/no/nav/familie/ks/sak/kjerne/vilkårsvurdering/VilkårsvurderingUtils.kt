package no.nav.familie.ks.sak.kjerne.vilkårsvurdering

import no.nav.familie.ks.sak.api.dto.VedtakBegrunnelseTilknyttetVilkårResponseDto
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityEØSBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.tilTriggesAv
import no.nav.familie.ks.sak.kjerne.vedtak.EØSStandardbegrunnelse
import no.nav.familie.ks.sak.kjerne.vedtak.Standardbegrunnelse
import no.nav.familie.ks.sak.kjerne.vedtak.tilSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.vedtak.tilSanityEØSBegrunnelse

fun standardbegrunnelserTilNedtrekksmenytekster(
    sanityBegrunnelser: List<SanityBegrunnelse>
) =
    Standardbegrunnelse
        .values()
        .groupBy { it.vedtakBegrunnelseType }
        .mapValues { begrunnelseGruppe ->
            begrunnelseGruppe.value
                .flatMap { vedtakBegrunnelse ->
                    vedtakBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
                        sanityBegrunnelser,
                        vedtakBegrunnelse
                    )
                }
        }

fun eøsStandardbegrunnelserTilNedtrekksmenytekster(
    sanityEØSBegrunnelser: List<SanityEØSBegrunnelse>
) = EØSStandardbegrunnelse.values().groupBy { it.vedtakBegrunnelseType }
    .mapValues { begrunnelseGruppe ->
        begrunnelseGruppe.value.flatMap { vedtakBegrunnelse ->
            eøsBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
                sanityEØSBegrunnelser,
                vedtakBegrunnelse
            )
        }
    }

fun vedtakBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
    sanityBegrunnelser: List<SanityBegrunnelse>,
    vedtakBegrunnelse: Standardbegrunnelse
): List<VedtakBegrunnelseTilknyttetVilkårResponseDto> {
    val sanityBegrunnelse = vedtakBegrunnelse.tilSanityBegrunnelse(sanityBegrunnelser) ?: return emptyList()

    val triggesAv = sanityBegrunnelse.tilTriggesAv()
    val visningsnavn = sanityBegrunnelse.navnISystem

    return if (triggesAv.vilkår.isEmpty()) {
        listOf(
            VedtakBegrunnelseTilknyttetVilkårResponseDto(
                id = vedtakBegrunnelse,
                navn = visningsnavn,
                vilkår = null
            )
        )
    } else {
        triggesAv.vilkår.map {
            VedtakBegrunnelseTilknyttetVilkårResponseDto(
                id = vedtakBegrunnelse,
                navn = visningsnavn,
                vilkår = it
            )
        }
    }
}

fun eøsBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
    sanityEØSBegrunnelser: List<SanityEØSBegrunnelse>,
    vedtakBegrunnelse: EØSStandardbegrunnelse
): List<VedtakBegrunnelseTilknyttetVilkårResponseDto> {
    val eøsSanityBegrunnelse = vedtakBegrunnelse.tilSanityEØSBegrunnelse(sanityEØSBegrunnelser) ?: return emptyList()

    return listOf(
        VedtakBegrunnelseTilknyttetVilkårResponseDto(
            id = vedtakBegrunnelse,
            navn = eøsSanityBegrunnelse.navnISystem,
            vilkår = null
        )
    )
}
