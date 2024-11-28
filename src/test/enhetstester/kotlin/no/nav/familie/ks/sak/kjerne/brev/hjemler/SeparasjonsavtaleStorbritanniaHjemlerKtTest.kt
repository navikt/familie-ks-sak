package no.nav.familie.ks.sak.kjerne.brev.hjemler

import no.nav.familie.ks.sak.data.lagSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SeparasjonsavtaleStorbritanniaHjemlerKtTest {
    @Test
    fun `skal returnere en tom liste når sanity eøs begrunnelser er tom`() {
        // Act
        val seprasjonsavtaleStorbritanniaHjemler =
            utledSeprasjonsavtaleStorbritanniaHjemler(
                sanityBegrunnelser = emptyList(),
            )

        // Assert
        assertThat(seprasjonsavtaleStorbritanniaHjemler).isEmpty()
    }

    @Test
    fun `skal utlede seprasjonsavtale for storbritannia hjemler`() {
        // Arrange
        val sanityEøsBegrunnelse =
            lagSanityBegrunnelse(
                apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD.name,
                hjemlerSeperasjonsavtalenStorbritannina = listOf("1", "4", "3", "4"),
            )

        val sanityEØSBegrunnelser = listOf(sanityEøsBegrunnelse)

        // Act
        val seprasjonsavtaleStorbritanniaHjemler =
            utledSeprasjonsavtaleStorbritanniaHjemler(
                sanityBegrunnelser = sanityEØSBegrunnelser,
            )

        // Assert
        assertThat(seprasjonsavtaleStorbritanniaHjemler).containsOnly("1", "4", "3")
    }
}
