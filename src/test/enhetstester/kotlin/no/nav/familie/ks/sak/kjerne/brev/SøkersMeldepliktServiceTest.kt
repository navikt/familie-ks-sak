package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class SøkersMeldepliktServiceTest {
    private val mockedVilkårsvurderingService: VilkårsvurderingService = mockk()
    private val søkersMeldepliktService: SøkersMeldepliktService =
        SøkersMeldepliktService(
            vilkårsvurderingService = mockedVilkårsvurderingService,
        )

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
        fun `skal returnere true om søker har meldt fra om barnehageplass`() {
            // Arrange
            val vedtak = lagVedtak()

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
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
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

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

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
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
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

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

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = {
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
                        )
                    },
                )

            every {
                mockedVilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

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
