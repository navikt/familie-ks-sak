package no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ks.sak.api.dto.UtfyltStatus
import no.nav.familie.ks.sak.api.dto.tilUtenlandskPeriodebeløpDto
import no.nav.familie.ks.sak.data.lagUtenlandskPeriodebeløp
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.Intervall
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class UtenlandskePeriodebeløpUtfyltTest {
    @Test
    fun `Skal sette UtfyltStatus til OK når alle felter er utfylt`() {
        val utenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                beløp = BigDecimal.valueOf(500),
                valutakode = "NOK",
                intervall = Intervall.MÅNEDLIG,
            )

        val restUtenlandskPeriodebeløp = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        assertEquals(UtfyltStatus.OK, restUtenlandskPeriodebeløp.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til UFULLSTENDIG når ett eller to felter er utfylt`() {
        var utenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                beløp = BigDecimal.valueOf(500),
            )

        var restUtenlandskPeriodebeløp = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restUtenlandskPeriodebeløp.status)

        utenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                beløp = BigDecimal.valueOf(500),
                valutakode = "NOK",
            )

        restUtenlandskPeriodebeløp = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restUtenlandskPeriodebeløp.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til IKKE_UTFYLT når ingen felter er utfylt`() {
        val utenlandskPeriodebeløp = lagUtenlandskPeriodebeløp()

        val restUtenlandskPeriodebeløp = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        assertEquals(UtfyltStatus.IKKE_UTFYLT, restUtenlandskPeriodebeløp.status)
    }
}
