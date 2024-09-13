package no.nav.familie.ks.sak.integrasjon.oppgave

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.data.lagEnhet
import no.nav.familie.ks.sak.data.lagEnhetTilgang
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val MIDLERTIDIG_ENHET_4863 = "4863"
private const val VIKAFOSSEN_ENHET_2103 = "2103"

class NavIdentOgEnhetsnummerServiceTest {
    private val mockedArbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository = mockk()
    private val mockedIntegrasjonClient: IntegrasjonClient = mockk()
    private val navIdentOgEnhetsnummerService: NavIdentOgEnhetsnummerService =
        NavIdentOgEnhetsnummerService(
            arbeidsfordelingPåBehandlingRepository = mockedArbeidsfordelingPåBehandlingRepository,
            integrasjonClient = mockedIntegrasjonClient,
        )

    @Test
    fun `skal kaste feil om arbeidsfordeling returnerer midlertidig enhet 4863 og NAV-ident er null`() {
        // Arrange
        val behandlingId = 1L

        every {
            mockedArbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
            )
        } returns
            lagArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
                behandlendeEnhetId = MIDLERTIDIG_ENHET_4863,
            )

        // Act & assert
        val exception =
            assertThrows<Feil> {
                navIdentOgEnhetsnummerService.hentNavIdentOgEnhetsnummer(
                    behandlingId = behandlingId,
                    navIdent = null,
                )
            }
        assertThat(exception.message).isEqualTo("Kan ikke sette midlertidig enhet 4863 om man mangler nav-ident")
    }

    @Test
    fun `skal kaste feil om arbeidsfordeling returnerer midlertidig enhet 4863 og NAV-ident ikke har tilgang til noen andre enheter enn 4863 og 2103`() {
        // Arrange
        val behandlingId = 1L
        val navIdent = "1"
        val enhetNavIdentHarTilgangTil1 = MIDLERTIDIG_ENHET_4863
        val enhetNavIdentHarTilgangTil2 = VIKAFOSSEN_ENHET_2103

        every {
            mockedArbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
            )
        } returns
            lagArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
                behandlendeEnhetId = MIDLERTIDIG_ENHET_4863,
            )

        every {
            mockedIntegrasjonClient.hentEnhetTilgang(
                navIdent = navIdent,
            )
        } returns
            lagEnhetTilgang(
                enheter =
                    listOf(
                        lagEnhet(
                            enhetsnummer = enhetNavIdentHarTilgangTil1,
                        ),
                        lagEnhet(
                            enhetsnummer = enhetNavIdentHarTilgangTil2,
                        ),
                    ),
            )

        // Act & assert
        val exception =
            assertThrows<Feil> {
                navIdentOgEnhetsnummerService.hentNavIdentOgEnhetsnummer(
                    behandlingId = behandlingId,
                    navIdent = navIdent,
                )
            }
        assertThat(exception.message).isEqualTo("Fant ingen passende enhetsnummer for nav-ident $navIdent")
    }

    @Test
    fun `skal returnere NAV-ident og første enhetsnummer som NAV-identen har tilgang til når arbeidsfordeling returnerer midlertidig enhet 4863`() {
        // Arrange
        val behandlingId = 1L
        val navIdent = "1"
        val enhetNavIdentHarTilgangTil1 = MIDLERTIDIG_ENHET_4863
        val enhetNavIdentHarTilgangTil2 = VIKAFOSSEN_ENHET_2103
        val enhetNavIdentHarTilgangTil3 = "1234"
        val enhetNavIdentHarTilgangTil4 = "4321"

        every {
            mockedArbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
            )
        } returns
            lagArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
                behandlendeEnhetId = MIDLERTIDIG_ENHET_4863,
            )

        every {
            mockedIntegrasjonClient.hentEnhetTilgang(
                navIdent = navIdent,
            )
        } returns
            lagEnhetTilgang(
                enheter =
                    listOf(
                        lagEnhet(
                            enhetsnummer = enhetNavIdentHarTilgangTil1,
                        ),
                        lagEnhet(
                            enhetsnummer = enhetNavIdentHarTilgangTil2,
                        ),
                        lagEnhet(
                            enhetsnummer = enhetNavIdentHarTilgangTil3,
                        ),
                        lagEnhet(
                            enhetsnummer = enhetNavIdentHarTilgangTil4,
                        ),
                    ),
            )

        // Act
        val navIdentOgEnhetsnummer =
            navIdentOgEnhetsnummerService.hentNavIdentOgEnhetsnummer(
                behandlingId = behandlingId,
                navIdent = navIdent,
            )

        // Assert
        assertThat(navIdentOgEnhetsnummer.navIdent).isEqualTo(navIdent)
        assertThat(navIdentOgEnhetsnummer.enhetsnummer).isEqualTo(enhetNavIdentHarTilgangTil3)
    }
}
