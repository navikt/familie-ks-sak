package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene.SøknadGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BarnMedOpplysningerDtoTest {
    @Test
    fun `skal beholde manueltRegistrert når søknaden lagres ned og hentes opp igjen`() {
        // Arrange
        val søknadDto =
            SøknadDto(
                søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = "12345678910"),
                barnaMedOpplysninger =
                    listOf(
                        BarnMedOpplysningerDto(ident = "01011012345", manueltRegistrert = true),
                        BarnMedOpplysningerDto(ident = "02022012345"),
                    ),
                endringAvOpplysningerBegrunnelse = "",
            )

        // Act
        val lagretSøknadDto = søknadDto.tilSøknadGrunnlag(behandlingId = 1L).tilSøknadDto()

        // Assert
        assertThat(lagretSøknadDto.barnaMedOpplysninger.single { it.ident == "01011012345" }.manueltRegistrert).isTrue()
        assertThat(lagretSøknadDto.barnaMedOpplysninger.single { it.ident == "02022012345" }.manueltRegistrert).isFalse()
    }

    @Test
    fun `skal defaulte manueltRegistrert til false for allerede lagrede søknader uten feltet`() {
        // Arrange
        val søknadUtenManueltRegistrert =
            """
            {
              "søkerMedOpplysninger": { "ident": "12345678910", "målform": "NB" },
              "barnaMedOpplysninger": [
                { "ident": "01011012345", "navn": "Barn", "inkludertISøknaden": true, "erFolkeregistrert": true }
              ],
              "endringAvOpplysningerBegrunnelse": ""
            }
            """.trimIndent()
        val søknadGrunnlag = SøknadGrunnlag(behandlingId = 1L, søknad = søknadUtenManueltRegistrert)

        // Act
        val søknadDto = søknadGrunnlag.tilSøknadDto()

        // Assert
        assertThat(søknadDto.barnaMedOpplysninger.single().manueltRegistrert).isFalse()
    }
}
