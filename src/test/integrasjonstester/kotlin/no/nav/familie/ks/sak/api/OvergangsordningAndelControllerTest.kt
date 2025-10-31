package no.nav.familie.ks.sak.api

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.OvergangsordningAndelDto
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.fake.FakeIntegrasjonKlient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndelRepository
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.hamcrest.CoreMatchers.containsString as Contains
import org.hamcrest.CoreMatchers.`is` as Is

class OvergangsordningAndelControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository

    @Autowired
    private lateinit var overgangsordningAndelRepository: OvergangsordningAndelRepository

    @Autowired
    private lateinit var fakeIntegrasjonKlient: FakeIntegrasjonKlient

    private var token = ""
    private val overgangsordningAndelControllerUrl = "/api/overgangsordningandel"
    private val barnFødselsdato = LocalDate.of(2024, 1, 1)

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        opprettSøkerFagsakOgBehandling(
            barn = randomAktør(randomFnr(barnFødselsdato)),
            fagsakStatus = FagsakStatus.LØPENDE,
            behandlingÅrsak = BehandlingÅrsak.OVERGANGSORDNING_2024,
        )
        opprettPersonopplysningGrunnlagOgPersonForBehandling(lagBarn = true, fødselsdatoBarn = barnFødselsdato)
        opprettOppfyltVilkårsvurdering(periodeTom = barnFødselsdato.plusYears(2))

        arbeidsfordelingPåBehandlingRepository.save(
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling.id,
                behandlendeEnhetId = "test",
                behandlendeEnhetNavn = "test",
            ),
        )
        token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
        fakeIntegrasjonKlient.reset()
    }

    @Nested
    inner class OppdaterOvergangsordningAndelOgOppdaterTilkjentYtelse {
        @Test
        fun `skal kaste feil hvis body er feil`() {
            val overgangsordningAndel = OvergangsordningAndel(id = 0, behandlingId = behandling.id)
            overgangsordningAndelRepository.saveAndFlush(overgangsordningAndel)

            val ugyldigOvergangsordningAndelDto =
                """
                {
                    "id": 999951,
                    "personIdent": "0101545030",
                    "antallTimer": 20,
                    "deltBosted": true,
                    "fom": "2024-03",
                    "tom": "2024-01"
                }
                """.trimIndent()

            val tomFomFeilmelding = "Til og med-dato kan ikke være før fra og med-dato"
            val personidentFeilmelding = "Personident må være et gyldig fødselsnummer"
            val antallTimerFeilmelding = "Hvis antall timer er større enn 0, kan ikke delt bosted være avhuket"

            Given {
                header("Authorization", "Bearer $token")
                contentType(ContentType.JSON)
                body(ugyldigOvergangsordningAndelDto)
            } When {
                put("$overgangsordningAndelControllerUrl/${behandling.id}/${overgangsordningAndel.id}")
            } Then {
                statusCode(400)
                body("status", Is("FEILET"))
                body("melding", Contains(tomFomFeilmelding))
                body("melding", Contains(personidentFeilmelding))
                body("melding", Contains(antallTimerFeilmelding))
            }
        }

        @Test
        fun `skal oppdatere og slå sammen overgangsordningandeler`() {
            val barn1 = personopplysningGrunnlag.personer.first { it.aktør == barn }

            val gamleOvergangsordningAndeler =
                listOf(
                    OvergangsordningAndel(
                        behandlingId = behandling.id,
                        person = barn1,
                        antallTimer = BigDecimal(20),
                        fom = barn1.fødselsdato.plusMonths(20).toYearMonth(),
                        tom = barn1.fødselsdato.plusMonths(21).toYearMonth(),
                    ),
                    OvergangsordningAndel(
                        behandlingId = behandling.id,
                        person = barn1,
                        antallTimer = BigDecimal.ZERO,
                        fom = barn1.fødselsdato.plusMonths(22).toYearMonth(),
                        tom = barn1.fødselsdato.plusMonths(23).toYearMonth(),
                    ),
                )

            overgangsordningAndelRepository.saveAllAndFlush(gamleOvergangsordningAndeler)

            // Sett antall timer til 0 i første overgangsordningandel
            val overgangsordningAndelDto =
                OvergangsordningAndelDto(
                    id = gamleOvergangsordningAndeler.first().id,
                    personIdent = barn1.aktør.aktivFødselsnummer(),
                    antallTimer = BigDecimal.ZERO,
                    deltBosted = false,
                    fom = barnFødselsdato.plusMonths(20).toYearMonth(),
                    tom = barnFødselsdato.plusMonths(21).toYearMonth(),
                )

            Given {
                header("Authorization", "Bearer $token")
                contentType(ContentType.JSON)
                body(objectMapper.writeValueAsString(overgangsordningAndelDto))
            } When {
                put("$overgangsordningAndelControllerUrl/${behandling.id}/${gamleOvergangsordningAndeler.first().id}")
            } Then {
                statusCode(200)
                body("status", Is("SUKSESS"))
                // Valideringsfunksjoner skal ikke være med i responsen
                body("data.overgangsordningAndeler[0].isTomSameOrAfterFom", Is(nullValue()))
                body("data.overgangsordningAndeler[0].isPersonidentValid", Is(nullValue()))
                body("data.overgangsordningAndeler[0].isAntallTimerAndDeltBostedValid", Is(nullValue()))
            } Extract {
                val overgangsordningAndeler =
                    path<List<Map<String, Any>>>("data.overgangsordningAndeler")
                        .map { objectMapper.convertValue(it, OvergangsordningAndelDto::class.java) }
                assertThat(overgangsordningAndeler)
                    .hasSize(1)
                    .anySatisfy {
                        // Forvent at overgangsordningandeler for barn 1 er slått sammen
                        assertThat(it.personIdent).isEqualTo(barn1.aktør.aktivFødselsnummer())
                        assertThat(it.antallTimer).isEqualTo(BigDecimal.ZERO)
                        assertThat(it.fom).isEqualTo(barn1.fødselsdato.plusMonths(20).toYearMonth())
                        assertThat(it.tom).isEqualTo(barn1.fødselsdato.plusMonths(23).toYearMonth())
                    }
            }
        }
    }

    @Nested
    inner class FjernOvergangsordningAndelOgOppdaterTilkjentYtelse {
        @Test
        fun `skal slette OvergangsordningAndel`() {
            val overgangsordningAndel = OvergangsordningAndel(id = 0, behandlingId = behandling.id)
            overgangsordningAndelRepository.saveAndFlush(overgangsordningAndel)

            Given {
                header("Authorization", "Bearer $token")
            } When {
                delete("$overgangsordningAndelControllerUrl/${behandling.id}/${overgangsordningAndel.id}")
            } Then {
                statusCode(200)
                body("status", Is("SUKSESS"))
                body("data.overgangsordningAndeler", Is(emptyList<OvergangsordningAndel>()))
            }
        }
    }

    @Nested
    inner class OpprettTomOvergangsordningAndel {
        @Test
        fun `skal returnere funksjonell feil dersom behandling ikke har årsak OVERGANGSORDNING_2024`() {
            opprettSøkerFagsakOgBehandling(
                fagsakStatus = FagsakStatus.LØPENDE,
                behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            )

            Given {
                header("Authorization", "Bearer $token")
            } When {
                post("$overgangsordningAndelControllerUrl/${behandling.id}")
            } Then {
                statusCode(200)
                body("status", Is("FUNKSJONELL_FEIL"))
                body("frontendFeilmelding", Is("Behandlingen har ikke årsak 'Overgangsordning 2024'"))
            }
        }

        @Test
        fun `skal opprette tom OvergangsordningAndel`() {
            Given {
                header("Authorization", "Bearer $token")
            } When {
                post("$overgangsordningAndelControllerUrl/${behandling.id}")
            } Then {
                statusCode(200)
                body("status", Is("SUKSESS"))
                body("data.overgangsordningAndeler[0].personIdent", IsNull<String>())
                body("data.overgangsordningAndeler[0].prosent", IsNull<BigDecimal>())
                body("data.overgangsordningAndeler[0].fom", IsNull<YearMonth>())
                body("data.overgangsordningAndeler[0].tom", IsNull<YearMonth>())
            }
        }
    }
}
