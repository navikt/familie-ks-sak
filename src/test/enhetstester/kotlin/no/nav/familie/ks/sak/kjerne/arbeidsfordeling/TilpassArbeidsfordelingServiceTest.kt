package no.nav.familie.ks.sak.kjerne.arbeidsfordeling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.Arbeidsfordelingsenhet
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilpassArbeidsfordelingServiceTest {
    private val mockedIntegrasjonClient: IntegrasjonClient = mockk()
    private val tilpassArbeidsfordelingService: TilpassArbeidsfordelingService =
        TilpassArbeidsfordelingService(
            integrasjonClient = mockedIntegrasjonClient,
        )

    @Nested
    inner class TilpassArbeidsfordelingsenhetTilSaksbehandlerTest {
        @Test
        fun `skal kaste feil om arbeidsfordeling er midlertidig enhet 4863 og NAV-ident er null`() {
            // Arrange
            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    enhetNavn = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                        arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                        navIdent = null,
                    )
                }
            assertThat(exception.message).isEqualTo("Kan ikke håndtere ${KontantstøtteEnhet.MIDLERTIDIG_ENHET} om man mangler NAV-ident.")
        }

        @Test
        fun `skal kaste feil om arbeidsfordeling er midlertidig enhet 4863 og NAV-ident er systembruker`() {
            // Arrange
            val arbeidsfordelingPåBehandling =
                Arbeidsfordelingsenhet(
                    enhetId = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    enhetNavn = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                        arbeidsfordelingsenhet = arbeidsfordelingPåBehandling,
                        navIdent = NavIdent(SYSTEM_FORKORTELSE),
                    )
                }
            assertThat(exception.message).isEqualTo("Kan ikke håndtere ${KontantstøtteEnhet.MIDLERTIDIG_ENHET} i automatiske behandlinger.")
        }

        @Test
        fun `skal kaste feil om arbeidsfordeling er midlertidig enhet 4863 og NAV-ident ikke har tilgang til noen andre enheter enn 4863 og 2103`() {
            // Arrange
            val navIdent = NavIdent("1")

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    enhetNavn = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns emptyList()

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                        arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                        navIdent = navIdent,
                    )
                }
            assertThat(exception.message).isEqualTo("Nav-Ident har ikke tilgang til noen enheter.")
        }

        @Test
        fun `skal returnere Vikafossen om NAV-identen kun har tilgang til Vikafossen når arbeidsfordeling er midlertidig enhet 4863`() {
            // Arrange
            val navIdent = NavIdent("1")

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    enhetNavn = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    KontantstøtteEnhet.VIKAFOSSEN,
                )

            // Act
            val tilpassetArbeidsfordelingsenhet =
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn)
        }

        @Test
        fun `skal returnere første enhetsnummer som NAV-identen har tilgang til når arbeidsfordeling er midlertidig enhet 4863`() {
            // Arrange
            val navIdent = NavIdent("1")
            val enhetNavIdentHarTilgangTil = KontantstøtteEnhet.OSLO

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    enhetNavn = KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    KontantstøtteEnhet.VIKAFOSSEN,
                    KontantstøtteEnhet.OSLO,
                    KontantstøtteEnhet.DRAMMEN,
                )

            // Act
            val tilpassetArbeidsfordelingsenhet =
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(enhetNavIdentHarTilgangTil.enhetsnummer)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(enhetNavIdentHarTilgangTil.enhetsnavn)
        }

        @Test
        fun `skal kaste feil hvis arbeidsfordeling er Vikafossen 2103 og NAV-ident er null`() {
            // Arrange
            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer,
                    enhetNavn = KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                        arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                        navIdent = null,
                    )
                }
            assertThat(exception.message).isEqualTo("Kan ikke håndtere ${KontantstøtteEnhet.VIKAFOSSEN} om man mangler NAV-ident.")
        }

        @Test
        fun `skal returnere Vikafossen 2103 dersom arbeidsfordeling er Vikafossen 2103 og NAV-ident ikke har tilgang til Vikafossen 2103`() {
            // Arrange
            val navIdent = NavIdent("1")
            val enhetNavIdentHarTilgangTil1 = KontantstøtteEnhet.STEINKJER
            val enhetNavIdentHarTilgangTil2 = KontantstøtteEnhet.VADSØ

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer,
                    enhetNavn = KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    KontantstøtteEnhet.STEINKJER,
                    KontantstøtteEnhet.VADSØ,
                )

            // Act
            val tilpassetArbeidsfordelingsenhet =
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn)
        }

        @Test
        fun `skal returnere Vikafossen 2103 dersom arbeidsfordeling er Vikafossen 2103 og NAV-ident har tilgang til Vikafossen 2103`() {
            // Arrange
            val navIdent = NavIdent("1")
            val enhetNavIdentHarTilgangTil = KontantstøtteEnhet.VIKAFOSSEN

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer,
                    enhetNavn = KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    KontantstøtteEnhet.BERGEN,
                    KontantstøtteEnhet.VIKAFOSSEN,
                )

            // Act
            val tilpassetArbeidsfordelingsenhet =
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(enhetNavIdentHarTilgangTil.enhetsnummer)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(enhetNavIdentHarTilgangTil.enhetsnavn)
        }

        @Test
        fun `skal returnere enhetId dersom arbeidsfordeling ikke er 2103 eller 4863 og NAV-ident er null`() {
            // Arrange
            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = KontantstøtteEnhet.STEINKJER.enhetsnummer,
                    enhetNavn = KontantstøtteEnhet.STEINKJER.enhetsnavn,
                )

            // Act
            val tilpassetArbeidsfordelingsenhet =
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = null,
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(KontantstøtteEnhet.STEINKJER.enhetsnummer)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(KontantstøtteEnhet.STEINKJER.enhetsnavn)
        }

        @Test
        fun `skal kaste feil om arbeidsfordeling ikke er 2103 eller 4863 og NAV-ident ikke har tilgang til noen enheter`() {
            // Arrange
            val navIdent = NavIdent("1")

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = KontantstøtteEnhet.DRAMMEN.enhetsnummer,
                    enhetNavn = KontantstøtteEnhet.DRAMMEN.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns emptyList()

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                        arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                        navIdent = navIdent,
                    )
                }
            assertThat(exception.message).isEqualTo("Nav-Ident har ikke tilgang til noen enheter.")
        }

        @Test
        fun `skal returnere Vikafossen hvis Nav-ident kun har tilgang til Vikafossen`() {
            // Arrange
            val navIdent = NavIdent("1")

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = "1234",
                    enhetNavn = "Fiktiv enhet",
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    KontantstøtteEnhet.VIKAFOSSEN,
                )

            // Act
            val tilpassetArbeidsfordelingsenhet =
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn)
        }

        @Test
        fun `skal returnere første enhet NAV-ident har tilgang om arbeidsfordeling ikke er 2103 eller 4863 og NAV-ident ikke har tilgang arbeidsfordeling enheten`() {
            // Arrange
            val navIdent = NavIdent("1")

            val enhetNavIdentHarTilgangTil = KontantstøtteEnhet.OSLO

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = KontantstøtteEnhet.STEINKJER.enhetsnummer,
                    enhetNavn = KontantstøtteEnhet.STEINKJER.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    KontantstøtteEnhet.OSLO,
                    KontantstøtteEnhet.DRAMMEN,
                )

            // Act
            val tilpassetArbeidsfordelingsenhet =
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(enhetNavIdentHarTilgangTil.enhetsnummer)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(enhetNavIdentHarTilgangTil.enhetsnavn)
        }

        @Test
        fun `skal returnere arbeidsfordeling dersom arbeidsfordeling ikke er 2103 eller 4863 og NAV-ident har tilgang arbeidsfordeling enheten`() {
            // Arrange
            val navIdent = NavIdent("1")

            val arbeidsfordelingEnhet = KontantstøtteEnhet.OSLO

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = arbeidsfordelingEnhet.enhetsnummer,
                    enhetNavn = arbeidsfordelingEnhet.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    KontantstøtteEnhet.MIDLERTIDIG_ENHET,
                    KontantstøtteEnhet.VIKAFOSSEN,
                    KontantstøtteEnhet.OSLO,
                )

            // Act
            val tilpassetArbeidsfordelingsenhet =
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(arbeidsfordelingEnhet.enhetsnummer)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(arbeidsfordelingEnhet.enhetsnavn)
        }

        @Test
        fun `skal returnere behandlendeEnhetId og behandlendeEnhetNavn dersom arbeidsfordeling ikke er 2103 eller 4863 og NAV-ident er SYSTEM_FORKORTELSE`() {
            // Arrange
            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = "1234",
                    enhetNavn = "Fiktiv enhet",
                )

            // Act
            val tilpassetArbeidsfordelingsenhet =
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = NavIdent(SYSTEM_FORKORTELSE),
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(arbeidsfordelingsenhet.enhetId)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(arbeidsfordelingsenhet.enhetNavn)
        }
    }

    @Nested
    inner class BestemTilordnetRessursPåOppgave {
        @Test
        fun `skal returnere navIdent dersom navIdent har tilgang til arbeidsfordelingsenhet`() {
            // Arrange
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet(enhetId = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer, enhetNavn = KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn)
            val navIdent = NavIdent("1")

            every { mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent = navIdent) } returns
                listOf(
                    KontantstøtteEnhet.VIKAFOSSEN,
                )

            // Act
            val tilordnetRessurs = tilpassArbeidsfordelingService.bestemTilordnetRessursPåOppgave(arbeidsfordelingsenhet, navIdent)

            // Assert
            assertThat(tilordnetRessurs).isEqualTo(navIdent)
        }

        @Test
        fun `skal returnere null dersom navIdent ikke har tilgang til arbeidsfordelingsenhet`() {
            // Arrange
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet(enhetId = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer, enhetNavn = KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn)
            val navIdent = NavIdent("1")

            every { mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent = navIdent) } returns
                listOf(
                    KontantstøtteEnhet.OSLO,
                )

            // Act
            val tilordnetRessurs = tilpassArbeidsfordelingService.bestemTilordnetRessursPåOppgave(arbeidsfordelingsenhet, navIdent)

            // Assert
            assertThat(tilordnetRessurs).isNull()
        }

        @Test
        fun `skal returnere null dersom navIdent er null`() {
            // Arrange
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet(enhetId = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer, enhetNavn = KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn)
            val navIdent = null

            // Act
            val tilordnetRessurs = tilpassArbeidsfordelingService.bestemTilordnetRessursPåOppgave(arbeidsfordelingsenhet, navIdent)

            // Assert
            assertThat(tilordnetRessurs).isNull()
        }

        @Test
        fun `skal returnere null dersom vi er i systemkontekst`() {
            // Arrange
            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer,
                    enhetNavn = KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn,
                )
            val navIdent = NavIdent(SYSTEM_FORKORTELSE)

            // Act
            val tilordnetRessurs =
                tilpassArbeidsfordelingService.bestemTilordnetRessursPåOppgave(arbeidsfordelingsenhet, navIdent)

            // Assert
            assertThat(tilordnetRessurs).isNull()
        }
    }
}
