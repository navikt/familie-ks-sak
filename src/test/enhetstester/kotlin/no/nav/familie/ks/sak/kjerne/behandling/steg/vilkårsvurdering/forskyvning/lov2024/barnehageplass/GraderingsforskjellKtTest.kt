package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lov2024.barnehageplass

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.regelverkFørFebruar2025.lov2024.barnehageplass.Graderingsforskjell
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.regelverkFørFebruar2025.lov2024.barnehageplass.finnGraderingsforskjellMellomDenneOgForrigePeriode2024
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class GraderingsforskjellKtTest {
    @Test
    fun `skal utlede REDUKSJON_TIL_BARNEHAGEPLASS_SAMME_MÅNED_SOM_ANDRE_VILKÅR_FØRST_BLIR_OPPFYLT når det er en reduksjon til deltid barnehageplass i graderingen til vilkårsresultatet fra forrige og denne perioden og alle andre vilkår er oppfylt samme måned`() {
        // Arrange
        val vilkårResultatForrigePeriode =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = null,
                periodeTom = LocalDate.of(2024, 1, 15),
                antallTimer = BigDecimal(0),
            )
        val vilkårResultatDennePeriode =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = LocalDate.of(2024, 1, 16),
                periodeTom = null,
                antallTimer = BigDecimal(20),
            )

        val tidligsteÅrMånedAlleAndreVilkårErOppfylt = YearMonth.of(2024, 1)

        // Act
        val graderingsforskjell =
            finnGraderingsforskjellMellomDenneOgForrigePeriode2024(
                vilkårResultatForrigePeriode,
                vilkårResultatDennePeriode,
                tidligsteÅrMånedAlleAndreVilkårErOppfylt,
            )

        // Assert
        assertThat(graderingsforskjell).isEqualTo(
            Graderingsforskjell.REDUKSJON_TIL_BARNEHAGEPLASS_SAMME_MÅNED_SOM_ANDRE_VILKÅR_FØRST_BLIR_OPPFYLT,
        )
    }

    @Test
    fun `skal utlede ØKNING_FRA_FULL_BARNEHAGEPLASS når det er en økning fra full barnehageplass i graderingen til vilkårsresultatet fra forrige og denne perioden`() {
        // Arrange
        val vilkårResultatForrigePeriode =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = null,
                periodeTom = LocalDate.of(2024, 1, 15),
                antallTimer = BigDecimal(40),
            )
        val vilkårResultatDennePeriode =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = LocalDate.of(2024, 1, 16),
                periodeTom = null,
                antallTimer = BigDecimal(20),
            )

        // Act
        val graderingsforskjell =
            finnGraderingsforskjellMellomDenneOgForrigePeriode2024(
                vilkårResultatForrigePeriode,
                vilkårResultatDennePeriode,
                null,
            )

        // Assert
        assertThat(graderingsforskjell).isEqualTo(
            Graderingsforskjell.ØKNING_FRA_FULL_BARNEHAGEPLASS,
        )
    }

    @Test
    fun `skal utlede REDUKSJON når det er en reduksjon i gradering mellom vilkårsresultatet fra forrige og denne perioden`() {
        // Arrange
        val vilkårResultatForrigePeriode =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = null,
                periodeTom = LocalDate.of(2024, 1, 15),
                antallTimer = BigDecimal(10),
            )
        val vilkårResultatDennePeriode =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = LocalDate.of(2024, 1, 16),
                periodeTom = null,
                antallTimer = BigDecimal(20),
            )

        // Act
        val graderingsforskjell =
            finnGraderingsforskjellMellomDenneOgForrigePeriode2024(
                vilkårResultatForrigePeriode,
                vilkårResultatDennePeriode,
                null,
            )

        // Assert
        assertThat(graderingsforskjell).isEqualTo(
            Graderingsforskjell.REDUKSJON,
        )
    }

    @Test
    fun `skal utlede ØKNING når det er en økning i gradering mellom vilkårsresultatet fra forrige og denne perioden`() {
        // Arrange
        val vilkårResultatForrigePeriode =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = null,
                periodeTom = LocalDate.of(2024, 1, 15),
                antallTimer = BigDecimal(30),
            )
        val vilkårResultatDennePeriode =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = LocalDate.of(2024, 1, 16),
                periodeTom = null,
                antallTimer = BigDecimal(20),
            )

        // Act
        val graderingsforskjell =
            finnGraderingsforskjellMellomDenneOgForrigePeriode2024(
                vilkårResultatForrigePeriode,
                vilkårResultatDennePeriode,
                null,
            )

        // Assert
        assertThat(graderingsforskjell).isEqualTo(
            Graderingsforskjell.ØKNING,
        )
    }

    @Test
    fun `skal utlede ØKNING når det er en økning i gradering mellom vilkårsresultatet fra forrige og denne perioden da vilkårresultatet fra forrige perioden er null`() {
        // Arrange
        val vilkårResultatDennePeriode =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = LocalDate.of(2024, 1, 1),
                periodeTom = LocalDate.of(2024, 1, 31),
                antallTimer = BigDecimal(20),
            )

        // Act
        val graderingsforskjell =
            finnGraderingsforskjellMellomDenneOgForrigePeriode2024(
                null,
                vilkårResultatDennePeriode,
                null,
            )

        // Assert
        assertThat(graderingsforskjell).isEqualTo(
            Graderingsforskjell.ØKNING,
        )
    }

    @Test
    fun `skal utlede ØKNING når det er en økning i gradering mellom vilkårsresultatet fra forrige og denne perioden da vilkårresultatet fra denne perioden har null antall timer`() {
        // Arrange
        val vilkårResultatForrigePeriode =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = null,
                periodeTom = LocalDate.of(2024, 1, 15),
                antallTimer = BigDecimal(10),
            )

        val vilkårResultatDennePeriode =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = LocalDate.of(2024, 1, 16),
                periodeTom = null,
                antallTimer = null,
            )

        // Act
        val graderingsforskjell =
            finnGraderingsforskjellMellomDenneOgForrigePeriode2024(
                vilkårResultatForrigePeriode,
                vilkårResultatDennePeriode,
                null,
            )

        // Assert
        assertThat(graderingsforskjell).isEqualTo(
            Graderingsforskjell.ØKNING,
        )
    }

    @Test
    fun `skal utlede LIK når det er ingen endring i gradering mellom vilkårsresultatet fra forrige og denne perioden`() {
        // Arrange
        val vilkårResultatForrigePeriode =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = null,
                periodeTom = LocalDate.of(2024, 1, 15),
                antallTimer = BigDecimal(20),
            )
        val vilkårResultatDennePeriode =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = LocalDate.of(2024, 1, 16),
                periodeTom = null,
                antallTimer = BigDecimal(20),
            )

        // Act
        val graderingsforskjell =
            finnGraderingsforskjellMellomDenneOgForrigePeriode2024(
                vilkårResultatForrigePeriode,
                vilkårResultatDennePeriode,
                null,
            )

        // Assert
        assertThat(graderingsforskjell).isEqualTo(
            Graderingsforskjell.LIK,
        )
    }
}
