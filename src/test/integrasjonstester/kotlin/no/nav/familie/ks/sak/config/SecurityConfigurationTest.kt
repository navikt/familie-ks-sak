package no.nav.familie.ks.sak.config

import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.hamcrest.CoreMatchers.`is` as Is

class SecurityConfigurationTest : OppslagSpringRunnerTest() {
    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @Nested
    inner class PermitAll {
        @Test
        fun `internal liveness-endepunkt er tilgjengelig uten token`() {
            Given {
                this
            } When {
                get("/internal/health/liveness")
            } Then {
                statusCode(200)
            }
        }

        @Test
        fun `favicon er tilgjengelig uten token`() {
            Given {
                this
            } When {
                get("/favicon.ico")
            } Then {
                statusCode(200)
            }
        }
    }

    @Nested
    inner class UtenToken {
        @Test
        fun `api-kall uten token returnerer 401 med Ressurs-body`() {
            Given {
                this
            } When {
                get("/api/fagsaker/minimal/1")
            } Then {
                statusCode(401)
                body("status", Is("FEILET"))
                body("frontendFeilmelding", Is("Kall ikke autorisert"))
            }
        }
    }

    @Nested
    inner class InternAppTilgang {
        @ParameterizedTest
        @EnumSource(value = BehandlerRolle::class, names = ["VEILEDER", "SAKSBEHANDLER", "BESLUTTER", "FORVALTER"])
        fun `saksbehandler med rolle har tilgang til interne endepunkter`(behandlerRolle: BehandlerRolle) {
            Given {
                header("Authorization", "Bearer ${lokalTestToken(behandlerRolle = behandlerRolle)}")
            } When {
                get("/api/fagsaker/minimal/1")
            } Then {
                statusCode(not(anyOf(equalTo(401), equalTo(403))))
            }
        }

        @Test
        fun `m2m-token fra teamfamilie-app har tilgang til interne endepunkter`() {
            Given {
                header("Authorization", "Bearer ${lokalTestToken(mapOf("azp_name" to "dev-gcp:teamfamilie:tilfeldig-applikasjon"))}")
            } When {
                get("/sok/fagsaker-hvor-person-er-deltaker")
            } Then {
                statusCode(not(anyOf(equalTo(401), equalTo(403))))
            }
        }

        @Test
        fun `m2m-token uten teamfamilie-namespace har ikke tilgang til interne endepunkter`() {
            Given {
                header("Authorization", "Bearer ${lokalTestToken(mapOf("azp_name" to "dev-gcp:ukjent-namespace:ukjent-applikasjon"))}")
            } When {
                get("/api/fagsaker/minimal/1")
            } Then {
                statusCode(403)
            }
        }
    }

    @Nested
    inner class BisysTilgang {
        private fun bisysToken(applikasjonNavn: String) = lokalTestToken(mapOf("azp_name" to "dev-gcp:bidrag:$applikasjonNavn"))

        @Test
        fun `bisys-token har ikke tilgang til generelt api-endepunkt`() {
            Given {
                header("Authorization", "Bearer ${bisysToken("bidrag-grunnlag")}")
            } When {
                get("/api/fagsaker/minimal/1")
            } Then {
                statusCode(403)
            }
        }

        @ParameterizedTest
        @ValueSource(strings = ["bidrag-grunnlag", "bidrag-grunnlag-feature"])
        fun `bisys-token har tilgang til bisys-endepunkt`(applikasjonNavn: String) {
            Given {
                header("Authorization", "Bearer ${bisysToken(applikasjonNavn)}")
            } When {
                post("/api/bisys/hent-utbetalingsinfo")
            } Then {
                statusCode(not(anyOf(equalTo(401), equalTo(403))))
            }
        }
    }
}
