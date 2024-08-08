package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.OppdaterSammensattKontrollsakDto
import no.nav.familie.ks.sak.api.dto.OpprettSammensattKontrollsakDto
import no.nav.familie.ks.sak.api.dto.SlettSammensattKontrollsakDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.hamcrest.CoreMatchers.`is` as Is

class SammensattKontrollsakControllerTest : OppslagSpringRunnerTest() {
    private val controllerUrl: String = "/api/sammensatt-kontrollsak"

    @Autowired
    private lateinit var sammensattKontrollsakRepository: SammensattKontrollsakRepository

    @MockkBean
    private lateinit var sammensattKontrollsakValidator: SammensattKontrollsakValidator

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
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
        fun `skal returnere 403 forbidden når valideringen feiler`() {
            // Arrange
            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            every {
                sammensattKontrollsakValidator.validerHentSammensattKontrollsakTilgang()
            } throws
                FunksjonellFeil(
                    melding = "En feil oppstod",
                    httpStatus = HttpStatus.FORBIDDEN,
                )

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
            } When {
                get("$controllerUrl/123")
            } Then {
                statusCode(HttpStatus.FORBIDDEN.value())
                body("data", nullValue())
                body("status", Is("FUNKSJONELL_FEIL"))
                body("melding", Is("En feil oppstod"))
                body("frontendFeilmelding", Is("En feil oppstod"))
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

            every {
                sammensattKontrollsakValidator.validerHentSammensattKontrollsakTilgang()
            } just runs

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

            every {
                sammensattKontrollsakValidator.validerHentSammensattKontrollsakTilgang()
            } just runs

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
                objectMapper.writeValueAsString(
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
                objectMapper.writeValueAsString(
                    OpprettSammensattKontrollsakDto(
                        behandlingId = behandling.id,
                        fritekst = "blabla",
                    ),
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            every {
                sammensattKontrollsakValidator.validerOpprettSammensattKontrollsakTilgang()
            } throws
                FunksjonellFeil(
                    melding = "En feil oppstod",
                    httpStatus = HttpStatus.FORBIDDEN,
                )

            every {
                sammensattKontrollsakValidator.validerRedigerbarBehandlingForBehandlingId(
                    behandlingId = behandling.id,
                )
            } just runs

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
                body("melding", Is("En feil oppstod"))
                body("frontendFeilmelding", Is("En feil oppstod"))
            }
        }

        @Test
        fun `skal returnere 400 bad request når validering av redigerbar behandling feiler`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()

            val body =
                objectMapper.writeValueAsString(
                    OpprettSammensattKontrollsakDto(
                        behandlingId = behandling.id,
                        fritekst = "blabla",
                    ),
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            every {
                sammensattKontrollsakValidator.validerOpprettSammensattKontrollsakTilgang()
            } just runs

            every {
                sammensattKontrollsakValidator.validerRedigerbarBehandlingForBehandlingId(
                    behandlingId = behandling.id,
                )
            } throws
                FunksjonellFeil(
                    melding = "En feil oppstod",
                    httpStatus = HttpStatus.BAD_REQUEST,
                )

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
                contentType(ContentType.JSON)
                body(body)
            } When {
                post(controllerUrl)
            } Then {
                statusCode(HttpStatus.BAD_REQUEST.value())
                body("data", nullValue())
                body("status", Is("FUNKSJONELL_FEIL"))
                body("melding", Is("En feil oppstod"))
                body("frontendFeilmelding", Is("En feil oppstod"))
            }
        }

        @Test
        fun `skal opprette sammensatt kontrollsak`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()

            val body =
                objectMapper.writeValueAsString(
                    OpprettSammensattKontrollsakDto(
                        behandlingId = behandling.id,
                        fritekst = "blabla",
                    ),
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            every {
                sammensattKontrollsakValidator.validerOpprettSammensattKontrollsakTilgang()
            } just runs

            every {
                sammensattKontrollsakValidator.validerRedigerbarBehandlingForBehandlingId(
                    behandlingId = behandling.id,
                )
            } just runs

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
                objectMapper.writeValueAsString(
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
                objectMapper.writeValueAsString(
                    oppdaterSammensattKontrollsakDto,
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            every {
                sammensattKontrollsakValidator.validerOppdaterSammensattKontrollsakTilgang()
            } throws
                FunksjonellFeil(
                    melding = "En feil oppstod",
                    httpStatus = HttpStatus.FORBIDDEN,
                )

            every {
                sammensattKontrollsakValidator.validerRedigerbarBehandlingForSammensattKontrollsakId(
                    sammensattKontrollsakId = oppdaterSammensattKontrollsakDto.id,
                )
            } just runs

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
                body("melding", Is("En feil oppstod"))
                body("frontendFeilmelding", Is("En feil oppstod"))
            }
        }

        @Test
        fun `skal returnere 400 bad request når validering av redigerbar behandling feiler`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()

            val oppdaterSammensattKontrollsakDto =
                OppdaterSammensattKontrollsakDto(
                    id = 0L,
                    fritekst = "blabla",
                )

            val body =
                objectMapper.writeValueAsString(
                    oppdaterSammensattKontrollsakDto,
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            every {
                sammensattKontrollsakValidator.validerOppdaterSammensattKontrollsakTilgang()
            } just runs

            every {
                sammensattKontrollsakValidator.validerRedigerbarBehandlingForSammensattKontrollsakId(
                    sammensattKontrollsakId = oppdaterSammensattKontrollsakDto.id,
                )
            } throws
                FunksjonellFeil(
                    melding = "En feil oppstod",
                    httpStatus = HttpStatus.BAD_REQUEST,
                )

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
                body("melding", Is("En feil oppstod"))
                body("frontendFeilmelding", Is("En feil oppstod"))
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
                objectMapper.writeValueAsString(
                    oppdaterSammensattKontrollsakDto,
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            every {
                sammensattKontrollsakValidator.validerOppdaterSammensattKontrollsakTilgang()
            } just runs

            every {
                sammensattKontrollsakValidator.validerRedigerbarBehandlingForSammensattKontrollsakId(
                    sammensattKontrollsakId = oppdaterSammensattKontrollsakDto.id,
                )
            } just runs

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
                objectMapper.writeValueAsString(
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
                objectMapper.writeValueAsString(
                    slettSammensattKontrollsakDto,
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            every {
                sammensattKontrollsakValidator.validerSlettSammensattKontrollsakTilgang()
            } throws
                FunksjonellFeil(
                    melding = "En feil oppstod",
                    httpStatus = HttpStatus.FORBIDDEN,
                )

            every {
                sammensattKontrollsakValidator.validerRedigerbarBehandlingForSammensattKontrollsakId(
                    sammensattKontrollsakId = slettSammensattKontrollsakDto.id,
                )
            } just runs

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
                body("melding", Is("En feil oppstod"))
                body("frontendFeilmelding", Is("En feil oppstod"))
            }
        }

        @Test
        fun `skal returnere 400 bad request når validering av redigerbar behandling feiler`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()

            val slettSammensattKontrollsakDto =
                SlettSammensattKontrollsakDto(
                    id = 0L,
                )

            val body =
                objectMapper.writeValueAsString(
                    slettSammensattKontrollsakDto,
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            every {
                sammensattKontrollsakValidator.validerSlettSammensattKontrollsakTilgang()
            } just runs

            every {
                sammensattKontrollsakValidator.validerRedigerbarBehandlingForSammensattKontrollsakId(
                    sammensattKontrollsakId = slettSammensattKontrollsakDto.id,
                )
            } throws
                FunksjonellFeil(
                    melding = "En feil oppstod",
                    httpStatus = HttpStatus.BAD_REQUEST,
                )

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
                contentType(ContentType.JSON)
                body(body)
            } When {
                delete(controllerUrl)
            } Then {
                statusCode(HttpStatus.BAD_REQUEST.value())
                body("data", nullValue())
                body("status", Is("FUNKSJONELL_FEIL"))
                body("melding", Is("En feil oppstod"))
                body("frontendFeilmelding", Is("En feil oppstod"))
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
                objectMapper.writeValueAsString(
                    slettSammensattKontrollsakDto,
                )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            every {
                sammensattKontrollsakValidator.validerSlettSammensattKontrollsakTilgang()
            } just runs

            every {
                sammensattKontrollsakValidator.validerRedigerbarBehandlingForSammensattKontrollsakId(
                    sammensattKontrollsakId = slettSammensattKontrollsakDto.id,
                )
            } just runs

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
