package no.nav.familie.ks.sak.kjerne.brev.hjemler

import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HjemlerKombinatorKtTest {
    @Test
    fun `skal retunere en tom liste om ingen hjemler blir sendt inn for kombinering`() {
        // Act
        val kombinerteHjemler =
            kombinerHjemler(
                målform = Målform.NB,
                separasjonsavtaleStorbritanniaHjemler = emptyList(),
                ordinæreHjemler = emptyList(),
                eøsForordningen883Hjemler = emptyList(),
                eøsForordningen987Hjemler = emptyList(),
                forvaltningslovenHjemler = emptyList(),
            )

        // Assert
        assertThat(kombinerteHjemler).isEmpty()
    }

    @Test
    fun `skal kombinere hjemler kun separasjonsavtale storbritannia på bokmål`() {
        // Arrange
        val separasjonsavtaleStorbritanniaHjemler = listOf("1", "3", "2")

        // Act
        val kombinerteHjemler =
            kombinerHjemler(
                målform = Målform.NB,
                separasjonsavtaleStorbritanniaHjemler = separasjonsavtaleStorbritanniaHjemler,
                ordinæreHjemler = emptyList(),
                eøsForordningen883Hjemler = emptyList(),
                eøsForordningen987Hjemler = emptyList(),
                forvaltningslovenHjemler = emptyList(),
            )

        // Assert
        assertThat(kombinerteHjemler).containsOnly("Separasjonsavtalen mellom Storbritannia og Norge artikkel 1, 3 og 2")
    }

    @Test
    fun `skal kombinere hjemler kun separasjonsavtale storbritannia på nynorsk`() {
        // Arrange
        val separasjonsavtaleStorbritanniaHjemler = listOf("1", "3", "2")

        // Act
        val kombinerteHjemler =
            kombinerHjemler(
                målform = Målform.NN,
                separasjonsavtaleStorbritanniaHjemler = separasjonsavtaleStorbritanniaHjemler,
                ordinæreHjemler = emptyList(),
                eøsForordningen883Hjemler = emptyList(),
                eøsForordningen987Hjemler = emptyList(),
                forvaltningslovenHjemler = emptyList(),
            )

        // Assert
        assertThat(kombinerteHjemler).containsOnly("Separasjonsavtalen mellom Storbritannia og Noreg artikkel 1, 3 og 2")
    }

    @Test
    fun `skal kombinere kun ordinære hjemler på bokmål`() {
        // Arrange
        val ordinæreHjemler = listOf("1", "3", "2")

        // Act
        val kombinerteHjemler =
            kombinerHjemler(
                målform = Målform.NB,
                separasjonsavtaleStorbritanniaHjemler = emptyList(),
                ordinæreHjemler = ordinæreHjemler,
                eøsForordningen883Hjemler = emptyList(),
                eøsForordningen987Hjemler = emptyList(),
                forvaltningslovenHjemler = emptyList(),
            )

        // Assert
        assertThat(kombinerteHjemler).containsOnly("kontantstøtteloven §§ 1, 3 og 2")
    }

    @Test
    fun `skal kombinere kun ordinære hjemler på nynorsk`() {
        // Arrange
        val ordinæreHjemler = listOf("1", "3", "2")

        // Act
        val kombinerteHjemler =
            kombinerHjemler(
                målform = Målform.NN,
                separasjonsavtaleStorbritanniaHjemler = emptyList(),
                ordinæreHjemler = ordinæreHjemler,
                eøsForordningen883Hjemler = emptyList(),
                eøsForordningen987Hjemler = emptyList(),
                forvaltningslovenHjemler = emptyList(),
            )

        // Assert
        assertThat(kombinerteHjemler).containsOnly("kontantstøttelova §§ 1, 3 og 2")
    }

    @Test
    fun `skal kombinere kun eøs forordningen 883 hjemler på bokmål`() {
        // Arrange
        val eøsForordningen883Hjemler = listOf("1", "3", "2")

        // Act
        val kombinerteHjemler =
            kombinerHjemler(
                målform = Målform.NB,
                separasjonsavtaleStorbritanniaHjemler = emptyList(),
                ordinæreHjemler = emptyList(),
                eøsForordningen883Hjemler = eøsForordningen883Hjemler,
                eøsForordningen987Hjemler = emptyList(),
                forvaltningslovenHjemler = emptyList(),
            )

        // Assert
        assertThat(kombinerteHjemler).containsOnly("EØS-forordning 883/2004 artikkel 1, 3 og 2")
    }

    @Test
    fun `skal kombinere kun eøs forordningen 883 hjemler på nynorsk`() {
        // Arrange
        val eøsForordningen883Hjemler = listOf("1", "3", "2")

        // Act
        val kombinerteHjemler =
            kombinerHjemler(
                målform = Målform.NN,
                separasjonsavtaleStorbritanniaHjemler = emptyList(),
                ordinæreHjemler = emptyList(),
                eøsForordningen883Hjemler = eøsForordningen883Hjemler,
                eøsForordningen987Hjemler = emptyList(),
                forvaltningslovenHjemler = emptyList(),
            )

        // Assert
        assertThat(kombinerteHjemler).containsOnly("EØS-forordning 883/2004 artikkel 1, 3 og 2")
    }

    @Test
    fun `skal kombinere kun eøs forordningen 987 hjemler på bokmål`() {
        // Arrange
        val eøsForordningen987Hjemler = listOf("1", "3", "2")

        // Act
        val kombinerteHjemler =
            kombinerHjemler(
                målform = Målform.NB,
                separasjonsavtaleStorbritanniaHjemler = emptyList(),
                ordinæreHjemler = emptyList(),
                eøsForordningen883Hjemler = emptyList(),
                eøsForordningen987Hjemler = eøsForordningen987Hjemler,
                forvaltningslovenHjemler = emptyList(),
            )

        // Assert
        assertThat(kombinerteHjemler).containsOnly("EØS-forordning 987/2009 artikkel 1, 3 og 2")
    }

    @Test
    fun `skal kombinere kun eøs forordningen 987 hjemler på nynorsk`() {
        // Arrange
        val eøsForordningen987Hjemler = listOf("1", "3", "2")

        // Act
        val kombinerteHjemler =
            kombinerHjemler(
                målform = Målform.NN,
                separasjonsavtaleStorbritanniaHjemler = emptyList(),
                ordinæreHjemler = emptyList(),
                eøsForordningen883Hjemler = emptyList(),
                eøsForordningen987Hjemler = eøsForordningen987Hjemler,
                forvaltningslovenHjemler = emptyList(),
            )

        // Assert
        assertThat(kombinerteHjemler).containsOnly("EØS-forordning 987/2009 artikkel 1, 3 og 2")
    }

    @Test
    fun `skal kombinere kun forvaltningsloven hjemler på bokmål`() {
        // Arrange
        val forvaltningslovenHjemler = listOf("1", "3", "2")

        // Act
        val kombinerteHjemler =
            kombinerHjemler(
                målform = Målform.NB,
                separasjonsavtaleStorbritanniaHjemler = emptyList(),
                ordinæreHjemler = emptyList(),
                eøsForordningen883Hjemler = emptyList(),
                eøsForordningen987Hjemler = emptyList(),
                forvaltningslovenHjemler = forvaltningslovenHjemler,
            )

        // Assert
        assertThat(kombinerteHjemler).containsOnly("forvaltningsloven §§ 1, 3 og 2")
    }

    @Test
    fun `skal kombinere kun forvaltningsloven hjemler på nynorsk`() {
        // Arrange
        val forvaltningslovenHjemler = listOf("1", "3", "2")

        // Act
        val kombinerteHjemler =
            kombinerHjemler(
                målform = Målform.NN,
                separasjonsavtaleStorbritanniaHjemler = emptyList(),
                ordinæreHjemler = emptyList(),
                eøsForordningen883Hjemler = emptyList(),
                eøsForordningen987Hjemler = emptyList(),
                forvaltningslovenHjemler = forvaltningslovenHjemler,
            )

        // Assert
        assertThat(kombinerteHjemler).containsOnly("forvaltningslova §§ 1, 3 og 2")
    }

    @Test
    fun `skal kombinere alle hjemler på bokmål`() {
        // Arrange
        val separasjonsavtaleStorbritanniaHjemler = listOf("1", "3", "12")
        val ordinæreHjemler = listOf("12")
        val eøsForordningen883Hjemler = listOf("6")
        val eøsForordningen987Hjemler = listOf("9000")
        val forvaltningslovenHjemler = listOf("1337", "322", "644")

        // Act
        val kombinerteHjemler =
            kombinerHjemler(
                målform = Målform.NB,
                separasjonsavtaleStorbritanniaHjemler = separasjonsavtaleStorbritanniaHjemler,
                ordinæreHjemler = ordinæreHjemler,
                eøsForordningen883Hjemler = eøsForordningen883Hjemler,
                eøsForordningen987Hjemler = eøsForordningen987Hjemler,
                forvaltningslovenHjemler = forvaltningslovenHjemler,
            )

        // Assert
        assertThat(kombinerteHjemler).containsExactly(
            "Separasjonsavtalen mellom Storbritannia og Norge artikkel 1, 3 og 12",
            "kontantstøtteloven § 12",
            "EØS-forordning 883/2004 artikkel 6",
            "EØS-forordning 987/2009 artikkel 9000",
            "forvaltningsloven §§ 1337, 322 og 644",
        )
    }

    @Test
    fun `skal kombinere alle hjemler på nynorsk`() {
        // Arrange
        val separasjonsavtaleStorbritanniaHjemler = listOf("1", "3", "12")
        val ordinæreHjemler = listOf("12")
        val eøsForordningen883Hjemler = listOf("6")
        val eøsForordningen987Hjemler = listOf("9000")
        val forvaltningslovenHjemler = listOf("1337", "322", "644")

        // Act
        val kombinerteHjemler =
            kombinerHjemler(
                målform = Målform.NN,
                separasjonsavtaleStorbritanniaHjemler = separasjonsavtaleStorbritanniaHjemler,
                ordinæreHjemler = ordinæreHjemler,
                eøsForordningen883Hjemler = eøsForordningen883Hjemler,
                eøsForordningen987Hjemler = eøsForordningen987Hjemler,
                forvaltningslovenHjemler = forvaltningslovenHjemler,
            )

        // Assert
        assertThat(kombinerteHjemler).containsExactly(
            "Separasjonsavtalen mellom Storbritannia og Noreg artikkel 1, 3 og 12",
            "kontantstøttelova § 12",
            "EØS-forordning 883/2004 artikkel 6",
            "EØS-forordning 987/2009 artikkel 9000",
            "forvaltningslova §§ 1337, 322 og 644",
        )
    }
}
