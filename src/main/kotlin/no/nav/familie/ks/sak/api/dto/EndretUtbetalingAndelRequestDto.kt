package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class EndretUtbetalingAndelRequestDto(
    val id: Long?,
    val personIdent: String?,
    val prosent: BigDecimal?,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val årsak: Årsak?,
    val avtaletidspunktDeltBosted: LocalDate?,
    val søknadstidspunkt: LocalDate?,
    val begrunnelse: String?,
    val erEksplisittAvslagPåSøknad: Boolean?,
)

data class EndretUtbetalingAndelResponsDto(
    val id: Long?,
    val personIdent: String?,
    val prosent: BigDecimal?,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val årsak: Årsak?,
    val avtaletidspunktDeltBosted: LocalDate?,
    val søknadstidspunkt: LocalDate?,
    val begrunnelse: String?,
    val erEksplisittAvslagPåSøknad: Boolean?,
    val erTilknyttetAndeler: Boolean,
)

data class SanityBegrunnelseMedEndringsårsakResponseDto(
    val id: IBegrunnelse,
    val navn: String,
    val endringsårsaker: List<Årsak>,
)
