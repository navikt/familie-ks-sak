package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakBegrunnelseType


data class VedtaksbegrunnelseDto(
    val standardbegrunnelse: String,
    val vedtakBegrunnelseSpesifikasjon: String,
    val vedtakBegrunnelseType: VedtakBegrunnelseType
)
