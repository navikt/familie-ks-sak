package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lov2021.barnehageplass

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.regelverkFørFebruar2025.lov2021.barnehageplass.Graderingsforskjell
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.regelverkFørFebruar2025.lov2021.barnehageplass.finnGraderingsforskjellMellomDenneOgForrigePeriode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class GraderingsforskjellKtTest {
    @Test
    fun `skal returnere graderingsforksjell REDUKSJON_GÅR_TIL_INGEN_UTBETALING når det er en reduksjon til null utbetaling mellom denne og forrige periodes vilkårresultat`() {
        // Arrange
        val vilkårResultatForrigePeriode = lagVilkårResultat(antallTimer = BigDecimal(20))
        val vilkårResultatDennePerioden = lagVilkårResultat(antallTimer = BigDecimal(40))

        // Act
        val graderingsforskjell =
            finnGraderingsforskjellMellomDenneOgForrigePeriode(
                vilkårResultatForrigePeriode = vilkårResultatForrigePeriode,
                vilkårResultatDennePerioden = vilkårResultatDennePerioden,
                erFørstePeriode = false,
            )

        // Assert
        assertThat(graderingsforskjell).isEqualTo(Graderingsforskjell.REDUKSJON_GÅR_TIL_INGEN_UTBETALING)
    }

    @Test
    fun `skal returnere graderingsforksjell REDUKSJON når det er en reduksjon til ikke null utbetaling mellom denne og forrige periodes vilkårresultat`() {
        // Arrange
        val vilkårResultatForrigePeriode = lagVilkårResultat(antallTimer = BigDecimal(10))
        val vilkårResultatDennePerioden = lagVilkårResultat(antallTimer = BigDecimal(20))

        // Act
        val graderingsforskjell =
            finnGraderingsforskjellMellomDenneOgForrigePeriode(
                vilkårResultatForrigePeriode = vilkårResultatForrigePeriode,
                vilkårResultatDennePerioden = vilkårResultatDennePerioden,
                erFørstePeriode = false,
            )

        // Assert
        assertThat(graderingsforskjell).isEqualTo(Graderingsforskjell.REDUKSJON)
    }

    @Test
    fun `skal returnere graderingsforksjell ØKNING_GRUNNET_SLUTT_I_BARNEHAGE når det er en økning på grunn av slutt i barnehage mellom denne og forrige periodes vilkårresultat`() {
        // Arrange
        val vilkårResultatForrigePeriode = lagVilkårResultat(antallTimer = BigDecimal(10))
        val vilkårResultatDennePerioden = lagVilkårResultat(antallTimer = null)

        // Act
        val graderingsforskjell =
            finnGraderingsforskjellMellomDenneOgForrigePeriode(
                vilkårResultatForrigePeriode = vilkårResultatForrigePeriode,
                vilkårResultatDennePerioden = vilkårResultatDennePerioden,
                erFørstePeriode = false,
            )

        // Assert
        assertThat(graderingsforskjell).isEqualTo(Graderingsforskjell.ØKNING_GRUNNET_SLUTT_I_BARNEHAGE)
    }

    @Test
    fun `skal returnere graderingsforksjell INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING når det er en økning fra max timer i forrige periodes vilkårresultat til mindre timer i denne periodens vilkårresultater og det er første periode`() {
        // Arrange
        val vilkårResultatForrigePeriode = lagVilkårResultat(antallTimer = BigDecimal(40))
        val vilkårResultatDennePerioden = lagVilkårResultat(antallTimer = BigDecimal(5))

        // Act
        val graderingsforskjell =
            finnGraderingsforskjellMellomDenneOgForrigePeriode(
                vilkårResultatForrigePeriode = vilkårResultatForrigePeriode,
                vilkårResultatDennePerioden = vilkårResultatDennePerioden,
                erFørstePeriode = true,
            )

        // Assert
        assertThat(graderingsforskjell).isEqualTo(Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING)
    }

    @Test
    fun `skal returnere graderingsforksjell INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING når det er en økning fra max timer i forrige periodes vilkårresultat til mindre timer i denne periodens vilkårresultater og det ikke er første periode`() {
        // Arrange
        val vilkårResultatForrigePeriode = lagVilkårResultat(antallTimer = BigDecimal(40))
        val vilkårResultatDennePerioden = lagVilkårResultat(antallTimer = BigDecimal(5))

        // Act
        val graderingsforskjell =
            finnGraderingsforskjellMellomDenneOgForrigePeriode(
                vilkårResultatForrigePeriode = vilkårResultatForrigePeriode,
                vilkårResultatDennePerioden = vilkårResultatDennePerioden,
                erFørstePeriode = false,
            )

        // Assert
        assertThat(graderingsforskjell).isEqualTo(Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING)
    }

    @Test
    fun `skal returnere graderingsforksjell ØKNING når det er en økning mellom forrige og denne periodes vilkårresultat`() {
        // Arrange
        val vilkårResultatForrigePeriode = lagVilkårResultat(antallTimer = BigDecimal(20))
        val vilkårResultatDennePerioden = lagVilkårResultat(antallTimer = BigDecimal(10))

        // Act
        val graderingsforskjell =
            finnGraderingsforskjellMellomDenneOgForrigePeriode(
                vilkårResultatForrigePeriode = vilkårResultatForrigePeriode,
                vilkårResultatDennePerioden = vilkårResultatDennePerioden,
                erFørstePeriode = true,
            )

        // Assert
        assertThat(graderingsforskjell).isEqualTo(Graderingsforskjell.ØKNING)
    }

    @Test
    fun `skal returnere graderingsforksjell LIK når både forrige periodes og denne periodes vilkårresultat er null`() {
        // Act
        val graderingsforskjell =
            finnGraderingsforskjellMellomDenneOgForrigePeriode(
                vilkårResultatForrigePeriode = null,
                vilkårResultatDennePerioden = null,
                erFørstePeriode = false,
            )

        // Assert
        assertThat(graderingsforskjell).isEqualTo(Graderingsforskjell.LIK)
    }

    @Test
    fun `skal returnere graderingsforksjell LIK når forrige periodes vilkårresultat denne vilkårresultat er samme antall timer`() {
        // Act
        val graderingsforskjell =
            finnGraderingsforskjellMellomDenneOgForrigePeriode(
                vilkårResultatForrigePeriode = lagVilkårResultat(antallTimer = BigDecimal(20)),
                vilkårResultatDennePerioden = lagVilkårResultat(antallTimer = BigDecimal(20)),
                erFørstePeriode = false,
            )

        // Assert
        assertThat(graderingsforskjell).isEqualTo(Graderingsforskjell.LIK)
    }
}
