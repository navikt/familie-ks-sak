package no.nav.familie.ks.sak.api.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelseDeserializer
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class EndretUtbetalingAndelRequestDto(
    val id: Long,
    val personIdent: String,
    val prosent: BigDecimal,
    val fom: YearMonth,
    val tom: YearMonth,
    val årsak: Årsak,
    val avtaletidspunktDeltBosted: LocalDate?,
    val søknadstidspunkt: LocalDate,
    val begrunnelse: String,
    val erEksplisittAvslagPåSøknad: Boolean?,
    @JsonDeserialize(using = IBegrunnelseDeserializer::class)
    val begrunnelser: List<NasjonalEllerFellesBegrunnelse>,
)

fun EndretUtbetalingAndelRequestDto.mapTilVedtaksbegrunnelser(): List<NasjonalEllerFellesBegrunnelse> {
    if (this.erEksplisittAvslagPåSøknad == false) {
        return emptyList()
    }
    return when (this.årsak) {
        Årsak.DELT_BOSTED,
        Årsak.ENDRE_MOTTAKER,
        Årsak.ETTERBETALING_3MND,
        -> listOf(NasjonalEllerFellesBegrunnelse.AVSLAG_SØKT_FOR_SENT_ENDRINGSPERIODE)

        Årsak.ALLEREDE_UTBETALT,
        ->
            this.begrunnelser

        Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
        -> listOf(NasjonalEllerFellesBegrunnelse.AVSLAG_FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024)
    }
}
