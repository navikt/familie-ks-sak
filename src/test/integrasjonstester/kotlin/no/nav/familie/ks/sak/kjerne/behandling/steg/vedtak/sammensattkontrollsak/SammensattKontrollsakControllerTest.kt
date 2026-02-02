package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.OppdaterSammensattKontrollsakDto
import no.nav.familie.ks.sak.api.dto.OpprettSammensattKontrollsakDto
import no.nav.familie.ks.sak.api.dto.SlettSammensattKontrollsakDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.fake.FakeFeatureToggleService
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.hamcrest.CoreMatchers.`is` as Is

@Disabled
class SammensattKontrollsakControllerTest : OppslagSpringRunnerTest() {
    private val controllerUrl: String = "/api/sammensatt-kontrollsak"

    @Autowired
    private lateinit var sammensattKontrollsakRepository: SammensattKontrollsakRepository

    @Autowired
    private lateinit var fakeFeatureToggleService: FakeFeatureToggleService

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        fakeFeatureToggleService.reset()
    }

    @Nested
    inner class HentSammensattKontrollsakTest {
        @Test
        fun `skal returnere UNAUTHORIZED om token ikke er satt`() {
            // Act & assert
            When {
                get("$controllerUrl/123")
            } Then {
                statusCode(HttpStatus.UNAUTHORIZED.value())
            }
        }

        @Test
        fun `skal returnere 403 forbidden når man ikke har tilgang på å endre sammensatt kontrollsak`() {
            // Arrange
            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            fakeFeatureToggleService.set(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER, false)

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
            } When {
                get("$controllerUrl/123")
            } Then {
                statusCode(HttpStatus.FORBIDDEN.value())
                body("data", nullValue())
                body("status", Is("FUNKSJONELL_FEIL"))
                body("melding", Is("Mangler tilgang for å hente sammensatt kontrollsak."))
                body("frontendFeilmelding", Is("Mangler tilgang for å hente sammensatt kontrollsak."))
            }
        }

        @Test
        fun `skal hente sammensatt kontrollsak for behandlingId`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()

            val sammensattKontrollsak =
                SammensattKontrollsak(
                    behandlingId = behandling.id,
                    fritekst = "blabla",
                )

            sammensattKontrollsakRepository.save(sammensattKontrollsak)

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
            } When {
                get("$controllerUrl/${behandling.id}")
            } Then {
                statusCode(HttpStatus.OK.value())
                body("data.id", notNullValue())
                body("data.behandlingId", Is(behandling.id.toInt()))
                body("data.fritekst", Is("blabla"))
                body("status", Is("SUKSESS"))
                body("melding", Is("Innhenting av data var vellykket"))
                body("frontendFeilmelding", nullValue())
            }
        }

        @Test
        fun `skal håndtere tilfeller hvor ingen sammensatt kontrollsak er funnet for behandlingId`() {
            // Arrange
            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
            } When {
                get("$controllerUrl/123")
            } Then {
                statusCode(HttpStatus.OK.value())
                body("data", nullValue())
                body("status", Is("SUKSESS"))
                body("melding", Is("Innhenting av data var vellykket"))
                body("frontendFeilmelding", nullValue())
            }
        }
    }

    @Nested
    inner class OpprettSammensattKontrollsakTest {
        @Test
        fun `skal returnere UNAUTHORIZED om token ikke er satt`() {
            // Arrange
            val body =
                jsonMapper.writeValueAsString(
                    OpprettSammensattKontrollsakDto(
                        behandlingId = 123L,
                        fritekst = "blabla",
                    ),
                )

            // Act & assert
            Given {
                contentType(ContentType.JSON)
                body(body)
            } When {
                post(controllerUrl)
            } Then {
                statusCode(HttpStatus.UNAUTHORIZED.value())
            }
        }

        @Test
        fun `skal returnere 403 forbidden når validering av tilgang feiler`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()

            val body =
                jsonMapper.writeValueAsString(
                    OpprettSammensattKontrollsakDto(
                        behandlingId = behandling.id,
                        fritekst = "blabla",
                    ),
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
            fakeFeatureToggleService.set(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER, false)

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
                contentType(ContentType.JSON)
                body(body)
            } When {
                post(controllerUrl)
            } Then {
                statusCode(HttpStatus.FORBIDDEN.value())
                body("data", nullValue())
                body("status", Is("FUNKSJONELL_FEIL"))
                body("melding", Is("Mangler tilgang for å opprette sammensatt kontrollsak."))
                body("frontendFeilmelding", Is("Mangler tilgang for å opprette sammensatt kontrollsak."))
            }
        }

        @Test
        fun `skal opprette sammensatt kontrollsak`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()

            val body =
                jsonMapper.writeValueAsString(
                    OpprettSammensattKontrollsakDto(
                        behandlingId = behandling.id,
                        fritekst = "blabla",
                    ),
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
                contentType(ContentType.JSON)
                body(body)
            } When {
                post(controllerUrl)
            } Then {
                statusCode(HttpStatus.OK.value())
                body("data.id", notNullValue())
                body("data.behandlingId", Is(behandling.id.toInt()))
                body("data.fritekst", Is("blabla"))
                body("status", Is("SUKSESS"))
                body("melding", Is("Innhenting av data var vellykket"))
                body("frontendFeilmelding", nullValue())
            }
        }
    }

    @Nested
    inner class OppdaterSammensattKontrollsakTest {
        @Test
        fun `skal returnere UNAUTHORIZED om token ikke er satt`() {
            // Arrange
            val body =
                jsonMapper.writeValueAsString(
                    OppdaterSammensattKontrollsakDto(
                        id = 0L,
                        fritekst = "blabla",
                    ),
                )

            // Act & assert
            Given {
                contentType(ContentType.JSON)
                body(body)
            } When {
                put(controllerUrl)
            } Then {
                statusCode(HttpStatus.UNAUTHORIZED.value())
            }
        }

        @Test
        fun `skal returnere 403 forbidden når validering av tilgang feiler`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()

            val oppdaterSammensattKontrollsakDto =
                OppdaterSammensattKontrollsakDto(
                    id = 0L,
                    fritekst = "blabla",
                )

            val body =
                jsonMapper.writeValueAsString(
                    oppdaterSammensattKontrollsakDto,
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
            fakeFeatureToggleService.set(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER, false)

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
                contentType(ContentType.JSON)
                body(body)
            } When {
                put(controllerUrl)
            } Then {
                statusCode(HttpStatus.FORBIDDEN.value())
                body("data", nullValue())
                body("status", Is("FUNKSJONELL_FEIL"))
                body("melding", Is("Mangler tilgang for å oppdatere sammensatt kontrollsak."))
                body("frontendFeilmelding", Is("Mangler tilgang for å oppdatere sammensatt kontrollsak."))
            }
        }

        @Test
        fun `skal returnere 400 bad request når validering av redigerbar behandling feiler`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()

            val oppdaterSammensattKontrollsakDto =
                OppdaterSammensattKontrollsakDto(
                    id = behandling.id,
                    fritekst = "blabla",
                )

            val body =
                jsonMapper.writeValueAsString(
                    oppdaterSammensattKontrollsakDto,
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
                contentType(ContentType.JSON)
                body(body)
            } When {
                put(controllerUrl)
            } Then {
                statusCode(HttpStatus.BAD_REQUEST.value())
                body("data", nullValue())
                body("status", Is("FUNKSJONELL_FEIL"))
                body("melding", Is("Fant ingen sammensatt kontrollsak for id=${behandling.id}."))
                body("frontendFeilmelding", Is("Fant ingen sammensatt kontrollsak for id=${behandling.id}."))
            }
        }

        @Test
        fun `skal oppdatere sammensatt kontrollsak`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()

            sammensattKontrollsakRepository.save(
                SammensattKontrollsak(
                    behandlingId = behandling.id,
                    fritekst = "blabla",
                ),
            )

            val sammensattKontrollsak =
                sammensattKontrollsakRepository.finnSammensattKontrollsakForBehandling(
                    behandlingId = behandling.id,
                ) ?: throw Feil("Fant ingen sammensatt kontrollsak")

            val oppdaterSammensattKontrollsakDto =
                OppdaterSammensattKontrollsakDto(
                    id = sammensattKontrollsak.id,
                    fritekst = "blabla",
                )

            val body =
                jsonMapper.writeValueAsString(
                    oppdaterSammensattKontrollsakDto,
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
                contentType(ContentType.JSON)
                body(body)
            } When {
                put(controllerUrl)
            } Then {
                statusCode(HttpStatus.OK.value())
                body("data.id", Is(sammensattKontrollsak.id.toInt()))
                body("data.behandlingId", Is(behandling.id.toInt()))
                body("data.fritekst", Is("blabla"))
                body("status", Is("SUKSESS"))
                body("melding", Is("Innhenting av data var vellykket"))
                body("frontendFeilmelding", nullValue())
            }
        }
    }

    @Nested
    inner class SlettSammensattKontrollsakTest {
        @Test
        fun `skal returnere UNAUTHORIZED om token ikke er satt`() {
            // Arrange
            val body =
                jsonMapper.writeValueAsString(
                    SlettSammensattKontrollsakDto(
                        id = 0L,
                    ),
                )

            // Act & assert
            Given {
                contentType(ContentType.JSON)
                body(body)
            } When {
                delete(controllerUrl)
            } Then {
                statusCode(HttpStatus.UNAUTHORIZED.value())
            }
        }

        @Test
        fun `skal returnere 403 forbidden når validering av tilgang feiler`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()

            val slettSammensattKontrollsakDto =
                SlettSammensattKontrollsakDto(
                    id = 0L,
                )

            val body =
                jsonMapper.writeValueAsString(
                    slettSammensattKontrollsakDto,
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
            fakeFeatureToggleService.set(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER, false)

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
                contentType(ContentType.JSON)
                body(body)
            } When {
                delete(controllerUrl)
            } Then {
                statusCode(HttpStatus.FORBIDDEN.value())
                body("data", nullValue())
                body("status", Is("FUNKSJONELL_FEIL"))
                body("melding", Is("Mangler tilgang for å slette sammensatt kontrollsak."))
                body("frontendFeilmelding", Is("Mangler tilgang for å slette sammensatt kontrollsak."))
            }
        }

        @Test
        fun `skal slette sammensatt kontrollsak`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()

            sammensattKontrollsakRepository.save(
                SammensattKontrollsak(
                    behandlingId = behandling.id,
                    fritekst = "blabla",
                ),
            )

            val sammensattKontrollsak =
                sammensattKontrollsakRepository.finnSammensattKontrollsakForBehandling(
                    behandlingId = behandling.id,
                ) ?: throw Feil("Fant ingen sammensatt kontrollsak")

            val slettSammensattKontrollsakDto =
                SlettSammensattKontrollsakDto(
                    id = sammensattKontrollsak.id,
                )

            val body =
                jsonMapper.writeValueAsString(
                    slettSammensattKontrollsakDto,
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
                contentType(ContentType.JSON)
                body(body)
            } When {
                delete(controllerUrl)
            } Then {
                statusCode(HttpStatus.OK.value())
                body("data", Is(sammensattKontrollsak.id.toInt()))
                body("status", Is("SUKSESS"))
                body("melding", Is("Innhenting av data var vellykket"))
                body("frontendFeilmelding", nullValue())
            }
        }
    }
}
