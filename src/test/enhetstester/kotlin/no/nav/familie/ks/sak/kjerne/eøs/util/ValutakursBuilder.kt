package no.nav.familie.ks.sak.kjerne.eøs.util

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import java.time.YearMonth

class ValutakursBuilder(
    startMåned: YearMonth,
    behandlingId: BehandlingId = BehandlingId(1L),
) : SkjemaBuilder<Valutakurs, ValutakursBuilder>(startMåned, behandlingId) {
    fun medKurs(
        k: String,
        valutakode: String?,
        vararg barn: Person,
    ) = medSkjema(k, barn.toList()) {
        when {
            it == '-' -> {
                Valutakurs.NULL
            }

            it == '$' -> {
                Valutakurs.NULL.copy(valutakode = valutakode)
            }

            it?.isDigit() ?: false -> {
                Valutakurs.NULL.copy(
                    kurs = it?.digitToInt()?.toBigDecimal(),
                    valutakode = valutakode,
                    valutakursdato = null,
                )
            }

            else -> {
                null
            }
        }
    }
}
