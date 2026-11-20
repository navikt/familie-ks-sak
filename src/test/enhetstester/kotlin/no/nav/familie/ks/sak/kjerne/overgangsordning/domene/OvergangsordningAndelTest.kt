package no.nav.familie.ks.sak.kjerne.overgangsordning.domene

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.randomAktør
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

class OvergangsordningAndelTest {
    private val aktør = randomAktør()

    @Test
    fun `toString skal ikke inneholde aktørId`() {
        // Arrange
        val overgangsordningAndel =
            OvergangsordningAndel(
                id = 1,
                behandlingId = 2,
                aktør = aktør,
                fom = YearMonth.of(2024, 9),
                tom = YearMonth.of(2024, 10),
            )

        // Act
        val tekst = overgangsordningAndel.toString()

        // Assert
        assertFalse(tekst.contains(aktør.aktørId))
        assertTrue(tekst.contains("behandling=2"))
    }

    @Test
    fun `validerAtObligatoriskeFelterErUtfylt skal ikke ta med aktørId i feilmeldingen`() {
        // Arrange
        val overgangsordningAndel = OvergangsordningAndel(behandlingId = 2, aktør = aktør)

        // Act
        val feil = assertThrows<FunksjonellFeil> { overgangsordningAndel.validerAtObligatoriskeFelterErUtfylt() }

        // Assert
        assertFalse(feil.melding.contains(aktør.aktørId))
        assertTrue(feil.melding.startsWith("Person, fom og tom skal være utfylt"))
    }
}
