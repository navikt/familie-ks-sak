package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class SøkersMeldepliktServiceTest {
    private val mockedVilkårsvurderingService: VilkårsvurderingService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val søkersMeldepliktService: SøkersMeldepliktService =
        SøkersMeldepliktService(
            vilkårsvurderingService = mockedVilkårsvurderingService,
            behandlingService = behandlingService,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
            featureToggleService = featureToggleService,
        )

    @BeforeEach
    fun setUp() {
        every { featureToggleService.isEnabled(FeatureToggle.BRUK_NY_LOGIKK_FOR_SØKERS_MELDEPLIKT) } returns true
    }

    @Nested
    inner class SkalMeldeFraOmEndringerEøsSelvstendigRettTest {
        @ParameterizedTest
        @EnumSource(
            value = Behandlingsresultat::class,
            names = [
                "AVSLÅTT",
                "ENDRET_OG_OPPHØRT",
                "OPPHØRT",
            ],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal returnere true om behandlingen har passende behandlingsresultat og utdypende vilkårsvurdering inneholder ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING og vilkårtypen er BOSATT_I_RIKET`(
            behandlingsresultat: Behandlingsresultat,
        ) {
            // Arrange
            val behandling =
                lagBehandling(
                    resultat = behandlingsresultat,
                )

            val vedtak =
                lagVedtak(
                    behandling = behandling,
                )

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = randomAktør(),
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            utdypendeVilkårsvurderinger =
                                                listOf(
                                                    UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING,
                                                ),
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            // Act
            val skalMeldeFraOmEndringerEøsSelvstendigRett =
                søkersMeldepliktService.skalSøkerMeldeFraOmEndringerEøsSelvstendigRett(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(skalMeldeFraOmEndringerEøsSelvstendigRett).isTrue()
        }

        @Test
        fun `skal returnere false om utdypende vilkårsvurdering ikke inneholder ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING`() {
            // Arrange
            val behandling =
                lagBehandling(
                    resultat = Behandlingsresultat.INNVILGET,
                )

            val vedtak =
                lagVedtak(
                    behandling = behandling,
                )

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = randomAktør(),
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            utdypendeVilkårsvurderinger = emptyList(),
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            // Act
            val skalMeldeFraOmEndringerEøsSelvstendigRett =
                søkersMeldepliktService.skalSøkerMeldeFraOmEndringerEøsSelvstendigRett(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(skalMeldeFraOmEndringerEøsSelvstendigRett).isFalse()
        }

        @Test
        fun `skal returnere false om vilkårtypen ikke er BOSATT_I_RIKET`() {
            // Arrange
            val behandling =
                lagBehandling(
                    resultat = Behandlingsresultat.INNVILGET,
                )

            val vedtak =
                lagVedtak(
                    behandling = behandling,
                )

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = randomAktør(),
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            vilkårType = Vilkår.BARNETS_ALDER,
                                            utdypendeVilkårsvurderinger =
                                                listOf(
                                                    UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING,
                                                ),
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            // Act
            val skalMeldeFraOmEndringerEøsSelvstendigRett =
                søkersMeldepliktService.skalSøkerMeldeFraOmEndringerEøsSelvstendigRett(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(skalMeldeFraOmEndringerEøsSelvstendigRett).isFalse()
        }

        @ParameterizedTest
        @EnumSource(
            value = Behandlingsresultat::class,
            names = [
                "AVSLÅTT",
                "ENDRET_OG_OPPHØRT",
                "OPPHØRT",
            ],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal returnere false om behandlingen ikke har passende behandlingsresultat`(
            behandlingsresultat: Behandlingsresultat,
        ) {
            // Arrange
            val behandling =
                lagBehandling(
                    resultat = behandlingsresultat,
                )

            val vedtak =
                lagVedtak(
                    behandling = behandling,
                )

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = randomAktør(),
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            utdypendeVilkårsvurderinger =
                                                listOf(
                                                    UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING,
                                                ),
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            // Act
            val skalMeldeFraOmEndringerEøsSelvstendigRett =
                søkersMeldepliktService.skalSøkerMeldeFraOmEndringerEøsSelvstendigRett(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(skalMeldeFraOmEndringerEøsSelvstendigRett).isFalse()
        }
    }

    @Nested
    inner class HarSøkerMeldtFraOmBarnehagePlassTest {
        @Test
        fun `skal returnere true om søker har meldt fra om barnehageplass på barnet som ikke var i forrige behandling`() {
            // Arrange
            val fagsak = lagFagsak()
            val behandling = lagBehandling(id = 1, fagsak = fagsak)
            val forrigeBehandling = lagBehandling(id = 2, fagsak = fagsak, status = BehandlingStatus.AVSLUTTET, resultat = Behandlingsresultat.INNVILGET)

            val vedtak = lagVedtak(behandling)

            val barn1 = lagPerson()
            val barn2 = lagPerson()

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
                        setOf(
                            lagPersonResultat(
                                aktør = barn1.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                aktør = barn2.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = false,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                behandlingService.hentForrigeBehandlingSomErVedtatt(vedtak.behandling)
            } returns forrigeBehandling

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn1, barn2)

            every {
                personopplysningGrunnlagService.hentBarnaThrows(forrigeBehandling.id)
            } returns listOf(barn2)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass =
                søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isTrue()
        }

        @Test
        fun `skal returnere false om søker kun har meldt fra om barnehageplass på barnet som også var i forrige behandling`() {
            // Arrange
            val fagsak = lagFagsak()
            val behandling = lagBehandling(id = 1, fagsak = fagsak)
            val forrigeBehandling = lagBehandling(id = 2, fagsak = fagsak, status = BehandlingStatus.AVSLUTTET, resultat = Behandlingsresultat.INNVILGET)

            val vedtak = lagVedtak(behandling)

            val barn1 = lagPerson()
            val barn2 = lagPerson()

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
                        setOf(
                            lagPersonResultat(
                                aktør = barn1.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = false,
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                aktør = barn2.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                behandlingService.hentForrigeBehandlingSomErVedtatt(vedtak.behandling)
            } returns forrigeBehandling

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn1, barn2)

            every {
                personopplysningGrunnlagService.hentBarnaThrows(forrigeBehandling.id)
            } returns listOf(barn2)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass =
                søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isFalse()
        }

        @Test
        fun `skal returnere true om søker har meldt fra om barnehageplass på både barnet som er ny i nåværende behandling og barnet i forrige behandling`() {
            // Arrange
            val fagsak = lagFagsak()
            val behandling = lagBehandling(id = 1, fagsak = fagsak)
            val forrigeBehandling = lagBehandling(id = 2, fagsak = fagsak, status = BehandlingStatus.AVSLUTTET, resultat = Behandlingsresultat.INNVILGET)

            val vedtak = lagVedtak(behandling)

            val barn1 = lagPerson()
            val barn2 = lagPerson()

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
                        setOf(
                            lagPersonResultat(
                                aktør = barn1.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                aktør = barn2.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                behandlingService.hentForrigeBehandlingSomErVedtatt(vedtak.behandling)
            } returns forrigeBehandling

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn1, barn2)

            every {
                personopplysningGrunnlagService.hentBarnaThrows(forrigeBehandling.id)
            } returns listOf(barn2)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass =
                søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isTrue()
        }

        @Test
        fun `skal returnere false om søker ikke har meldt fra om barnehageplass på et av de relevante barna`() {
            // Arrange
            val fagsak = lagFagsak()
            val behandling = lagBehandling(id = 1, fagsak = fagsak)

            val vedtak = lagVedtak(behandling)

            val barn1 = lagPerson()
            val barn2 = lagPerson()

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
                        setOf(
                            lagPersonResultat(
                                aktør = barn1.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                aktør = barn2.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = false,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                behandlingService.hentForrigeBehandlingSomErVedtatt(vedtak.behandling)
            } returns null

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn1, barn2)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass =
                søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isFalse()
        }

        @Test
        fun `skal returnere true kun hvis søker har meldt fra om barnehageplass på alle barna i en revurdering`() {
            // Arrange
            val fagsak = lagFagsak()
            val behandling = lagBehandling(id = 1, fagsak = fagsak, type = BehandlingType.REVURDERING)
            val forrigeBehandling = lagBehandling(id = 2, fagsak = fagsak, status = BehandlingStatus.AVSLUTTET, resultat = Behandlingsresultat.INNVILGET)

            val vedtak = lagVedtak(behandling)

            val barn1 = lagPerson()
            val barn2 = lagPerson()

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
                        setOf(
                            lagPersonResultat(
                                aktør = barn1.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                aktør = barn2.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                behandlingService.hentForrigeBehandlingSomErVedtatt(vedtak.behandling)
            } returns forrigeBehandling

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn1, barn2)

            every {
                personopplysningGrunnlagService.hentBarnaThrows(forrigeBehandling.id)
            } returns listOf(barn1, barn2)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass =
                søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isTrue()
        }

        @Test
        fun `skal returnere false hvis søker ikke har meldt fra om barnehageplass på alle barna i en revurdering`() {
            // Arrange
            val fagsak = lagFagsak()
            val behandling = lagBehandling(id = 1, fagsak = fagsak, type = BehandlingType.REVURDERING)
            val forrigeBehandling = lagBehandling(id = 2, fagsak = fagsak, status = BehandlingStatus.AVSLUTTET, resultat = Behandlingsresultat.INNVILGET)

            val vedtak = lagVedtak(behandling)

            val barn1 = lagPerson()
            val barn2 = lagPerson()

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
                        setOf(
                            lagPersonResultat(
                                aktør = barn1.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = false,
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                aktør = barn2.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                behandlingService.hentForrigeBehandlingSomErVedtatt(vedtak.behandling)
            } returns forrigeBehandling

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn1, barn2)

            every {
                personopplysningGrunnlagService.hentBarnaThrows(forrigeBehandling.id)
            } returns listOf(barn1, barn2)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass =
                søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isFalse()
        }

        @Test
        fun `skal kaste exception om ingen relevante barn finnes`() {
            // Arrange
            val fagsak = lagFagsak()
            val behandling = lagBehandling(id = 1, fagsak = fagsak)
            val forrigeBehandling = lagBehandling(id = 2, fagsak = fagsak, status = BehandlingStatus.AVSLUTTET, resultat = Behandlingsresultat.INNVILGET)

            val vedtak = lagVedtak(behandling)

            val barn = lagPerson()

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
                        setOf(
                            lagPersonResultat(
                                aktør = barn.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                aktør = barn.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = false,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                behandlingService.hentForrigeBehandlingSomErVedtatt(vedtak.behandling)
            } returns forrigeBehandling

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn)

            every {
                personopplysningGrunnlagService.hentBarnaThrows(forrigeBehandling.id)
            } returns listOf(barn)

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                        vedtak = vedtak,
                    )
                }
            assertThat(exception.message).isEqualTo("Forventer minst et relevant barn ment fant ingen for behandlingId=${behandling.id}")
        }

        @Test
        fun `skal returnere true om søker har meldt fra om barnehageplass`() {
            // Arrange
            val vedtak = lagVedtak()
            val barn = lagPerson()

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
                        setOf(
                            lagPersonResultat(
                                aktør = barn.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                behandlingService.hentForrigeBehandlingSomErVedtatt(vedtak.behandling)
            } returns null

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass =
                søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isTrue()
        }

        @Test
        fun `skal returnere false om søker ikke har meldt fra om barnehageplass`() {
            // Arrange
            val vedtak = lagVedtak()
            val barn = lagPerson()

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
                        setOf(
                            lagPersonResultat(
                                aktør = barn.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = false,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                behandlingService.hentForrigeBehandlingSomErVedtatt(vedtak.behandling)
            } returns null

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass =
                søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isFalse()
        }

        @Test
        fun `skal returnere false om søker har meldt fra om barnehageplass ikke er oppgitt`() {
            // Arrange
            val vedtak = lagVedtak()
            val barn = lagPerson()

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
                        setOf(
                            lagPersonResultat(
                                aktør = barn.aktør,
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = null,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                behandlingService.hentForrigeBehandlingSomErVedtatt(vedtak.behandling)
            } returns null

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass =
                søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isFalse()
        }

        @Test
        fun `skal returnere true om søker har meldt fra om barnehageplass når toggle er av`() {
            // Arrange
            val vedtak = lagVedtak()

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every { featureToggleService.isEnabled(FeatureToggle.BRUK_NY_LOGIKK_FOR_SØKERS_MELDEPLIKT) } returns false

            // Act
            val harSøkerMeldtFraOmBarnehagePlass =
                søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isTrue()
        }

        @Test
        fun `skal returnere false om søker ikke har meldt fra om barnehageplass når toggle er av`() {
            // Arrange
            val vedtak = lagVedtak()

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = false,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every { featureToggleService.isEnabled(FeatureToggle.BRUK_NY_LOGIKK_FOR_SØKERS_MELDEPLIKT) } returns false

            // Act
            val harSøkerMeldtFraOmBarnehagePlass =
                søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isFalse()
        }

        @Test
        fun `skal returnere false om søker har meldt fra om barnehageplass ikke er oppgitt når toggle er av`() {
            // Arrange
            val vedtak = lagVedtak()

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = it,
                                            søkerHarMeldtFraOmBarnehageplass = null,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every { featureToggleService.isEnabled(FeatureToggle.BRUK_NY_LOGIKK_FOR_SØKERS_MELDEPLIKT) } returns false

            // Act
            val harSøkerMeldtFraOmBarnehagePlass =
                søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                    vedtak = vedtak,
                )

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isFalse()
        }
    }
}
