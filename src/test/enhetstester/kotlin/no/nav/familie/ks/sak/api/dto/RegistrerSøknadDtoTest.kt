package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.data.lagRegistrerSøknadDto
import no.nav.familie.ks.sak.data.randomFnr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RegistrerSøknadDtoTest {
    @Test
    fun `skal kaste feil når et barn som er valgt i søknaden har ugyldig ident`() {
        // Arrange
        val søknad =
            lagRegistrerSøknadDto().søknad.copy(
                barnaMedOpplysninger =
                    listOf(
                        BarnMedOpplysningerDto(ident = randomFnr()),
                        BarnMedOpplysningerDto(ident = "12345678901 suid=Z999999"),
                    ),
            )

        // Act & Assert
        assertThrows<IllegalStateException> {
            RegistrerSøknadDto(søknad = søknad, bekreftEndringerViaFrontend = true)
        }
    }

    @Test
    fun `skal kun validere identen til barn som er valgt i søknaden`() {
        // Arrange
        val valgtBarnIdent = randomFnr()
        val søknad =
            lagRegistrerSøknadDto().søknad.copy(
                barnaMedOpplysninger =
                    listOf(
                        BarnMedOpplysningerDto(ident = valgtBarnIdent),
                        BarnMedOpplysningerDto(ident = "ikke folkeregistrert", erFolkeregistrert = false),
                        BarnMedOpplysningerDto(ident = "ikke valgt", inkludertISøknaden = false),
                    ),
            )

        // Act
        val registrerSøknadDto = RegistrerSøknadDto(søknad = søknad, bekreftEndringerViaFrontend = true)

        // Assert
        assertThat(registrerSøknadDto.søknad.hentIdenterTilValgteBarn()).containsExactly(valgtBarnIdent)
    }
}
