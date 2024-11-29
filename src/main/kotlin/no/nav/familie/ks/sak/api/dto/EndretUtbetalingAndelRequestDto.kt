package no.nav.familie.ks.sak.api.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelseDeserializer
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
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
    @JsonDeserialize(using = IBegrunnelseDeserializer::class)
    val begrunnelser: List<NasjonalEllerFellesBegrunnelse>?,
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
    val begrunnelser: List<NasjonalEllerFellesBegrunnelse>?,
)

data class SanityBegrunnelseMedEndringsårsakResponseDto(
    val id: IBegrunnelse,
    val navn: String,
    val endringsårsaker: List<Årsak>,
)
