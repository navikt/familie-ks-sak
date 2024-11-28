package no.nav.familie.ks.sak.kjerne.brev.hjemler

import no.nav.familie.ks.sak.data.lagSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrdinæreHjemlerKtTest {
    @Test
    fun `skal utlede en tom liste om ingen hjemler skal inkluderes`() {
        // Act
        val ordinæreHjemler =
            utledOrdinæreHjemler(
                sanityBegrunnelser = emptyList(),
                opplysningspliktHjemlerSkalMedIBrev = false,
            )

        // Assert
        assertThat(ordinæreHjemler).isEmpty()
    }

    @Test
    fun `skal utlede ordinære hjemler for sanity begrunnelser`() {
        // Arrange
        val sanityBegrunnelse = lagSanityBegrunnelse(apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE.name, hjemler = listOf("1", "3", "2"))

        val sanityBegrunnelser = listOf(sanityBegrunnelse)

        // Act
        val ordinæreHjemler =
            utledOrdinæreHjemler(
                sanityBegrunnelser = sanityBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false,
            )

        // Assert
        assertThat(ordinæreHjemler).containsOnly("1", "3", "2")
    }

    @Test
    fun `skal utlede ordinære hjemler for sanity eøs begrunnelser`() {
        // Arrange
        val sanityBegrunnelse = lagSanityBegrunnelse(apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD.name, hjemler = listOf("1", "3", "2"))

        // Act
        val ordinæreHjemler =
            utledOrdinæreHjemler(
                sanityBegrunnelser = listOf(sanityBegrunnelse),
                opplysningspliktHjemlerSkalMedIBrev = false,
            )

        // Assert
        assertThat(ordinæreHjemler).containsOnly("1", "3", "2")
    }

    @Test
    fun `skal utlede opplysningsplikt hjemmel`() {
        // Act
        val ordinæreHjemler =
            utledOrdinæreHjemler(
                sanityBegrunnelser = emptyList(),
                opplysningspliktHjemlerSkalMedIBrev = true,
            )

        // Assert
        assertThat(ordinæreHjemler).containsOnly("13", "16")
    }

    @Test
    fun `skal utlede alle ordinære hjemler`() {
        // Arrange
        val sanityBegrunnelse = lagSanityBegrunnelse(apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE.name, hjemler = listOf("1", "3", "2"))

        val sanityEøsBegrunnelse = lagSanityBegrunnelse(apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD.name, hjemler = listOf("1", "6", "2"))

        val sanityBegrunnelser = listOf(sanityBegrunnelse, sanityEøsBegrunnelse)

        // Act
        val ordinæreHjemler =
            utledOrdinæreHjemler(
                sanityBegrunnelser = sanityBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = true,
            )

        // Assert
        assertThat(ordinæreHjemler).containsOnly("1", "2", "3", "6", "13", "16")
    }
}
