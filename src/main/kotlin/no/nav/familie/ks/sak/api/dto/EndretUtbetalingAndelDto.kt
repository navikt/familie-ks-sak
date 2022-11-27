package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class EndretUtbetalingAndelDto(
    val id: Long?,
    val personIdent: String?,
    val prosent: BigDecimal?,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val årsak: Årsak?,
    val avtaletidspunktDeltBosted: LocalDate?,
    val søknadstidspunkt: LocalDate?,
    val begrunnelse: String?,
    val erTilknyttetAndeler: Boolean?
)
