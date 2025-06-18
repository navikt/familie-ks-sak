package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class EndretUtbetalingAndelResponsDto(
    val id: Long?,
    val personIdent: String?,
    val personIdenter: List<String>?,
    val prosent: BigDecimal?,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val årsak: Årsak?,
    val søknadstidspunkt: LocalDate?,
    val begrunnelse: String?,
    val erEksplisittAvslagPåSøknad: Boolean?,
    val erTilknyttetAndeler: Boolean,
    val vedtaksbegrunnelser: List<NasjonalEllerFellesBegrunnelse>,
)
