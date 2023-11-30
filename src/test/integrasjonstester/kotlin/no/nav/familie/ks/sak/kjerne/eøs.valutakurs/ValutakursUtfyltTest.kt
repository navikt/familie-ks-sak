package no.nav.familie.ks.sak.kjerne.eøs.valutakurs

import no.nav.familie.ks.sak.api.dto.UtfyltStatus
import no.nav.familie.ks.sak.api.dto.tilValutakursDto
import no.nav.familie.ks.sak.data.lagValutakurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ValutakursUtfyltTest {
    @Test
    fun `Skal sette UtfyltStatus til OK når alle felter er utfylt`() {
        val valutakurs =
            lagValutakurs(
                valutakursdato = LocalDate.now(),
                kurs = BigDecimal.valueOf(10),
            )

        val restValutakurs = valutakurs.tilValutakursDto()

        Assertions.assertEquals(UtfyltStatus.OK, restValutakurs.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til UFULLSTENDIG når ett felt er utfylt`() {
        var valutakurs =
            lagValutakurs(
                valutakursdato = LocalDate.now(),
            )

        var restValutakurs = valutakurs.tilValutakursDto()

        Assertions.assertEquals(UtfyltStatus.UFULLSTENDIG, restValutakurs.status)

        valutakurs =
            lagValutakurs(
                kurs = BigDecimal.valueOf(10),
            )

        restValutakurs = valutakurs.tilValutakursDto()

        Assertions.assertEquals(UtfyltStatus.UFULLSTENDIG, restValutakurs.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til IKKE_UTFYLT når ingen felter er utfylt`() {
        val valutakurs = lagValutakurs()

        val restValutakurs = valutakurs.tilValutakursDto()

        Assertions.assertEquals(UtfyltStatus.IKKE_UTFYLT, restValutakurs.status)
    }
}
