package no.nav.familie.ks.sak.kjerne.brev.hjemler

import no.nav.familie.ks.sak.data.lagSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EØSForordningen987HjemlerKtTest {
    @Test
    fun `skal retunere en tom liste om sanity EØS begrunnelsene er en tom liste og refusjon EØS hjemmel ikke skal være med i brevet`() {
        // Act
        val hjemlerForEøsForordningen987 =
            utledEØSForordningen987Hjemler(
                sanityBegrunnelser = emptyList(),
                refusjonEøsHjemmelSkalMedIBrev = false,
            )

        // Assert
        assertThat(hjemlerForEøsForordningen987).isEmpty()
    }

    @Test
    fun `skal retunere liste med refusjon EØS hjemmel som eneste innslag da sanity EØS begrunnelser er en tom liste`() {
        // Act
        val hjemlerForEøsForordningen987 =
            utledEØSForordningen987Hjemler(
                sanityBegrunnelser = emptyList(),
                refusjonEøsHjemmelSkalMedIBrev = true,
            )

        // Assert
        assertThat(hjemlerForEøsForordningen987).containsOnly("60")
    }

    @Test
    fun `skal retunere en liste av hjemler uten refusjon EØS hjemmel `() {
        // Arrange
        val sanityEøsBegrunnelse1 =
            lagSanityBegrunnelse(
                apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD.name,
                hjemlerEøsForordningen987 = listOf("1", "3", "5"),
            )
        val sanityEøsBegrunnelse2 =
            lagSanityBegrunnelse(
                apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.name,
                hjemlerEøsForordningen987 = listOf("4", "3", "6"),
            )

        val sanityEøsBegrunnelser =
            listOf(
                sanityEøsBegrunnelse1,
                sanityEøsBegrunnelse2,
            )

        // Act
        val hjemlerForEøsForordningen987 =
            utledEØSForordningen987Hjemler(
                sanityBegrunnelser = sanityEøsBegrunnelser,
                refusjonEøsHjemmelSkalMedIBrev = false,
            )

        // Assert
        assertThat(hjemlerForEøsForordningen987).containsOnly("1", "3", "5", "4", "6")
    }

    @Test
    fun `skal retunere en liste av hjemler med refusjon EØS hjemmel `() {
        // Arrange
        val sanityEøsBegrunnelse1 =
            lagSanityBegrunnelse(
                apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD.name,
                hjemlerEøsForordningen987 = listOf("1", "3", "5"),
            )
        val sanityEøsBegrunnelse2 =
            lagSanityBegrunnelse(
                apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.name,
                hjemlerEøsForordningen987 = listOf("4", "3", "6"),
            )

        val sanityEøsBegrunnelser =
            listOf(
                sanityEøsBegrunnelse1,
                sanityEøsBegrunnelse2,
            )

        // Act
        val hjemlerForEøsForordningen987 =
            utledEØSForordningen987Hjemler(
                sanityBegrunnelser = sanityEøsBegrunnelser,
                refusjonEøsHjemmelSkalMedIBrev = true,
            )

        // Assert
        assertThat(hjemlerForEøsForordningen987).containsOnly("1", "3", "5", "4", "6", "60")
    }
}
