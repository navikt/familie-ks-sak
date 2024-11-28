package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VilkårsvurderingTest {
    @Nested
    inner class ErOpplysningspliktVilkårOppfyltTest {
        @Test
        fun `skal exception om personresultater er tom`() {
            // Arrange
            val vilkårsvurdering =
                lagVilkårsvurdering().also { it.personResultater = emptySet() }

            // Act & assert
            val exception =
                assertThrows<NoSuchElementException> {
                    vilkårsvurdering.erOpplysningspliktVilkårOppfylt()
                }
            assertThat(exception.message).isEqualTo("Collection contains no element matching the predicate.")
        }

        @Test
        fun `skal kaste exception om ingen vilkår er for søker`() {
            // Arrange
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultat = { vilkårsvurdering ->
                        lagPersonResultat(
                            vilkårsvurdering = vilkårsvurdering,
                            aktør = vilkårsvurdering.behandling.fagsak.aktør,
                            lagVilkårResultater = { personResultat ->
                                setOf(
                                    lagVilkårResultat(
                                        personResultat = personResultat,
                                        vilkårType = Vilkår.BARNETS_ALDER,
                                    ),
                                )
                            },
                            lagAnnenVurdering = { emptySet() },
                        )
                    },
                )

            // Act & assert
            val exception =
                assertThrows<NoSuchElementException> {
                    vilkårsvurdering.erOpplysningspliktVilkårOppfylt()
                }
            assertThat(exception.message).isEqualTo("Collection contains no element matching the predicate.")
        }

        @Test
        fun `skal returnere false om annen vurdering er tom`() {
            // Arrange
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultat = { vilkårsvurdering ->
                        lagPersonResultat(
                            vilkårsvurdering = vilkårsvurdering,
                            aktør = vilkårsvurdering.behandling.fagsak.aktør,
                            lagVilkårResultater = { personResultat ->
                                setOf(
                                    lagVilkårResultat(
                                        personResultat = personResultat,
                                        vilkårType = Vilkår.BOSATT_I_RIKET,
                                    ),
                                )
                            },
                            lagAnnenVurdering = { emptySet() },
                        )
                    },
                )

            // Act
            val erOpplysningspliktVilkårOppfylt = vilkårsvurdering.erOpplysningspliktVilkårOppfylt()

            // Assert
            assertThat(erOpplysningspliktVilkårOppfylt).isFalse()
        }

        @Test
        fun `skal returnere false om annen vurdering opplysningsplikt ikke er oppfylt`() {
            // Arrange
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultat = { vilkårsvurdering ->
                        lagPersonResultat(
                            vilkårsvurdering = vilkårsvurdering,
                            aktør = vilkårsvurdering.behandling.fagsak.aktør,
                            lagVilkårResultater = { personResultat ->
                                setOf(
                                    lagVilkårResultat(
                                        personResultat = personResultat,
                                        vilkårType = Vilkår.BOSATT_I_RIKET,
                                    ),
                                )
                            },
                            lagAnnenVurdering = {
                                setOf(
                                    AnnenVurdering(personResultat = it, type = AnnenVurderingType.OPPLYSNINGSPLIKT, resultat = Resultat.IKKE_OPPFYLT),
                                )
                            },
                        )
                    },
                )

            // Act
            val erOpplysningspliktVilkårOppfylt = vilkårsvurdering.erOpplysningspliktVilkårOppfylt()

            // Assert
            assertThat(erOpplysningspliktVilkårOppfylt).isFalse()
        }

        @Test
        fun `skal returnere true om annen vurdering opplysningsplikt er oppfylt`() {
            // Arrange
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultat = { vilkårsvurdering ->
                        lagPersonResultat(
                            vilkårsvurdering = vilkårsvurdering,
                            aktør = vilkårsvurdering.behandling.fagsak.aktør,
                            lagVilkårResultater = { personResultat ->
                                setOf(
                                    lagVilkårResultat(
                                        personResultat = personResultat,
                                        vilkårType = Vilkår.BOSATT_I_RIKET,
                                    ),
                                )
                            },
                            lagAnnenVurdering = {
                                setOf(
                                    AnnenVurdering(personResultat = it, type = AnnenVurderingType.OPPLYSNINGSPLIKT, resultat = Resultat.OPPFYLT),
                                )
                            },
                        )
                    },
                )

            // Act
            val erOpplysningspliktVilkårOppfylt = vilkårsvurdering.erOpplysningspliktVilkårOppfylt()

            // Assert
            assertThat(erOpplysningspliktVilkårOppfylt).isTrue()
        }
    }
}
