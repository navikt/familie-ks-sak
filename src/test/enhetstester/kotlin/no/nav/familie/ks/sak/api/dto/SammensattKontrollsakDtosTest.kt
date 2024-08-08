package no.nav.familie.ks.sak.api.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SammensattKontrollsakDtosTest {
    @Test
    fun `skal mappe fra opprett dto til entity`() {
        // Arrange
        val opprettSammensattKontrollsakDto =
            OpprettSammensattKontrollsakDto(
                behandlingId = 1L,
                fritekst = "blabla",
            )

        // Act
        val sammensattKontrollsak = opprettSammensattKontrollsakDto.tilSammensattKontrollsak()

        // Assert
        assertThat(sammensattKontrollsak.id).isEqualTo(0L)
        assertThat(sammensattKontrollsak.behandlingId).isEqualTo(opprettSammensattKontrollsakDto.behandlingId)
        assertThat(sammensattKontrollsak.fritekst).isEqualTo(opprettSammensattKontrollsakDto.fritekst)
    }
}
