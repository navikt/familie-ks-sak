package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagInitieltTilkjentYtelse
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.YearMonth

class SøkersMeldepliktServiceTest {
    private val vilkårsvurderingService: VilkårsvurderingService = mockk()
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository = mockk()
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val søkersMeldepliktService: SøkersMeldepliktService =
        SøkersMeldepliktService(
            vilkårsvurderingService = vilkårsvurderingService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
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
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
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
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
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
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
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
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
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
        fun `skal returnere true om søker har meldt fra om barnehageplass på barn med andel i inneværende måned`() {
            // Arrange
            val inneværendeMåned = YearMonth.now()
            val vedtak = lagVedtak()
            val barn = lagPerson()

            val tilkjentYtelse = lagInitieltTilkjentYtelse(vedtak.behandling)
            val andelTilkjentYtelse =
                listOf(
                    lagAndelTilkjentYtelse(
                        tilkjentYtelse = tilkjentYtelse,
                        aktør = barn.aktør,
                        stønadFom = inneværendeMåned.minusMonths(1),
                        stønadTom = inneværendeMåned,
                    ),
                )

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                aktør = barn.aktør,
                                vilkårsvurdering = vilkårsvurdering,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = personResultat,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(vedtak.behandling.id)
            } returns andelTilkjentYtelse

            every {
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass = søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(vedtak)

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isTrue()
        }

        @Test
        fun `skal returnere false om søker ikke har meldt fra om barnehageplass på barn med andel i inneværende måned`() {
            // Arrange
            val inneværendeMåned = YearMonth.now()
            val vedtak = lagVedtak()
            val barn = lagPerson()

            val tilkjentYtelse = lagInitieltTilkjentYtelse(vedtak.behandling)
            val andelTilkjentYtelse =
                listOf(
                    lagAndelTilkjentYtelse(
                        tilkjentYtelse = tilkjentYtelse,
                        aktør = barn.aktør,
                        stønadFom = inneværendeMåned.minusMonths(1),
                        stønadTom = inneværendeMåned,
                    ),
                )

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                aktør = barn.aktør,
                                vilkårsvurdering = vilkårsvurdering,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = personResultat,
                                            søkerHarMeldtFraOmBarnehageplass = false,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(vedtak.behandling.id)
            } returns andelTilkjentYtelse

            every {
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass = søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(vedtak)

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isFalse()
        }

        @Test
        fun `skal returnere true om søker har meldt fra om barnehageplass på barn med framtidig andel`() {
            // Arrange
            val inneværendeMåned = YearMonth.now()
            val vedtak = lagVedtak()
            val barn = lagPerson()

            val tilkjentYtelse = lagInitieltTilkjentYtelse(vedtak.behandling)
            val andelTilkjentYtelse =
                listOf(
                    lagAndelTilkjentYtelse(
                        tilkjentYtelse = tilkjentYtelse,
                        aktør = barn.aktør,
                        stønadFom = inneværendeMåned.plusMonths(1),
                        stønadTom = inneværendeMåned.plusMonths(2),
                    ),
                )

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                aktør = barn.aktør,
                                vilkårsvurdering = vilkårsvurdering,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = personResultat,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(vedtak.behandling.id)
            } returns andelTilkjentYtelse

            every {
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass = søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(vedtak)

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isTrue()
        }

        @Test
        fun `skal returnere false om søker ikke har meldt fra om barnehageplass på barn med framtidig andel`() {
            // Arrange
            val inneværendeMåned = YearMonth.now()
            val vedtak = lagVedtak()
            val barn = lagPerson()

            val tilkjentYtelse = lagInitieltTilkjentYtelse(vedtak.behandling)
            val andelTilkjentYtelse =
                listOf(
                    lagAndelTilkjentYtelse(
                        tilkjentYtelse = tilkjentYtelse,
                        aktør = barn.aktør,
                        stønadFom = inneværendeMåned.plusMonths(1),
                        stønadTom = inneværendeMåned.plusMonths(2),
                    ),
                )

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                aktør = barn.aktør,
                                vilkårsvurdering = vilkårsvurdering,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = personResultat,
                                            søkerHarMeldtFraOmBarnehageplass = false,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(vedtak.behandling.id)
            } returns andelTilkjentYtelse

            every {
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass = søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(vedtak)

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isFalse()
        }

        @Test
        fun `skal returnere true om det ikke finnes barn som har relevant andel`() {
            // Arrange
            val inneværendeMåned = YearMonth.now()
            val vedtak = lagVedtak()
            val barn = lagPerson()

            val tilkjentYtelse = lagInitieltTilkjentYtelse(vedtak.behandling)
            val andelTilkjentYtelse =
                listOf(
                    lagAndelTilkjentYtelse(
                        tilkjentYtelse = tilkjentYtelse,
                        aktør = barn.aktør,
                        stønadFom = inneværendeMåned.minusMonths(2),
                        stønadTom = inneværendeMåned.minusMonths(1),
                    ),
                )

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                aktør = barn.aktør,
                                vilkårsvurdering = vilkårsvurdering,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = personResultat,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(vedtak.behandling.id)
            } returns andelTilkjentYtelse

            every {
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass = søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(vedtak)

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isTrue()
        }

        @Test
        fun `skal returnere true om søker har meldt fra om barnehageplass på alle barna med relevant andel selv om ikke alle barna er relevant`() {
            // Arrange
            val inneværendeMåned = YearMonth.now()
            val vedtak = lagVedtak()
            val barn1 = lagPerson()
            val barn2 = lagPerson()
            val barn3 = lagPerson()

            val tilkjentYtelse = lagInitieltTilkjentYtelse(vedtak.behandling)
            val andelTilkjentYtelse =
                listOf(
                    lagAndelTilkjentYtelse(
                        tilkjentYtelse = tilkjentYtelse,
                        aktør = barn1.aktør,
                        stønadFom = inneværendeMåned.minusMonths(2),
                        stønadTom = inneværendeMåned.minusMonths(1),
                    ),
                    lagAndelTilkjentYtelse(
                        tilkjentYtelse = tilkjentYtelse,
                        aktør = barn2.aktør,
                        stønadFom = inneværendeMåned,
                        stønadTom = inneværendeMåned.plusMonths(1),
                    ),
                    lagAndelTilkjentYtelse(
                        tilkjentYtelse = tilkjentYtelse,
                        aktør = barn3.aktør,
                        stønadFom = inneværendeMåned.plusMonths(2),
                        stønadTom = inneværendeMåned.plusMonths(3),
                    ),
                )

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                aktør = barn1.aktør,
                                vilkårsvurdering = vilkårsvurdering,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = personResultat,
                                            søkerHarMeldtFraOmBarnehageplass = false,
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                aktør = barn2.aktør,
                                vilkårsvurdering = vilkårsvurdering,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = personResultat,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                aktør = barn3.aktør,
                                vilkårsvurdering = vilkårsvurdering,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = personResultat,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(vedtak.behandling.id)
            } returns andelTilkjentYtelse

            every {
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn1, barn2, barn3)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass = søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(vedtak)

            // Assert
            assertThat(harSøkerMeldtFraOmBarnehagePlass).isTrue()
        }

        @Test
        fun `skal returnere false om søker ikke har meldt fra om barnehageplass på alle barna med relevant andel selv om ikke alle barna er relevant`() {
            // Arrange
            val inneværendeMåned = YearMonth.now()
            val vedtak = lagVedtak()
            val barn1 = lagPerson()
            val barn2 = lagPerson()
            val barn3 = lagPerson()

            val tilkjentYtelse = lagInitieltTilkjentYtelse(vedtak.behandling)
            val andelTilkjentYtelse =
                listOf(
                    lagAndelTilkjentYtelse(
                        tilkjentYtelse = tilkjentYtelse,
                        aktør = barn1.aktør,
                        stønadFom = inneværendeMåned.minusMonths(2),
                        stønadTom = inneværendeMåned.minusMonths(1),
                    ),
                    lagAndelTilkjentYtelse(
                        tilkjentYtelse = tilkjentYtelse,
                        aktør = barn2.aktør,
                        stønadFom = inneværendeMåned,
                        stønadTom = inneværendeMåned.plusMonths(1),
                    ),
                    lagAndelTilkjentYtelse(
                        tilkjentYtelse = tilkjentYtelse,
                        aktør = barn3.aktør,
                        stønadFom = inneværendeMåned.plusMonths(2),
                        stønadTom = inneværendeMåned.plusMonths(3),
                    ),
                )

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = vedtak.behandling,
                    lagPersonResultat = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                aktør = barn1.aktør,
                                vilkårsvurdering = vilkårsvurdering,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = personResultat,
                                            søkerHarMeldtFraOmBarnehageplass = false,
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                aktør = barn2.aktør,
                                vilkårsvurdering = vilkårsvurdering,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = personResultat,
                                            søkerHarMeldtFraOmBarnehageplass = true,
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                aktør = barn3.aktør,
                                vilkårsvurdering = vilkårsvurdering,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BARNEHAGEPLASS,
                                            personResultat = personResultat,
                                            søkerHarMeldtFraOmBarnehageplass = false,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every {
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(vedtak.behandling.id)
            } returns andelTilkjentYtelse

            every {
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
                    behandlingId = vedtak.behandling.id,
                )
            } returns vilkårsvurdering

            every {
                personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id)
            } returns listOf(barn1, barn2, barn3)

            // Act
            val harSøkerMeldtFraOmBarnehagePlass = søkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(vedtak)

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
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
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
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
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
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(
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
