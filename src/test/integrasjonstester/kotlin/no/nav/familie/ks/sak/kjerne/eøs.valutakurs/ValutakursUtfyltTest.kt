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
        // Arrange
        val valutakurs =
            lagValutakurs(
                valutakursdato = LocalDate.now(),
                kurs = BigDecimal.valueOf(10),
            )

        // Act
        val restValutakurs = valutakurs.tilValutakursDto()

        // Assert
        Assertions.assertEquals(UtfyltStatus.OK, restValutakurs.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til UFULLSTENDIG når ett felt er utfylt`() {
        // Arrange
        var valutakurs =
            lagValutakurs(
                valutakursdato = LocalDate.now(),
            )

        // Act
        var restValutakurs = valutakurs.tilValutakursDto()

        // Assert
        Assertions.assertEquals(UtfyltStatus.UFULLSTENDIG, restValutakurs.status)

        // Arrange
        valutakurs =
            lagValutakurs(
                kurs = BigDecimal.valueOf(10),
            )

        // Act
        restValutakurs = valutakurs.tilValutakursDto()

        // Assert
        Assertions.assertEquals(UtfyltStatus.UFULLSTENDIG, restValutakurs.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til IKKE_UTFYLT når ingen felter er utfylt`() {
        // Arrange
        val valutakurs = lagValutakurs()

        // Act
        val restValutakurs = valutakurs.tilValutakursDto()

        // Assert
        Assertions.assertEquals(UtfyltStatus.IKKE_UTFYLT, restValutakurs.status)
    }
}
