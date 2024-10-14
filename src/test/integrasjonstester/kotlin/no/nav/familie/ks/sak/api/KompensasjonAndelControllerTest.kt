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
import no.nav.familie.ks.sak.api.dto.KompensasjonAndelDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleConfig.Companion.KOMPENSASJONSORDNING
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.KompensasjonAndel
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.KompensasjonAndelRepository
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.YearMonth
import org.hamcrest.CoreMatchers.containsString as Contains
import org.hamcrest.CoreMatchers.`is` as Is

class KompensasjonAndelControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository

    @Autowired
    private lateinit var kompensasjonAndelRepository: KompensasjonAndelRepository

    @MockkBean
    private lateinit var unleashNextMedContextService: UnleashNextMedContextService

    @MockkBean
    private lateinit var integrasjonClient: IntegrasjonClient

    private var token = ""
    private val kompensasjonAndelControllerUrl = "/api/kompensasjonandel"

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        opprettSøkerFagsakOgBehandling(
            fagsakStatus = FagsakStatus.LØPENDE,
            behandlingÅrsak = BehandlingÅrsak.KOMPENSASJONSORDNING_2024,
        )
        opprettPersonopplysningGrunnlagOgPersonForBehandling()
        opprettVilkårsvurdering(aktør = søker, behandling = behandling, resultat = Resultat.OPPFYLT)

        arbeidsfordelingPåBehandlingRepository.save(
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling.id,
                behandlendeEnhetId = "test",
                behandlendeEnhetNavn = "test",
            ),
        )
        every { integrasjonClient.hentLand(any()) } returns "Norge"
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang("test", true))
        every { unleashNextMedContextService.isEnabled(KOMPENSASJONSORDNING) } returns true
        token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
    }

    @Nested
    inner class OppdaterKompensasjonAndelOgOppdaterTilkjentYtelse {
        @Test
        fun `skal kaste feil hvis body er feil`() {
            val kompensasjonAndel = KompensasjonAndel(id = 0, behandlingId = behandling.id)
            kompensasjonAndelRepository.saveAndFlush(kompensasjonAndel)

            val ugyldigKompensasjonAndelDto =
                """
                {
                    "id": 999951,
                    "personIdent": "0101545030",
                    "prosent": 110,
                    "fom": "2024-03",
                    "tom": "2024-01"
                }
                """.trimIndent()

            val tomFomFeilmelding = "Til og med-dato kan ikke være før fra og med-dato"
            val personidentFeilmelding = "Personident må være elleve siffer"
            val prosentFeilmelding = "Prosent må være mellom 0 og 100"

            Given {
                header("Authorization", "Bearer $token")
                contentType(ContentType.JSON)
                body(ugyldigKompensasjonAndelDto)
            } When {
                put("$kompensasjonAndelControllerUrl/${behandling.id}/${kompensasjonAndel.id}")
            } Then {
                statusCode(400)
                body("status", Is("FEILET"))
                body("melding", Contains(tomFomFeilmelding))
                body("melding", Contains(personidentFeilmelding))
                body("melding", Contains(prosentFeilmelding))
            }
        }

        @Test
        fun `skal oppdatere kompensasjonAndel`() {
            val kompensasjonAndel = KompensasjonAndel(id = 0, behandlingId = behandling.id)
            kompensasjonAndelRepository.saveAndFlush(kompensasjonAndel)

            val kompensasjonAndelDto =
                KompensasjonAndelDto(
                    id = kompensasjonAndel.id,
                    personIdent = søker.aktivFødselsnummer(),
                    prosent = BigDecimal(100),
                    fom = YearMonth.of(2024, 1),
                    tom = YearMonth.of(2024, 3),
                )

            Given {
                header("Authorization", "Bearer $token")
                contentType(ContentType.JSON)
                body(objectMapper.writeValueAsString(kompensasjonAndelDto))
            } When {
                put("$kompensasjonAndelControllerUrl/${behandling.id}/${kompensasjonAndel.id}")
            } Then {
                statusCode(200)
                body("status", Is("SUKSESS"))
                body("data.kompensasjonAndeler[0].personIdent", Is(søker.aktivFødselsnummer()))
                body("data.kompensasjonAndeler[0].prosent", Is(100))
                body("data.kompensasjonAndeler[0].fom", Is("2024-01"))
                body("data.kompensasjonAndeler[0].tom", Is("2024-03"))
            }
        }
    }

    @Nested
    inner class FjernKompensasjonAndelOgOppdaterTilkjentYtelse {
        @Test
        fun `skal slette kompensasjonAndel`() {
            val kompensasjonAndel = KompensasjonAndel(id = 0, behandlingId = behandling.id)
            kompensasjonAndelRepository.saveAndFlush(kompensasjonAndel)

            Given {
                header("Authorization", "Bearer $token")
            } When {
                delete("$kompensasjonAndelControllerUrl/${behandling.id}/${kompensasjonAndel.id}")
            } Then {
                statusCode(200)
                body("status", Is("SUKSESS"))
                body("data.kompensasjonAndeler", Is(emptyList<KompensasjonAndel>()))
            }
        }
    }

    @Nested
    inner class OpprettTomKompensasjonAndel {
        @Test
        fun `skal returnere funksjonell feil dersom toggle ikke er skrudd på`() {
            every { unleashNextMedContextService.isEnabled(KOMPENSASJONSORDNING) } returns false

            Given {
                header("Authorization", "Bearer $token")
            } When {
                post("$kompensasjonAndelControllerUrl/${behandling.id}")
            } Then {
                statusCode(200)
                body("status", Is("FUNKSJONELL_FEIL"))
                body("frontendFeilmelding", Is("Behandling med årsak kompensasjonsordning er ikke tilgjengelig"))
            }
        }

        @Test
        fun `skal returnere funksjonell feil dersom behandling ikke har årsak KOMPENSASJONSORDNING_2024`() {
            opprettSøkerFagsakOgBehandling(
                fagsakStatus = FagsakStatus.LØPENDE,
                behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            )

            Given {
                header("Authorization", "Bearer $token")
            } When {
                post("$kompensasjonAndelControllerUrl/${behandling.id}")
            } Then {
                statusCode(200)
                body("status", Is("FUNKSJONELL_FEIL"))
                body("frontendFeilmelding", Is("Behandlingen har ikke årsak 'Kompensasjonsordning 2024'"))
            }
        }

        @Test
        fun `skal opprette tom kompensasjonAndel`() {
            Given {
                header("Authorization", "Bearer $token")
            } When {
                post("$kompensasjonAndelControllerUrl/${behandling.id}")
            } Then {
                statusCode(200)
                body("status", Is("SUKSESS"))
                body("data.kompensasjonAndeler[0].personIdent", IsNull<String>())
                body("data.kompensasjonAndeler[0].prosent", IsNull<BigDecimal>())
                body("data.kompensasjonAndeler[0].fom", IsNull<YearMonth>())
                body("data.kompensasjonAndeler[0].tom", IsNull<YearMonth>())
            }
        }
    }
}
