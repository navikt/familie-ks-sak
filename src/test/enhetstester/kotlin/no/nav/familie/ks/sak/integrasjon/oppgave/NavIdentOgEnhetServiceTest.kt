package no.nav.familie.ks.sak.integrasjon.oppgave

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.data.lagEnhet
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NavIdentOgEnhetServiceTest {
    private val mockedIntegrasjonClient: IntegrasjonClient = mockk()
    private val navIdentOgEnhetService: NavIdentOgEnhetService =
        NavIdentOgEnhetService(
            integrasjonClient = mockedIntegrasjonClient,
        )

    @Nested
    inner class HentNavIdentOgEnhetsnummerTest {
        @Test
        fun `skal kaste feil om arbeidsfordeling returnerer midlertidig enhet 4863 og NAV-ident er null`() {
            // Arrange
            val behandlingId = 1L

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    navIdentOgEnhetService.hentNavIdentOgEnhet(
                        arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                        navIdent = null,
                    )
                }
            assertThat(exception.message).isEqualTo("Kan ikke sette midlertidig enhet 4863 om man mangler NAV-ident")
        }

        @Test
        fun `skal kaste feil om arbeidsfordeling returnerer midlertidig enhet 4863 og NAV-ident ikke har tilgang til noen andre enheter enn 4863 og 2103`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")
            val enhetNavIdentHarTilgangTil1 = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer
            val enhetNavIdentHarTilgangTil2 = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil1,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil2,
                    ),
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    navIdentOgEnhetService.hentNavIdentOgEnhet(
                        arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                        navIdent = navIdent,
                    )
                }
            assertThat(exception.message).isEqualTo("Fant ingen passende enhetsnummer for nav-ident $navIdent")
        }

        @Test
        fun `skal returnere NAV-ident og første enhetsnummer som NAV-identen har tilgang til når arbeidsfordeling returnerer midlertidig enhet 4863`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")
            val enhetNavIdentHarTilgangTil1 = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer
            val enhetNavIdentHarTilgangTil2 = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer
            val enhetNavIdentHarTilgangTil3 = KontantstøtteEnhet.OSLO.enhetsnummer
            val enhetNavIdentHarTilgangTil4 = KontantstøtteEnhet.DRAMMEN.enhetsnummer

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
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
                )

            // Act
            val navIdentOgEnhet =
                navIdentOgEnhetService.hentNavIdentOgEnhet(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(navIdentOgEnhet.navIdent).isEqualTo(navIdent)
            assertThat(navIdentOgEnhet.enhetsnummer).isEqualTo(enhetNavIdentHarTilgangTil3)
        }

        @Test
        fun `skal kaste feil hvis arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident er null`() {
            // Arrange
            val behandlingId = 1L

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    navIdentOgEnhetService.hentNavIdentOgEnhet(
                        arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                        navIdent = null,
                    )
                }
            assertThat(exception.message).isEqualTo("Kan ikke sette Vikafossen enhet 2103 om man mangler NAV-ident")
        }

        @Test
        fun `skal returnere Vikafossen 2103 uten NAV-ident om arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident ikke har tilgang til Vikafossen 2103`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")
            val enhetNavIdentHarTilgangTil1 = KontantstøtteEnhet.STEINKJER.enhetsnummer
            val enhetNavIdentHarTilgangTil2 = KontantstøtteEnhet.VADSØ.enhetsnummer

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil1,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil2,
                    ),
                )

            // Act
            val navIdentOgEnhet =
                navIdentOgEnhetService.hentNavIdentOgEnhet(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(navIdentOgEnhet.navIdent).isNull()
            assertThat(navIdentOgEnhet.enhetsnummer).isEqualTo(KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer)
        }

        @Test
        fun `skal returnere Vikafossen 2103 med NAV-ident om arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident har tilgang til Vikafossen 2103`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")
            val enhetNavIdentHarTilgangTil1 = KontantstøtteEnhet.BERGEN.enhetsnummer
            val enhetNavIdentHarTilgangTil2 = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil1,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil2,
                    ),
                )

            // Act
            val navIdentOgEnhet =
                navIdentOgEnhetService.hentNavIdentOgEnhet(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(navIdentOgEnhet.navIdent).isEqualTo(navIdent)
            assertThat(navIdentOgEnhet.enhetsnummer).isEqualTo(enhetNavIdentHarTilgangTil2)
        }

        @Test
        fun `skal returnere behandlendeEnhetId uten NAV-ident om arbeidsfordeling ikke returnere 2103 eller 4863 og NAV-ident er null`() {
            // Arrange
            val behandlingId = 1L

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.STEINKJER.enhetsnummer,
                )

            // Act
            val navIdentOgEnhetsnummer =
                navIdentOgEnhetService.hentNavIdentOgEnhet(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = null,
                )

            // Assert
            assertThat(navIdentOgEnhetsnummer.navIdent).isNull()
            assertThat(navIdentOgEnhetsnummer.enhetsnummer).isEqualTo(KontantstøtteEnhet.STEINKJER.enhetsnummer)
        }

        @Test
        fun `skal kaste feil om arbeidsfordeling ikke returnerer 2103 eller 4863 og NAV-ident ikke har tilgang til noen enheter`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")
            val enhetsnummerForEnhetNavIdentHarTilgangTil1 = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer
            val enhetsnummerForEnhetNavIdentHarTilgangTil2 = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.DRAMMEN.enhetsnummer,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = enhetsnummerForEnhetNavIdentHarTilgangTil1,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetsnummerForEnhetNavIdentHarTilgangTil2,
                    ),
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    navIdentOgEnhetService.hentNavIdentOgEnhet(
                        arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                        navIdent = navIdent,
                    )
                }
            assertThat(exception.message).isEqualTo("Fant ingen passende enhetsnummer for NAV-ident $navIdent")
        }

        @Test
        fun `skal returnere NAV-ident og første enhet NAV-ident har tilgang om arbeidsfordeling ikke returnere 2103 eller 4863 og NAV-ident ikke har tilgang arbeidsfordeling enheten`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")

            val enhetsnummerForEnhetNavIdentHarTilgangTil1 = KontantstøtteEnhet.OSLO.enhetsnummer
            val enhetsnummerForEnhetNavIdentHarTilgangTil2 = KontantstøtteEnhet.DRAMMEN.enhetsnummer

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.STEINKJER.enhetsnummer,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = enhetsnummerForEnhetNavIdentHarTilgangTil1,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetsnummerForEnhetNavIdentHarTilgangTil2,
                    ),
                )

            // Act
            val navIdentOgEnhetsnummer =
                navIdentOgEnhetService.hentNavIdentOgEnhet(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(navIdentOgEnhetsnummer.navIdent).isEqualTo(navIdent)
            assertThat(navIdentOgEnhetsnummer.enhetsnummer).isEqualTo(enhetsnummerForEnhetNavIdentHarTilgangTil1)
        }

        @Test
        fun `skal returnere NAV-ident og arbeidsfordeling enhetsnummer om arbeidsfordeling ikke returnere 2103 eller 4863 og NAV-ident har tilgang arbeidsfordeling enheten`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")

            val arbeidsfordelingEnhet = KontantstøtteEnhet.OSLO.enhetsnummer

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = arbeidsfordelingEnhet,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    ),
                    lagEnhet(
                        enhetsnummer = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer,
                    ),
                    lagEnhet(
                        enhetsnummer = arbeidsfordelingEnhet,
                    ),
                )

            // Act
            val navIdentOgEnhetsnummer =
                navIdentOgEnhetService.hentNavIdentOgEnhet(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(navIdentOgEnhetsnummer.navIdent).isEqualTo(navIdent)
            assertThat(navIdentOgEnhetsnummer.enhetsnummer).isEqualTo(arbeidsfordelingEnhet)
        }
    }

    @Nested
    inner class NavIdentOgEnhetTest {
        @Test
        fun `skal kaste exception om enhetsnummer blir satt til mindre enn 4 siffer`() {
            // Act & assert
            val exception =
                assertThrows<IllegalArgumentException> {
                    NavIdentOgEnhet(null, "123", "Enhet 123")
                }
            assertThat(exception.message).isEqualTo("Enhetsnummer må være 4 siffer")
        }

        @Test
        fun `skal kaste exception om enhetsnummer blir satt til mer enn 4 siffer`() {
            // Act & assert
            val exception =
                assertThrows<IllegalArgumentException> {
                    NavIdentOgEnhet(null, "12345", "Enhet 12345")
                }
            assertThat(exception.message).isEqualTo("Enhetsnummer må være 4 siffer")
        }
    }
}
