package no.nav.familie.ks.sak.kjerne.arbeidsfordeling

import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet.Companion.erGyldigBehandlendeKontantstøtteEnhet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KontantstøtteEnhetTest {
    @Nested
    inner class MyTest {
        @Test
        fun `skal returnere false hvis ugyldig enhetsnummer`() {
            // Act
            val erGyldig = erGyldigBehandlendeKontantstøtteEnhet("1")

            // Assert
            assertThat(erGyldig).isFalse()
        }

        @Test
        fun `skal returnere true hvis gyldig enhetsnummer`() {
            // Act
            val erGyldig = erGyldigBehandlendeKontantstøtteEnhet(KontantstøtteEnhet.OSLO.enhetsnummer)

            // Assert
            assertThat(erGyldig).isTrue()
        }
    }
}
