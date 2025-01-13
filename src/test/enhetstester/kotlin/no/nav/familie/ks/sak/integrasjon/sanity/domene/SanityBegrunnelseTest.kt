package no.nav.familie.ks.sak.integrasjon.sanity.domene

import no.nav.familie.ks.sak.data.lagSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SanityBegrunnelseTest {
    @Test
    fun `erOvergangsordningBegrunnelse skal returnere true hvis begrunnelse er en overgangsordning begrunnelse`() {
        // Arrange && Act
        val sanityBegrunnelse = lagSanityBegrunnelse(apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING.sanityApiNavn)

        // Assert
        assertThat(sanityBegrunnelse.erOvergangsordningBegrunnelse()).isTrue()
    }

    @Test
    fun `erOvergangsordningBegrunnelse skal returnere true hvis begrunnelse er en opphør overgangsordning begrunnelse`() {
        // Arrange && Act
        val sanityBegrunnelse = lagSanityBegrunnelse(apiNavn = NasjonalEllerFellesBegrunnelse.OPPHØR_OVERGANGSORDNING_OPPHØR.sanityApiNavn)

        // Assert
        assertThat(sanityBegrunnelse.erOvergangsordningBegrunnelse()).isTrue()
    }

    @Test
    fun `erOvergangsordningBegrunnelse skal returnere false hvis begrunnelse ikke er en overgangsordning begrunnelse`() {
        // Arrange
        val sanityBegrunnelse = lagSanityBegrunnelse(apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE.sanityApiNavn)

        // Assert
        assertThat(sanityBegrunnelse.erOvergangsordningBegrunnelse()).isFalse()
    }
}
