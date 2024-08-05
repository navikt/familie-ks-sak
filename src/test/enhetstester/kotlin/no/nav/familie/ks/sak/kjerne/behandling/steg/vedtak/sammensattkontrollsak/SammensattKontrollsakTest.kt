package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SammensattKontrollsakTest {
    @Test
    fun `skal mappe entity til dto`() {
        // Arrange
        val sammensattKontrollsak =
            SammensattKontrollsak(
                id = 0L,
                behandlingId = 1L,
                fritekst = "blabla",
            )

        // Act
        val sammensattKontrollsakDto = sammensattKontrollsak.tilSammensattKontrollDto()

        // Assert
        assertThat(sammensattKontrollsakDto.id).isEqualTo(sammensattKontrollsak.id)
        assertThat(sammensattKontrollsakDto.behandlingId).isEqualTo(sammensattKontrollsak.behandlingId)
        assertThat(sammensattKontrollsakDto.fritekst).isEqualTo(sammensattKontrollsak.fritekst)
    }
}
