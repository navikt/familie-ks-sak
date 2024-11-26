package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2021.standard

import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.TilknyttetVilkårResultater
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TomForskyverKtTest {
    @Test
    fun `skal returnere null når gjeldene vilkårresultat tom dato er null`() {
        // Arrange
        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                gjeldende =
                    lagVilkårResultat(
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        periodeFom = LocalDate.of(2024, 7, 31),
                        periodeTom = null,
                    ),
                neste = null,
            )

        // Act
        val forskjøvetTom = forskyvTom(tilknyttetVilkårResultater)

        // Assert
        assertThat(forskjøvetTom).isNull()
    }

    @Test
    fun `skal returnere siste dag i måneden når gjeldende vilkårresultat slutter dagen først neste vilkårresultat begynner og gjeldende slutter på siste dag i måneden`() {
        // Arrange
        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                gjeldende =
                    lagVilkårResultat(
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        periodeFom = LocalDate.of(2024, 7, 31),
                        periodeTom = LocalDate.of(2024, 8, 1),
                    ),
                neste =
                    lagVilkårResultat(
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        periodeFom = LocalDate.of(2024, 11, 1),
                        periodeTom = LocalDate.of(2024, 12, 31),
                    ),
            )

        // Act
        val forskjøvetTom = forskyvTom(tilknyttetVilkårResultater)

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 7, 31))
    }

    @Test
    fun `skal returnere siste dag i forrige måneden når gjeldende vilkårresultat slutter dagen først neste vilkårresultat begynner og gjeldende slutter på første dag i måneden`() {
        // Arrange
        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                gjeldende =
                    lagVilkårResultat(
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        periodeFom = LocalDate.of(2024, 3, 1),
                        periodeTom = LocalDate.of(2024, 7, 1),
                    ),
                neste =
                    lagVilkårResultat(
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        periodeFom = LocalDate.of(2024, 7, 2),
                        periodeTom = LocalDate.of(2024, 12, 31),
                    ),
            )

        // Act
        val forskjøvetTom = forskyvTom(tilknyttetVilkårResultater)

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 7, 31))
    }

    @Test
    fun `skal returnere gjeldende vilkårresultat tom om man skal ha utbetaling i juli 2024 pga lovendringer`() {
        // Arrange
        val gjeldendeVilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = DATO_LOVENDRING_2024.minusYears(1),
                periodeTom = DATO_LOVENDRING_2024.minusDays(1),
            )

        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                gjeldende = gjeldendeVilkårResultat,
                neste = null,
            )

        // Act
        val forskjøvetTom = forskyvTom(tilknyttetVilkårResultater)

        // Assert
        assertThat(forskjøvetTom).isEqualTo(gjeldendeVilkårResultat.periodeTom)
    }

    @Test
    fun `skal returnere siste dag i forrige måned når BARNETS_ALDER vilkår fom dato er et år og en dag før dato for lovendring 2024`() {
        // Arrange
        val gjeldendeVilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = DATO_LOVENDRING_2024.minusDays(1).minusYears(1),
                periodeTom = DATO_LOVENDRING_2024.minusDays(1),
            )

        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                gjeldende = gjeldendeVilkårResultat,
                neste = null,
            )

        // Act
        val forskjøvetTom = forskyvTom(tilknyttetVilkårResultater)

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 6, 30))
    }

    @Test
    fun `skal returnere siste dag i forrige måned når BARNETS_ALDER vilkår tom dato er ett år for lovendring 2024`() {
        // Arrange
        val gjeldendeVilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = DATO_LOVENDRING_2024.minusYears(1),
                periodeTom = DATO_LOVENDRING_2024,
            )

        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                gjeldende = gjeldendeVilkårResultat,
                neste = null,
            )

        // Act
        val forskjøvetTom = forskyvTom(tilknyttetVilkårResultater)

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 7, 31))
    }

    @Test
    fun `skal returnere siste dag i forrige måned når BARNETS_ALDER vilkår tom dato er et år og 2 dager før lovendring 2024`() {
        // Arrange
        val gjeldendeVilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = DATO_LOVENDRING_2024.minusDays(2).minusYears(1),
                periodeTom = DATO_LOVENDRING_2024,
            )

        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                gjeldende = gjeldendeVilkårResultat,
                neste = null,
            )

        // Act
        val forskjøvetTom = forskyvTom(tilknyttetVilkårResultater)

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 7, 31))
    }

    @Test
    fun `skal returnere siste dag i forrige måned når BARNETS_ALDER vilkår fom dato er null`() {
        // Arrange
        val gjeldendeVilkårResultat =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = null,
                periodeTom = DATO_LOVENDRING_2024.minusDays(1),
            )

        val tilknyttetVilkårResultater =
            TilknyttetVilkårResultater(
                gjeldende = gjeldendeVilkårResultat,
                neste = null,
            )

        // Act
        val forskjøvetTom = forskyvTom(tilknyttetVilkårResultater)

        // Assert
        assertThat(forskjøvetTom).isEqualTo(gjeldendeVilkårResultat.periodeTom)
    }
}
