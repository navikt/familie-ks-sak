package no.nav.familie.ks.sak.kjerne.arbeidsfordeling

import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet.Companion.erGyldigBehandlendeKontantstøtteEnhet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class KontantstøtteEnhetTest {
    @Nested
    inner class ToStringTest {
        @ParameterizedTest
        @EnumSource(KontantstøtteEnhet::class)
        fun `skal returnere korrekt string`(kontantstøtteEnhet: KontantstøtteEnhet) {
            // Act
            val result = kontantstøtteEnhet.toString()

            // Assert
            assertThat(result).isEqualTo("${kontantstøtteEnhet.enhetsnavn} (${kontantstøtteEnhet.enhetsnummer})")
        }
    }

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
        fun `skal returnere false hvis enhetsnummer er 4863 midlertidig enhet`() {
            // Act
            val erGyldig = erGyldigBehandlendeKontantstøtteEnhet(KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer)

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
