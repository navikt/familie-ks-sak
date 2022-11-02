package no.nav.familie.ks.sak.api.dto

import java.time.LocalDate

data class VedtaksperiodeMedFriteksterDto(
    val fritekster: List<String> = emptyList()
)

data class VedtaksperiodeMedStandardbegrunnelserDto(
    val standardbegrunnelser: List<String>
)

data class GenererVedtaksperioderForOverstyrtEndringstidspunktDto(
    val behandlingId: Long,
    val overstyrtEndringstidspunkt: LocalDate
)

data class GenererFortsattInnvilgetVedtaksperioderDto(
    val skalGenererePerioderForFortsattInnvilget: Boolean,
    val behandlingId: Long
)
