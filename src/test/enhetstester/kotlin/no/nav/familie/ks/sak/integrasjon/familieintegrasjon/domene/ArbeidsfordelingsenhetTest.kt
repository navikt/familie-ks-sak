package no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene

import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ArbeidsfordelingsenhetTest {
    @Nested
    inner class OpprettFra {
        @ParameterizedTest
        @EnumSource(KontantstøtteEnhet::class)
        fun `skal opprette arbeidsfordelingsenhet fra kontantstøtte enhet`(enhet: KontantstøtteEnhet) {
            // Act
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(enhet)

            // Assert
            assertThat(arbeidsfordelingsenhet.enhetId).isEqualTo(enhet.enhetsnummer)
            assertThat(arbeidsfordelingsenhet.enhetNavn).isEqualTo(enhet.enhetsnavn)
        }
    }
}
