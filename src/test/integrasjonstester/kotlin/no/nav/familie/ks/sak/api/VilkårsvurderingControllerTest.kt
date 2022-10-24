package no.nav.familie.ks.sak.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.hamcrest.CoreMatchers.`is` as Is

class VilkårsvurderingControllerTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Autowired
    private lateinit var annenVurderingRepository: AnnenVurderingRepository

    @MockkBean
    private lateinit var integrasjonClient: IntegrasjonClient

    val vilkårsvurderingControllerUrl = "/api/vilkårsvurdering"

    @BeforeEach
    fun setup() {
        RestAssured.port = port
        opprettSøkerFagsakOgBehandling()
        opprettPersonopplysningGrunnlagOgPersonForBehandling(behandling.id)
        vilkårsvurderingService.opprettVilkårsvurdering(behandling, null)
        arbeidsfordelingPåBehandlingRepository.save(
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling.id,
                behandlendeEnhetId = "test",
                behandlendeEnhetNavn = "test"
            )
        )

        every { integrasjonClient.hentLand(any()) } returns "Norge"
    }

    @Test
    fun `endreVilkår - skal returnere endrede vilkår`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true, "test")

        val bosattIRiketVilkår = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)
            .personResultater.find { it.aktør == søker }?.vilkårResultater?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }!!

        assertThat(bosattIRiketVilkår.periodeFom, Is(nullValue()))

        val request =
            """
                {
                  "personIdent": "${søker.aktivFødselsnummer()}",
                  "endretVilkårResultat": {
                    "begrunnelse": "ftg",
                    "behandlingId": ${behandling.id.toInt()},
                    "endretAv": "Z994151",
                    "endretTidspunkt": "2022-10-10T08:28:21.776",
                    "erAutomatiskVurdert": true,
                    "erVurdert": false,
                    "id": ${bosattIRiketVilkår.id},
                    "periodeFom": "2022-10-06",
                    "resultat": "OPPFYLT",
                    "erEksplisittAvslagPåSøknad": false,
                    "avslagBegrunnelser": [],
                    "vilkårType": "BOSATT_I_RIKET",
                    "vurderesEtter": "NASJONALE_REGLER",
                    "utdypendeVilkårsvurderinger": [
                      "VURDERING_ANNET_GRUNNLAG"
                    ]
                  }
                }
            """.trimIndent()

        Given {
            header("Authorization", "Bearer $token")
            body(request)
            contentType(MediaType.APPLICATION_JSON_VALUE)
        } When {
            put("$vilkårsvurderingControllerUrl/${behandling.id}")
        } Then {
            body(
                "data.personResultater[0].vilkårResultater.find {it.vilkårType == 'BOSATT_I_RIKET'}.periodeFom",
                Is("2022-10-06")
            )
            statusCode(HttpStatus.OK.value())
        }
    }

    @Test
    fun `opprettNyttVilkår - skal kaste feil dersom det eksisterer uvurdert vilkår av samme type`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true, "test")

        val request =
            """
                {
                  "personIdent": "${søker.aktivFødselsnummer()}",
                  "vilkårType": "BOSATT_I_RIKET"
                  }
                }
            """.trimIndent()

        Given {
            header("Authorization", "Bearer $token")
            body(request)
            contentType(MediaType.APPLICATION_JSON_VALUE)
        } When {
            post("$vilkårsvurderingControllerUrl/${behandling.id}")
        } Then {
            body("status", Is("FUNKSJONELL_FEIL"))
            body("melding", Is("Det finnes allerede uvurderte vilkår av samme vilkårType"))
            body(
                "frontendFeilmelding",
                Is("Du må ferdigstille vilkårsvurderingen på en periode som allerede er påbegynt, før du kan legge til en ny periode")
            )
        }
    }

    @Test
    fun `opprettNyttVilkår - skal opprette nytt vilkår dersom det ikke eksisterer uvurdert vilkår av samme type på person`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true, "test")

        val request =
            """
                {
                  "personIdent": "${søker.aktivFødselsnummer()}",
                   "vilkårType": "BOR_MED_SØKER"
                  }
                }
            """.trimIndent()

        Given {
            header("Authorization", "Bearer $token")
            body(request)
            contentType(MediaType.APPLICATION_JSON_VALUE)
        } When {
            post("$vilkårsvurderingControllerUrl/${behandling.id}")
        } Then {
            body("data.personResultater[0].vilkårResultater.size()", Is(3))
            body(
                "data.personResultater[0].vilkårResultater.find {it.vilkårType == 'BOR_MED_SØKER'}",
                Is(notNullValue())
            )
        }
    }

    @Test
    fun `slettVilkår - skal kaste feil dersom det forsøkes å slette vilkår som ikke eksisterer`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true, "test")

        val ikkeEksisterendeVilkårId = 404

        val request =
            """
            ${søker.aktivFødselsnummer()}
            """.trimIndent()

        Given {
            header("Authorization", "Bearer $token")
            body(request)
            contentType(MediaType.APPLICATION_JSON_VALUE)
        } When {
            delete("$vilkårsvurderingControllerUrl/${behandling.id}/$ikkeEksisterendeVilkårId")
        } Then {
            body("status", Is("FEILET"))
            body("melding", Is("Prøver å slette et vilkår som ikke finnes"))
            body("frontendFeilmelding", Is("Vilkåret du prøver å slette finnes ikke i systemet."))
        }
    }

    @Test
    fun `slettVilkår - skal lage nytt initiell vilkår av samme type dersom det bare finnes en ved sletting`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true, "test")

        val bosattIRiketVilkår = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)
            .personResultater.find { it.aktør == søker }?.vilkårResultater?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }!!

        val gammelVilkårId = bosattIRiketVilkår.id

        val request =
            """
            ${søker.aktivFødselsnummer()}
            """.trimIndent()

        Given {
            header("Authorization", "Bearer $token")
            body(request)
            contentType(MediaType.APPLICATION_JSON_VALUE)
        } When {
            delete("$vilkårsvurderingControllerUrl/${behandling.id}/${bosattIRiketVilkår.id}")
        } Then {
            body(
                "data.personResultater[0].vilkårResultater.find {it.vilkårType == 'BOSATT_I_RIKET'}.id",
                Is(not(gammelVilkårId))
            )
        }
    }

    @Test
    fun `endreAnnenVurdering - skal kaste feil dersom annen vurdering ikke finnes`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.SAKSBEHANDLER)
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true, "test")
        val request =
            """
                {
                    "id": 404,
                    "resultat": "OPPFYLT",
                    "type": "OPPLYSNINGSPLIKT",
                    "begrunnelse": "Begrunnelse"
                  }
                }
            """.trimIndent()
        Given {
            header("Authorization", "Bearer $token")
            body(request)
            contentType(MediaType.APPLICATION_JSON_VALUE)
        } When {
            put("$vilkårsvurderingControllerUrl/${behandling.id}/annenvurdering")
        } Then {
            body("status", Is("FUNKSJONELL_FEIL"))
            body("melding", Is("Annen vurdering med id 404 finnes ikke i db"))
        }
    }

    @Test
    fun `endreAnnenVurdering - skal oppdatere eksisterende annen vurdering`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.SAKSBEHANDLER)
        val personResultat = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)
            .personResultater.find { it.aktør == søker }
        val annenVurdering = annenVurderingRepository.saveAndFlush(
            AnnenVurdering(
                personResultat = personResultat!!,
                type = AnnenVurderingType.OPPLYSNINGSPLIKT
            )
        )
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true, "test")
        val request =
            """
                {
                    "id": ${annenVurdering.id},
                    "resultat": "OPPFYLT",
                    "type": "OPPLYSNINGSPLIKT",
                    "begrunnelse": "Begrunnelse"
                  }
                }
            """.trimIndent()
        Given {
            header("Authorization", "Bearer $token")
            body(request)
            contentType(MediaType.APPLICATION_JSON_VALUE)
        } When {
            put("$vilkårsvurderingControllerUrl/${behandling.id}/annenvurdering")
        } Then {
            body(
                "data.personResultater[0].andreVurderinger[0].resultat",
                Is(Resultat.OPPFYLT.name)
            )
            body(
                "data.personResultater[0].andreVurderinger[0].begrunnelse",
                Is("Begrunnelse")
            )
        }
    }
}
