package no.nav.familie.ks.sak.api

import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.lagBehandlingStegTilstand
import no.nav.familie.ks.sak.data.lagVedtaksbegrunnelse
import no.nav.familie.ks.sak.data.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.fake.FakeIntegrasjonKlient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.hamcrest.CoreMatchers.`is` as Is

@Disabled
class VilkårsvurderingControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Autowired
    private lateinit var annenVurderingRepository: AnnenVurderingRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var vedtaksperiodeRepository: VedtaksperiodeRepository

    @Autowired
    private lateinit var fakeIntegrasjonKlient: FakeIntegrasjonKlient

    val vilkårsvurderingControllerUrl = "/api/vilkårsvurdering"

    private lateinit var token: String

    @BeforeEach
    fun setup() {
        RestAssured.port = port
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(behandling.id)
        vilkårsvurderingService.opprettVilkårsvurdering(behandling, null)
        arbeidsfordelingPåBehandlingRepository.save(
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling.id,
                behandlendeEnhetId = "test",
                behandlendeEnhetNavn = "test",
            ),
        )
        behandling.behandlingStegTilstand.add(
            lagBehandlingStegTilstand(behandling, BehandlingSteg.VILKÅRSVURDERING, BehandlingStegStatus.KLAR),
        )
        lagreBehandling(behandling)

        token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
        fakeIntegrasjonKlient.reset()
    }

    @Test
    fun `endreVilkår - skal returnere endrede vilkår`() {
        val bosattIRiketVilkår =
            vilkårsvurderingService
                .hentAktivVilkårsvurderingForBehandling(behandling.id)
                .personResultater
                .find { it.aktør == søker }
                ?.vilkårResultater
                ?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }!!

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
                Is("2022-10-06"),
            )
            statusCode(HttpStatus.OK.value())
        }
    }

    @Test
    fun `endreVilkår - skal endre vilkår og tilbakefører behandling til vilkårsvurdering`() {
        val behandlingForOppdatering = behandlingRepository.hentAktivBehandling(behandling.id)
        behandlingForOppdatering.behandlingStegTilstand.clear()
        behandlingForOppdatering.behandlingStegTilstand.addAll(
            setOf(
                lagBehandlingStegTilstand(
                    behandlingForOppdatering,
                    BehandlingSteg.VILKÅRSVURDERING,
                    BehandlingStegStatus.UTFØRT,
                ),
                lagBehandlingStegTilstand(
                    behandlingForOppdatering,
                    BehandlingSteg.BEHANDLINGSRESULTAT,
                    BehandlingStegStatus.UTFØRT,
                ),
                lagBehandlingStegTilstand(
                    behandlingForOppdatering,
                    BehandlingSteg.SIMULERING,
                    BehandlingStegStatus.UTFØRT,
                ),
                lagBehandlingStegTilstand(
                    behandlingForOppdatering,
                    BehandlingSteg.VEDTAK,
                    BehandlingStegStatus.KLAR,
                ),
            ),
        )

        lagreBehandling(behandlingForOppdatering)

        lagVedtakOgVedtaksperiode()

        val bosattIRiketVilkår =
            vilkårsvurderingService
                .hentAktivVilkårsvurderingForBehandling(behandling.id)
                .personResultater
                .find {
                    it.aktør == søker
                }?.vilkårResultater
                ?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }!!

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
                Is("2022-10-06"),
            )
            statusCode(HttpStatus.OK.value())
        }

        assertTrue { vedtaksperiodeRepository.finnVedtaksperioderForVedtak(vedtak.id).isEmpty() }

        val oppdatertBehandling = behandlingRepository.hentAktivBehandling(behandlingForOppdatering.id)
        assertBehandlingHarStegOgStatus(oppdatertBehandling, BehandlingSteg.VILKÅRSVURDERING, BehandlingStegStatus.KLAR)
        assertBehandlingHarStegOgStatus(
            oppdatertBehandling,
            BehandlingSteg.BEHANDLINGSRESULTAT,
            BehandlingStegStatus.TILBAKEFØRT,
        )
        assertBehandlingHarStegOgStatus(
            oppdatertBehandling,
            BehandlingSteg.SIMULERING,
            BehandlingStegStatus.TILBAKEFØRT,
        )
        assertBehandlingHarStegOgStatus(oppdatertBehandling, BehandlingSteg.VEDTAK, BehandlingStegStatus.TILBAKEFØRT)
    }

    @Test
    fun `opprettNyttVilkår - skal kaste feil dersom det eksisterer uvurdert vilkår av samme type`() {
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
                Is("Du må ferdigstille vilkårsvurderingen på en periode som allerede er påbegynt, før du kan legge til en ny periode"),
            )
        }
    }

    @Test
    fun `opprettNyttVilkår - skal opprette nytt vilkår dersom det ikke eksisterer uvurdert vilkår av samme type på person`() {
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
                Is(notNullValue()),
            )
        }
    }

    @Test
    fun `opprettNyttVilkår - skal opprette nytt vilkår og tilbakefører behandling til vilkårsvurdering steg`() {
        val behandlingForOppdatering = behandlingRepository.hentAktivBehandling(behandling.id)
        behandlingForOppdatering.behandlingStegTilstand.clear()
        behandlingForOppdatering.behandlingStegTilstand.addAll(
            setOf(
                lagBehandlingStegTilstand(
                    behandlingForOppdatering,
                    BehandlingSteg.VILKÅRSVURDERING,
                    BehandlingStegStatus.UTFØRT,
                ),
                lagBehandlingStegTilstand(
                    behandlingForOppdatering,
                    BehandlingSteg.BEHANDLINGSRESULTAT,
                    BehandlingStegStatus.UTFØRT,
                ),
                lagBehandlingStegTilstand(
                    behandlingForOppdatering,
                    BehandlingSteg.SIMULERING,
                    BehandlingStegStatus.UTFØRT,
                ),
                lagBehandlingStegTilstand(
                    behandlingForOppdatering,
                    BehandlingSteg.VEDTAK,
                    BehandlingStegStatus.KLAR,
                ),
            ),
        )

        lagreBehandling(behandlingForOppdatering)

        lagVedtakOgVedtaksperiode()

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
                Is(notNullValue()),
            )
        }

        assertTrue { vedtaksperiodeRepository.finnVedtaksperioderForVedtak(vedtak.id).isEmpty() }

        val oppdatertBehandling = behandlingRepository.hentAktivBehandling(behandlingForOppdatering.id)
        assertBehandlingHarStegOgStatus(oppdatertBehandling, BehandlingSteg.VILKÅRSVURDERING, BehandlingStegStatus.KLAR)
        assertBehandlingHarStegOgStatus(
            oppdatertBehandling,
            BehandlingSteg.BEHANDLINGSRESULTAT,
            BehandlingStegStatus.TILBAKEFØRT,
        )
        assertBehandlingHarStegOgStatus(
            oppdatertBehandling,
            BehandlingSteg.SIMULERING,
            BehandlingStegStatus.TILBAKEFØRT,
        )
        assertBehandlingHarStegOgStatus(oppdatertBehandling, BehandlingSteg.VEDTAK, BehandlingStegStatus.TILBAKEFØRT)
    }

    @Test
    fun `slettVilkår - skal kaste feil dersom det forsøkes å slette vilkår som ikke eksisterer`() {
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
        val bosattIRiketVilkår =
            vilkårsvurderingService
                .hentAktivVilkårsvurderingForBehandling(behandling.id)
                .personResultater
                .find { it.aktør == søker }
                ?.vilkårResultater
                ?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }!!

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
                Is(not(gammelVilkårId)),
            )
            body("status", Is("SUKSESS"))
        }
    }

    @Test
    fun `endreAnnenVurdering - skal kaste feil dersom annen vurdering ikke finnes`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.SAKSBEHANDLER)
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
        val personResultat =
            vilkårsvurderingService
                .hentAktivVilkårsvurderingForBehandling(behandling.id)
                .personResultater
                .find { it.aktør == søker }
        val annenVurdering =
            annenVurderingRepository.saveAndFlush(
                AnnenVurdering(
                    personResultat = personResultat!!,
                    type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                ),
            )
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
                Is(Resultat.OPPFYLT.name),
            )
            body(
                "data.personResultater[0].andreVurderinger[0].begrunnelse",
                Is("Begrunnelse"),
            )
        }
    }

    private fun lagVedtakOgVedtaksperiode() {
        lagVedtak()
        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak)
        vedtaksperiodeMedBegrunnelser.begrunnelser.clear()
        vedtaksperiodeMedBegrunnelser.begrunnelser.add(lagVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser))
        vedtaksperiodeRepository.saveAndFlush(vedtaksperiodeMedBegrunnelser)
    }

    private fun assertBehandlingHarStegOgStatus(
        behandling: Behandling,
        behandlingSteg: BehandlingSteg,
        behandlingStegStatus: BehandlingStegStatus,
    ) = assertTrue(
        behandling.behandlingStegTilstand.any {
            it.behandlingSteg == behandlingSteg && it.behandlingStegStatus == behandlingStegStatus
        },
    )
}
