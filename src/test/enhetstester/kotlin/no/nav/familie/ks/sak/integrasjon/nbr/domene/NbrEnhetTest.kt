package no.nav.familie.ks.sak.integrasjon.nbr.domene

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NbrEnhetTest {
    private val oslokommune = "958935420"

    @Test
    fun `finnBydelOrgNummer skal finne bydel for privat barnehage via administrativ struktur`() {
        // Arrange
        val enhet =
            lagEnhet(
                organisasjonsnummer = "975291405",
                lagRelasjon("971179686", "Oslo kommune bydel 4 St. Hanshaugen", "10", "Administrativ struktur"),
                lagRelasjon("971272643", "Stiftelsen Kanvas", "6", "Eierstruktur"),
                lagRelasjon(oslokommune, "Oslo kommune", "11", "Tilknytningsstruktur"),
            )

        // Act
        val bydelOrgNummer = enhet.finnBydelOrgNummer()

        // Assert
        assertThat(bydelOrgNummer).isEqualTo(OsloBydel.ST_HANSHAUGEN.organisasjonsnummer)
    }

    @Test
    fun `finnBydelOrgNummer skal finne bydel for kommunal barnehage som peker på bydelen med flere relasjonstyper`() {
        // Arrange
        val enhet =
            lagEnhet(
                organisasjonsnummer = "975316661",
                lagRelasjon("970145311", "Oslo kommune bydel 7 Vestre Aker", "10", "Administrativ struktur"),
                lagRelasjon("970145311", "Oslo kommune bydel 7 Vestre Aker", "11", "Tilknytningsstruktur"),
                lagRelasjon(oslokommune, "Oslo kommune", "6", "Eierstruktur"),
            )

        // Act
        val bydelOrgNummer = enhet.finnBydelOrgNummer()

        // Assert
        assertThat(bydelOrgNummer).isEqualTo(OsloBydel.VESTRE_AKER.organisasjonsnummer)
    }

    @Test
    fun `finnBydelOrgNummer skal returnere null når ingen relasjon peker på en bydel`() {
        // Arrange
        val enhet =
            lagEnhet(
                organisasjonsnummer = "931416456",
                lagRelasjon(oslokommune, "Oslo kommune", "10", "Administrativ struktur"),
            )

        // Act
        val bydelOrgNummer = enhet.finnBydelOrgNummer()

        // Assert
        assertThat(bydelOrgNummer).isNull()
    }

    @Test
    fun `finnBydelOrgNummer skal ignorere bydel som bare er knyttet via annen relasjonstype enn administrativ struktur`() {
        // Arrange
        val enhet =
            lagEnhet(
                organisasjonsnummer = "975291405",
                lagRelasjon("971179686", "Oslo kommune bydel 4 St. Hanshaugen", "6", "Eierstruktur"),
            )

        // Act
        val bydelOrgNummer = enhet.finnBydelOrgNummer()

        // Assert
        assertThat(bydelOrgNummer).isNull()
    }

    @Test
    fun `finnBydelOrgNummer skal returnere null når enheten peker på flere bydeler`() {
        // Arrange
        val enhet =
            lagEnhet(
                organisasjonsnummer = "975291405",
                lagRelasjon("971179686", "Oslo kommune bydel 4 St. Hanshaugen", "10", "Administrativ struktur"),
                lagRelasjon("874778702", "Oslo kommune bydel 5 Frogner", "10", "Administrativ struktur"),
            )

        // Act
        val bydelOrgNummer = enhet.finnBydelOrgNummer()

        // Assert
        assertThat(bydelOrgNummer).isNull()
    }

    private fun lagEnhet(
        organisasjonsnummer: String,
        vararg relasjoner: NbrRelasjon,
    ) = NbrEnhet(
        organisasjonsnummer = organisasjonsnummer,
        navn = "Barnehage",
        erAktiv = true,
        foreldreRelasjoner = relasjoner.toList(),
    )

    private fun lagRelasjon(
        organisasjonsnummer: String,
        navn: String,
        relasjonstypeId: String,
        relasjonstypeNavn: String,
    ) = NbrRelasjon(
        enhet = NbrRelatertEnhet(organisasjonsnummer = organisasjonsnummer, navn = navn),
        relasjonstype = NbrRelasjonstype(id = relasjonstypeId, navn = relasjonstypeNavn),
    )
}
