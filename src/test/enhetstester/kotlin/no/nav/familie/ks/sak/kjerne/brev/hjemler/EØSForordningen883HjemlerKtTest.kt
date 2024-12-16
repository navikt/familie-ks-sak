package no.nav.familie.ks.sak.kjerne.brev.hjemler

import no.nav.familie.ks.sak.data.lagSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EØSForordningen883HjemlerKtTest {
    @Test
    fun `skal returnere en tom liste om input er en tom liste`() {
        // Act
        val eøsForordningen883Hjemler = utledEØSForordningen883Hjemler(sanityBegrunnelser = emptyList())

        // Assert
        assertThat(eøsForordningen883Hjemler).isEmpty()
    }

    @Test
    fun `skal utlede EØS forordningen 883 hjemler`() {
        // Arrange
        val sanityEøsBegrunnelse1 =
            lagSanityBegrunnelse(
                apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD.name,
                hjemlerEøsForordningen883 = listOf("6", "4", "3"),
            )

        val sanityEøsBegrunnelse2 =
            lagSanityBegrunnelse(
                apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.name,
                hjemlerEøsForordningen883 = listOf("2", "1", "6"),
            )

        val sanityBegrunnelser = listOf(sanityEøsBegrunnelse1, sanityEøsBegrunnelse2)

        // Act
        val eøsForordningen883Hjemler = utledEØSForordningen883Hjemler(sanityBegrunnelser = sanityBegrunnelser)

        // Assert
        assertThat(eøsForordningen883Hjemler).containsOnly("6", "4", "3", "2", "1")
    }
}
