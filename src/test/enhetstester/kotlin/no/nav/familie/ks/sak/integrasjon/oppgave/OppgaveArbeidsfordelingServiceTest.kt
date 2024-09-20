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

class OppgaveArbeidsfordelingServiceTest {
    private val mockedIntegrasjonClient: IntegrasjonClient = mockk()
    private val oppgaveArbeidsfordelingService: OppgaveArbeidsfordelingService =
        OppgaveArbeidsfordelingService(
            integrasjonClient = mockedIntegrasjonClient,
        )

    @Nested
    inner class FinnArbeidsfordelingForOppgaveTest {
        @Test
        fun `skal kaste feil om arbeidsfordeling returnerer midlertidig enhet 4863 og NAV-ident er null`() {
            // Arrange
            val behandlingId = 1L

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    behandlendeEnhetNavn = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                        arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                        navIdent = null,
                    )
                }
            assertThat(exception.message).isEqualTo("Kan ikke sette ${KontantstøtteEnhet.MIDLERTIDIG_ENHET} om man mangler NAV-ident")
        }

        @Test
        fun `skal kaste feil om arbeidsfordeling returnerer midlertidig enhet 4863 og NAV-ident ikke har tilgang til noen andre enheter enn 4863 og 2103`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")
            val enhetNavIdentHarTilgangTil1 = KontantstøtteEnhet.MIDLERTIDIG_ENHET
            val enhetNavIdentHarTilgangTil2 = KontantstøtteEnhet.VIKAFOSSEN

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    behandlendeEnhetNavn = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil1.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil1.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil2.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil2.enhetsnavn,
                    ),
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
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
            val enhetNavIdentHarTilgangTil1 = KontantstøtteEnhet.MIDLERTIDIG_ENHET
            val enhetNavIdentHarTilgangTil2 = KontantstøtteEnhet.VIKAFOSSEN
            val enhetNavIdentHarTilgangTil3 = KontantstøtteEnhet.OSLO
            val enhetNavIdentHarTilgangTil4 = KontantstøtteEnhet.DRAMMEN

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    behandlendeEnhetNavn = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil1.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil1.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil2.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil2.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil3.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil3.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil4.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil4.enhetsnavn,
                    ),
                )

            // Act
            val oppgaveArbeidsfordeling =
                oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(oppgaveArbeidsfordeling.navIdent).isEqualTo(navIdent)
            assertThat(oppgaveArbeidsfordeling.enhetsnummer).isEqualTo(enhetNavIdentHarTilgangTil3.enhetsnummer)
            assertThat(oppgaveArbeidsfordeling.enhetsnavn).isEqualTo(enhetNavIdentHarTilgangTil3.enhetsnavn)
        }

        @Test
        fun `skal kaste feil hvis arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident er null`() {
            // Arrange
            val behandlingId = 1L

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer,
                    behandlendeEnhetNavn = KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                        arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                        navIdent = null,
                    )
                }
            assertThat(exception.message).isEqualTo("Kan ikke sette ${KontantstøtteEnhet.VIKAFOSSEN} om man mangler NAV-ident")
        }

        @Test
        fun `skal returnere Vikafossen 2103 uten NAV-ident om arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident ikke har tilgang til Vikafossen 2103`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")
            val enhetNavIdentHarTilgangTil1 = KontantstøtteEnhet.STEINKJER
            val enhetNavIdentHarTilgangTil2 = KontantstøtteEnhet.VADSØ

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer,
                    behandlendeEnhetNavn = KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil1.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil1.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil2.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil2.enhetsnavn,
                    ),
                )

            // Act
            val oppgaveArbeidsfordeling =
                oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(oppgaveArbeidsfordeling.navIdent).isNull()
            assertThat(oppgaveArbeidsfordeling.enhetsnummer).isEqualTo(KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer)
            assertThat(oppgaveArbeidsfordeling.enhetsnavn).isEqualTo(KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn)
        }

        @Test
        fun `skal returnere Vikafossen 2103 med NAV-ident om arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident har tilgang til Vikafossen 2103`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")
            val enhetNavIdentHarTilgangTil1 = KontantstøtteEnhet.BERGEN
            val enhetNavIdentHarTilgangTil2 = KontantstøtteEnhet.VIKAFOSSEN

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer,
                    behandlendeEnhetNavn = KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil1.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil1.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil2.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil2.enhetsnavn,
                    ),
                )

            // Act
            val oppgaveArbeidsfordeling =
                oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(oppgaveArbeidsfordeling.navIdent).isEqualTo(navIdent)
            assertThat(oppgaveArbeidsfordeling.enhetsnummer).isEqualTo(enhetNavIdentHarTilgangTil2.enhetsnummer)
            assertThat(oppgaveArbeidsfordeling.enhetsnavn).isEqualTo(enhetNavIdentHarTilgangTil2.enhetsnavn)
        }

        @Test
        fun `skal returnere behandlendeEnhetId uten NAV-ident om arbeidsfordeling ikke returnere 2103 eller 4863 og NAV-ident er null`() {
            // Arrange
            val behandlingId = 1L

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.STEINKJER.enhetsnummer,
                    behandlendeEnhetNavn = KontantstøtteEnhet.STEINKJER.enhetsnavn,
                )

            // Act
            val oppgaveArbeidsfordeling =
                oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = null,
                )

            // Assert
            assertThat(oppgaveArbeidsfordeling.navIdent).isNull()
            assertThat(oppgaveArbeidsfordeling.enhetsnummer).isEqualTo(KontantstøtteEnhet.STEINKJER.enhetsnummer)
            assertThat(oppgaveArbeidsfordeling.enhetsnavn).isEqualTo(KontantstøtteEnhet.STEINKJER.enhetsnavn)
        }

        @Test
        fun `skal kaste feil om arbeidsfordeling ikke returnerer 2103 eller 4863 og NAV-ident ikke har tilgang til noen enheter`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")
            val enhetNavIdentHarTilgangTil1 = KontantstøtteEnhet.MIDLERTIDIG_ENHET
            val enhetNavIdentHarTilgangTil2 = KontantstøtteEnhet.VIKAFOSSEN

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.DRAMMEN.enhetsnummer,
                    behandlendeEnhetNavn = KontantstøtteEnhet.DRAMMEN.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil1.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil1.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil2.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil2.enhetsnavn,
                    ),
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
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

            val enhetNavIdentHarTilgangTil1 = KontantstøtteEnhet.OSLO
            val enhetNavIdentHarTilgangTil2 = KontantstøtteEnhet.DRAMMEN

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.STEINKJER.enhetsnummer,
                    behandlendeEnhetNavn = KontantstøtteEnhet.STEINKJER.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil1.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil1.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil2.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil2.enhetsnavn,
                    ),
                )

            // Act
            val oppgaveArbeidsfordeling =
                oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(oppgaveArbeidsfordeling.navIdent).isEqualTo(navIdent)
            assertThat(oppgaveArbeidsfordeling.enhetsnummer).isEqualTo(enhetNavIdentHarTilgangTil1.enhetsnummer)
            assertThat(oppgaveArbeidsfordeling.enhetsnavn).isEqualTo(enhetNavIdentHarTilgangTil1.enhetsnavn)
        }

        @Test
        fun `skal returnere NAV-ident og arbeidsfordeling enhetsnummer om arbeidsfordeling ikke returnere 2103 eller 4863 og NAV-ident har tilgang arbeidsfordeling enheten`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")

            val arbeidsfordelingEnhet = KontantstøtteEnhet.OSLO

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = arbeidsfordelingEnhet.enhetsnummer,
                    behandlendeEnhetNavn = arbeidsfordelingEnhet.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                        enhetsnavn = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer,
                        enhetsnavn = KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = arbeidsfordelingEnhet.enhetsnummer,
                        enhetsnavn = arbeidsfordelingEnhet.enhetsnavn,
                    ),
                )

            // Act
            val oppgaveArbeidsfordeling =
                oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(oppgaveArbeidsfordeling.navIdent).isEqualTo(navIdent)
            assertThat(oppgaveArbeidsfordeling.enhetsnummer).isEqualTo(arbeidsfordelingEnhet.enhetsnummer)
            assertThat(oppgaveArbeidsfordeling.enhetsnavn).isEqualTo(arbeidsfordelingEnhet.enhetsnavn)
        }
    }

    @Nested
    inner class OppgaveArbeidsfordelingTest {
        @Test
        fun `skal kaste exception om enhetsnummer blir satt til mindre enn 4 siffer`() {
            // Act & assert
            val exception =
                assertThrows<IllegalArgumentException> {
                    OppgaveArbeidsfordeling(
                        null,
                        "123",
                        "Enhet 123",
                    )
                }
            assertThat(exception.message).isEqualTo("Enhetsnummer må være 4 siffer")
        }

        @Test
        fun `skal kaste exception om enhetsnummer blir satt til mer enn 4 siffer`() {
            // Act & assert
            val exception =
                assertThrows<IllegalArgumentException> {
                    OppgaveArbeidsfordeling(
                        null,
                        "12345",
                        "Enhet 12345",
                    )
                }
            assertThat(exception.message).isEqualTo("Enhetsnummer må være 4 siffer")
        }
    }
}
