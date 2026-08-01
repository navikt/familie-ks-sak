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
        // Arrange
        val utenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                beløp = BigDecimal.valueOf(500),
                valutakode = "NOK",
                intervall = Intervall.MÅNEDLIG,
            )

        // Act
        val restUtenlandskPeriodebeløp = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        // Assert
        assertEquals(UtfyltStatus.OK, restUtenlandskPeriodebeløp.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til UFULLSTENDIG når ett eller to felter er utfylt`() {
        // Arrange
        var utenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                beløp = BigDecimal.valueOf(500),
            )

        // Act
        var restUtenlandskPeriodebeløp = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        // Assert
        assertEquals(UtfyltStatus.UFULLSTENDIG, restUtenlandskPeriodebeløp.status)

        // Arrange
        utenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                beløp = BigDecimal.valueOf(500),
                valutakode = "NOK",
            )

        // Act
        restUtenlandskPeriodebeløp = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        // Assert
        assertEquals(UtfyltStatus.UFULLSTENDIG, restUtenlandskPeriodebeløp.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til IKKE_UTFYLT når ingen felter er utfylt`() {
        // Arrange
        val utenlandskPeriodebeløp = lagUtenlandskPeriodebeløp()

        // Act
        val restUtenlandskPeriodebeløp = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        // Assert
        assertEquals(UtfyltStatus.IKKE_UTFYLT, restUtenlandskPeriodebeløp.status)
    }
}
