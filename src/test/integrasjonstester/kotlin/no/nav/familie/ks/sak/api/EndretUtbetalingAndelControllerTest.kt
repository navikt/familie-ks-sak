package no.nav.familie.ks.sak.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndel
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

class EndretUtbetalingAndelControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @Autowired
    private lateinit var endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository

    @Autowired
    private lateinit var arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository

    @MockkBean
    private lateinit var integrasjonClient: IntegrasjonClient

    private val controllerUrl: String = "/api/endretutbetalingandel"

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        every { integrasjonClient.hentLand(any()) } returns "Norge"
    }

    @Nested
    inner class OppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelseTest {
        @Test
        fun `skal returnere UNAUTHORIZED om token ikke er satt`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()

            // Act & assert
            When {
                put("$controllerUrl/${behandling.id}")
            } Then {
                statusCode(HttpStatus.UNAUTHORIZED.value())
            }
        }

        @Test
        fun `skal kaste feil ved forsøk på å oppdatere endret utbetaling andel med veileder tilgang`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()
            opprettPersonopplysningGrunnlagOgPersonForBehandling()
            opprettVilkårsvurdering(aktør = søker, behandling = behandling, resultat = Resultat.OPPFYLT)
            lagTilkjentYtelse()
            andelTilkjentYtelseRepository.saveAndFlush(
                lagAndelTilkjentYtelse(
                    aktør = søker,
                    stønadFom = YearMonth.of(2024, 8),
                    stønadTom = YearMonth.of(2024, 8),
                    tilkjentYtelse = tilkjentYtelse,
                    behandling = behandling,
                ),
            )

            val lagretTomEndretUtbetalingAndel =
                endretUtbetalingAndelRepository.saveAndFlush(
                    lagEndretUtbetalingAndel(
                        behandlingId = behandling.id,
                        person = søkerPerson,
                    ),
                )

            val gyldigEndretUtbetalingAndelDto =
                """
                {
                    "id": ${lagretTomEndretUtbetalingAndel.id},
                    "personIdent": "${søker.aktivFødselsnummer()}",
                    "prosent": 0,
                    "fom": "2024-08",
                    "tom": "2024-08",
                    "årsak": "FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024",
                    "søknadstidspunkt": "2024-08-20",
                    "begrunnelse": "gyldig request"
                }
                """.trimIndent()

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.VEILEDER)

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
                body(gyldigEndretUtbetalingAndelDto)
                contentType(ContentType.JSON)
            } When {
                put("$controllerUrl/${behandling.id}")
            } Then {
                statusCode(HttpStatus.FORBIDDEN.value())
                body("status", Is("IKKE_TILGANG"))
                body("frontendFeilmelding", Is("Du har ikke tilgang til å Oppdater endretutbetalingandel."))
            }
        }

        @Test
        fun `skal oppdatere EndretUtbetalingAndel`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()
            opprettPersonopplysningGrunnlagOgPersonForBehandling()
            opprettVilkårsvurdering(aktør = søker, behandling = behandling, resultat = Resultat.OPPFYLT)
            lagTilkjentYtelse()
            andelTilkjentYtelseRepository.saveAndFlush(
                lagAndelTilkjentYtelse(
                    aktør = søker,
                    stønadFom = YearMonth.of(2024, 8),
                    stønadTom = YearMonth.of(2024, 8),
                    tilkjentYtelse = tilkjentYtelse,
                    behandling = behandling,
                ),
            )

            every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang("test", true))

            val lagretTomEndretUtbetalingAndel =
                endretUtbetalingAndelRepository.saveAndFlush(
                    lagEndretUtbetalingAndel(
                        behandlingId = behandling.id,
                        person = søkerPerson,
                    ),
                )

            val gyldigEndretUtbetalingAndelDto =
                """
                {
                    "id": ${lagretTomEndretUtbetalingAndel.id},
                    "personIdent": "${søker.aktivFødselsnummer()}",
                    "prosent": 0,
                    "fom": "2024-08",
                    "tom": "2024-08",
                    "årsak": "FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024",
                    "søknadstidspunkt": "2024-08-20",
                    "begrunnelse": "gyldig request"
                }
                """.trimIndent()

            arbeidsfordelingPåBehandlingRepository.save(
                ArbeidsfordelingPåBehandling(
                    behandlingId = behandling.id,
                    behandlendeEnhetId = "test",
                    behandlendeEnhetNavn = "test",
                ),
            )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
                body(gyldigEndretUtbetalingAndelDto)
                contentType(ContentType.JSON)
            } When {
                put("$controllerUrl/${behandling.id}")
            } Then {
                statusCode(HttpStatus.OK.value())
                body("data.behandlingId", Is(behandling.id.toInt()))
                body("data.endretUtbetalingAndeler[0].id", Is(lagretTomEndretUtbetalingAndel.id.toInt()))
                body("data.endretUtbetalingAndeler[0].prosent", Is(0))
                body("data.endretUtbetalingAndeler[0].fom", Is("2024-08"))
                body("data.endretUtbetalingAndeler[0].tom", Is("2024-08"))
                body("data.endretUtbetalingAndeler[0].årsak", Is("FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024"))
                body("status", Is("SUKSESS"))
                body("melding", Is("Innhenting av data var vellykket"))
            }
        }

        @Test
        fun `skal kaste feil hvis periode på EndretUtbetalingAndel ikke overlapper med eksisterende andel tilkjent ytelse`() {
            // Arrange
            opprettSøkerFagsakOgBehandling()
            opprettPersonopplysningGrunnlagOgPersonForBehandling()
            opprettVilkårsvurdering(aktør = søker, behandling = behandling, resultat = Resultat.OPPFYLT)
            lagTilkjentYtelse()
            andelTilkjentYtelseRepository.saveAndFlush(
                lagAndelTilkjentYtelse(
                    aktør = søker,
                    stønadFom = YearMonth.of(2024, 9),
                    stønadTom = YearMonth.of(2024, 9),
                    tilkjentYtelse = tilkjentYtelse,
                    behandling = behandling,
                ),
            )

            every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang("test", true))

            val lagretTomEndretUtbetalingAndel =
                endretUtbetalingAndelRepository.saveAndFlush(
                    lagEndretUtbetalingAndel(
                        behandlingId = behandling.id,
                        person = søkerPerson,
                    ),
                )

            val gyldigEndretUtbetalingAndelDto =
                """
                {
                    "id": ${lagretTomEndretUtbetalingAndel.id},
                    "personIdent": "${søker.aktivFødselsnummer()}",
                    "prosent": 0,
                    "fom": "2024-08",
                    "tom": "2024-08",
                    "årsak": "FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024",
                    "søknadstidspunkt": "2024-08-20",
                    "begrunnelse": "gyldig request"
                }
                """.trimIndent()

            arbeidsfordelingPåBehandlingRepository.save(
                ArbeidsfordelingPåBehandling(
                    behandlingId = behandling.id,
                    behandlendeEnhetId = "test",
                    behandlendeEnhetNavn = "test",
                ),
            )

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
                body(gyldigEndretUtbetalingAndelDto)
                contentType(ContentType.JSON)
            } When {
                put("$controllerUrl/${behandling.id}")
            } Then {
                statusCode(HttpStatus.OK.value())
                body("status", Is("FUNKSJONELL_FEIL"))
                body("frontendFeilmelding", Is("Du har valgt en periode der det ikke finnes tilkjent ytelse for valgt person i hele eller deler av perioden."))
            }
        }
    }
}
