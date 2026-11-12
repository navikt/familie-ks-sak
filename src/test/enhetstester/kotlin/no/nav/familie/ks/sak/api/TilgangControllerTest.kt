package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.ks.sak.api.dto.TilgangRequestDto
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.sikkerhet.PersonTilgang
import no.nav.familie.ks.sak.sikkerhet.PersonTilgangService
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilgangControllerTest {
    private val personOpplysningerService = mockk<PersonopplysningerService>()
    private val personidentService = mockk<PersonidentService>()
    private val personTilgangService = mockk<PersonTilgangService>()
    private val tilgangService = mockk<TilgangService>()
    private val tilgangController =
        TilgangController(
            personOpplysningerService = personOpplysningerService,
            personidentService = personidentService,
            personTilgangService = personTilgangService,
            tilgangService = tilgangService,
        )

    private val aktør = randomAktør()
    private val fødselsnummer = aktør.aktivFødselsnummer()

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandling(any(), any()) }
        every { personidentService.hentAktør(fødselsnummer) } returns aktør
        every { personOpplysningerService.hentAdressebeskyttelseSomSystembruker(aktør) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT
        every { personTilgangService.sjekkTilgangTilPerson(fødselsnummer) } returns PersonTilgang.medTilgang(fødselsnummer)
    }

    @Test
    fun `skal kreve veilederrolle`() {
        // Act
        val respons = tilgangController.hentTilgangOgDiskresjonskode(TilgangRequestDto(brukerIdent = fødselsnummer))

        // Assert
        verify(exactly = 1) { tilgangService.validerTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Sjekke tilgang til person") }
        assertThat(respons.body?.data?.saksbehandlerHarTilgang).isTrue()
    }

    @Test
    fun `skal ikke sjekke tilgang når kaller mangler veilederrolle`() {
        // Arrange
        every {
            tilgangService.validerTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Sjekke tilgang til person")
        } throws RolleTilgangskontrollFeil("Krever VEILEDER")

        // Act & Assert
        assertThrows<RolleTilgangskontrollFeil> { tilgangController.hentTilgangOgDiskresjonskode(TilgangRequestDto(brukerIdent = fødselsnummer)) }

        verify(exactly = 0) { personOpplysningerService.hentAdressebeskyttelseSomSystembruker(any()) }
    }
}
