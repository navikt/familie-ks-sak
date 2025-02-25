package no.nav.familie.ks.sak.kjerne.eøs.util

import no.nav.familie.ks.sak.api.dto.tilKalkulertMånedligBeløp
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import java.time.YearMonth

class UtenlandskPeriodebeløpBuilder(
    startMåned: YearMonth,
    behandlingId: BehandlingId = BehandlingId(1),
) : SkjemaBuilder<UtenlandskPeriodebeløp, UtenlandskPeriodebeløpBuilder>(startMåned, behandlingId) {
    fun medBeløp(
        k: String,
        valutakode: String?,
        utbetalingsland: String?,
        vararg barn: Person,
    ) = medSkjema(k, barn.toList()) {
        when {
            it == '-' -> UtenlandskPeriodebeløp.NULL.copy(utbetalingsland = utbetalingsland)
            it == '$' ->
                UtenlandskPeriodebeløp.NULL.copy(
                    valutakode = valutakode,
                    utbetalingsland = utbetalingsland,
                )
            it?.isDigit() ?: false -> {
                UtenlandskPeriodebeløp.NULL.copy(
                    beløp = it?.digitToInt()?.toBigDecimal(),
                    valutakode = valutakode,
                    intervall = Intervall.MÅNEDLIG,
                    utbetalingsland = utbetalingsland,
                    kalkulertMånedligBeløp = it?.digitToInt()?.toBigDecimal(),
                )
            }
            else -> null
        }
    }

    fun medIntervall(intervall: Intervall) =
        medTransformasjon { utenlandskPeriodebeløp -> utenlandskPeriodebeløp.copy(intervall = intervall) }.medTransformasjon { utenlandskPeriodebeløp ->
            utenlandskPeriodebeløp.copy(
                kalkulertMånedligBeløp = utenlandskPeriodebeløp.tilKalkulertMånedligBeløp(),
            )
        }
}
