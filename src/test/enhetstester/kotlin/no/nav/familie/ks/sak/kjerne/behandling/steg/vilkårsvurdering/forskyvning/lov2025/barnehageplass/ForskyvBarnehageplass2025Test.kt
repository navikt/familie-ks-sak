package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lov2025.barnehageplass

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.barnehageplass.forskyvBarnehageplassVilkår
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class ForskyvBarnehageplass2025Test {
    private val januar = YearMonth.of(2025, 1)
    private val februar = YearMonth.of(2025, 2)
    private val mars = YearMonth.of(2025, 3)
    private val april = YearMonth.of(2025, 4)
    private val juli = YearMonth.of(2025, 7)
    private val august = YearMonth.of(2025, 8)
    private val september = YearMonth.of(2025, 9)
    private val oktober = YearMonth.of(2025, 10)
    private val november = YearMonth.of(2025, 11)
    private val desember = YearMonth.of(2025, 12)

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 1 - Barn går fra ingen barnehageplass til deltids barnehageplass`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = august.atDay(15),
                    periodeTom = oktober.atDay(14),
                    antallTimer = null,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(15),
                    periodeTom = desember.atDay(1),
                    antallTimer = BigDecimal.valueOf(8),
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(2)

        assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(september.atDay(1))
        assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(september.atEndOfMonth())

        assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(oktober.atDay(1))
        assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(november.atEndOfMonth())
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 2 - Barn går fra deltids barnehageplass til ingen barnehageplass`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = august.atDay(15),
                    periodeTom = oktober.atDay(14),
                    antallTimer = BigDecimal.valueOf(8),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(15),
                    periodeTom = desember.atDay(1),
                    antallTimer = null,
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(2)

        assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(september.atDay(1))
        assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(oktober.atEndOfMonth())

        assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(november.atDay(1))
        assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(november.atEndOfMonth())
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 3,0 - Barnet går fra deltids barnehageplass til økt barnehageplass i månedsskifte`() {
        // Arrange
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = august.atDay(14),
                periodeTom = september.atEndOfMonth(),
                antallTimer = BigDecimal.valueOf(8),
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = oktober.atDay(1),
                periodeTom = desember.atDay(1),
                antallTimer = BigDecimal.valueOf(17),
            )

        val vilkårResultater = listOf(vilkårResultat1, vilkårResultat2)

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(2)

        assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(september.atDay(1))
        assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(september.atEndOfMonth())

        assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(oktober.atDay(1))
        assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(november.atEndOfMonth())
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 3,5 - Barnet går fra fulltids barnehageplass til deltids barnehageplass`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = august.atDay(14),
                    periodeTom = oktober.atDay(14),
                    antallTimer = BigDecimal.valueOf(33),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(15),
                    periodeTom = desember.atDay(1),
                    antallTimer = BigDecimal.valueOf(17),
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(2)

        assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(september.atDay(1))
        assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(oktober.atEndOfMonth())

        assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(november.atDay(1))
        assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(november.atEndOfMonth())
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Spesialhåndtering 1 - Barnet slutter i barnehage siste dag i september, skal ha full KS fra oktober`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = august.atDay(14),
                    periodeTom = september.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(17),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(1),
                    periodeTom = desember.atDay(1),
                    antallTimer = null,
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(2)

        assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(september.atDay(1))
        assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(september.atEndOfMonth())

        assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(oktober.atDay(1))
        assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(november.atEndOfMonth())
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    // TODO: Gå gjennom Spesialhåndtering og rundskriv
    @Test
    fun `Spesialhåndtering 2 - Barnet reduserer barnehageplass i slutten av september, skal ha mer KS fra oktober`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = august.atDay(14),
                    periodeTom = september.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(17),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(1),
                    periodeTom = desember.atDay(1),
                    antallTimer = BigDecimal.valueOf(8),
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(2)

        assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(september.atDay(1))
        assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(september.atEndOfMonth())

        assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(oktober.atDay(1))
        assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(november.atEndOfMonth())
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 5 - Barnet går fra deltids barnehageplass til full barnehageplass`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = august.atDay(14),
                    periodeTom = oktober.atDay(13),
                    antallTimer = BigDecimal.valueOf(8),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(14),
                    periodeTom = desember.atDay(1),
                    antallTimer = BigDecimal.valueOf(33),
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(2)

        assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(september.atDay(1))
        assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(september.atEndOfMonth())

        assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(oktober.atDay(1))
        assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(november.atEndOfMonth())
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 6 - Barnet går fra full barnehageplass til deltids barnehageplass`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = august.atDay(14),
                    periodeTom = oktober.atDay(13),
                    antallTimer = BigDecimal.valueOf(33),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(14),
                    periodeTom = desember.atDay(1),
                    antallTimer = BigDecimal.valueOf(8),
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(2)

        assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(september.atDay(1))
        assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(oktober.atEndOfMonth())

        assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(november.atDay(1))
        assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(november.atEndOfMonth())
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 7,1 - forskyvBarnehageplassVilkår skal støtte flere perioder i en måned`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = januar.atEndOfMonth(),
                    periodeTom = februar.atDay(12),
                    antallTimer = BigDecimal.valueOf(8),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = februar.atDay(13),
                    periodeTom = februar.atDay(23),
                    antallTimer = BigDecimal.valueOf(32),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = februar.atDay(24),
                    periodeTom = mars.atDay(1),
                    antallTimer = BigDecimal.valueOf(8),
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(1)

        assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(februar.atDay(1))
        assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(februar.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater.first().verdi.antallTimer).isEqualTo(BigDecimal.valueOf(32))
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 7,2 - forskyvBarnehageplassVilkår skal støtte flere perioder i en måned`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = januar.atEndOfMonth(),
                    periodeTom = februar.atDay(12),
                    antallTimer = BigDecimal.valueOf(32),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = februar.atDay(13),
                    periodeTom = februar.atDay(23),
                    antallTimer = BigDecimal.valueOf(8),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = februar.atDay(24),
                    periodeTom = april.atDay(1),
                    antallTimer = BigDecimal.valueOf(16),
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(2)

        assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(februar.atDay(1))
        assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(februar.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater.first().verdi.antallTimer).isEqualTo(BigDecimal.valueOf(32))

        assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(mars.atDay(1))
        assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(mars.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater.last().verdi.antallTimer).isEqualTo(BigDecimal.valueOf(16))
    }

    @Test
    fun `Scenario 1 fra rundskrivet - Endring i KS skjer fra og med samme måned som en økning i barnehageplass - test 1`() {
        // Arrange
        val vilkårResultaterForBarn =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = juli.atEndOfMonth(),
                    periodeTom = september.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(17),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(1),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(35),
                ),
            )

        // Act
        val forskjøvedeVilkårResultaterForBarn1 = forskyvBarnehageplassVilkår(vilkårResultaterForBarn)

        // Assert
        assertThat(forskjøvedeVilkårResultaterForBarn1).hasSize(2)

        assertThat(forskjøvedeVilkårResultaterForBarn1.first().fom).isEqualTo(august.atDay(1))
        assertThat(forskjøvedeVilkårResultaterForBarn1.first().tom).isEqualTo(september.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultaterForBarn1.first().verdi.antallTimer).isEqualTo(BigDecimal(17))

        assertThat(forskjøvedeVilkårResultaterForBarn1.last().fom).isEqualTo(oktober.atDay(1))
        assertThat(forskjøvedeVilkårResultaterForBarn1.last().tom).isEqualTo(desember.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultaterForBarn1.last().verdi.antallTimer).isEqualTo(BigDecimal(35))
    }

    @Test
    fun `Scenario 1 fra rundskrivet - Endring i KS skjer fra og med samme måned som en økning i barnehageplass - test 2`() {
        // Arrange
        val vilkårResultaterForBarn =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = juli.atEndOfMonth(),
                    periodeTom = oktober.atDay(15),
                    antallTimer = BigDecimal.valueOf(24),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(16),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(40),
                ),
            )

        // Act
        val forskjøvedeVilkårResultaterForBarn2 = forskyvBarnehageplassVilkår(vilkårResultaterForBarn)

        // Assert
        assertThat(forskjøvedeVilkårResultaterForBarn2).hasSize(2)

        assertThat(forskjøvedeVilkårResultaterForBarn2.first().fom).isEqualTo(august.atDay(1))
        assertThat(forskjøvedeVilkårResultaterForBarn2.first().tom).isEqualTo(september.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultaterForBarn2.first().verdi.antallTimer).isEqualTo(BigDecimal(24))

        assertThat(forskjøvedeVilkårResultaterForBarn2.last().fom).isEqualTo(oktober.atDay(1))
        assertThat(forskjøvedeVilkårResultaterForBarn2.last().tom).isEqualTo(desember.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultaterForBarn2.last().verdi.antallTimer).isEqualTo(BigDecimal(40))
    }

    @Test
    fun `Scenario 2 fra rundskrivet - Kontantstøtte ytes fra og med måneden etter at vilkårene er oppfylt`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = september.atDay(1),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(15),
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(1)
        assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(oktober.atDay(1))
        assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(desember.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater.first().verdi.antallTimer).isEqualTo(BigDecimal(15))
    }

    @Test
    fun `Scenario 3 fra rundskrivet - Kontantstøtte opphører fra og med måneden retten til støtte opphører`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = juli.atDay(1),
                    periodeTom = september.atEndOfMonth(),
                    antallTimer = null,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(1),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = BigDecimal(8),
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(2)

        assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(august.atDay(1))
        assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(september.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater.first().verdi.antallTimer).isNull()

        assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(oktober.atDay(1))
        assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(desember.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater.last().verdi.antallTimer).isEqualTo(BigDecimal(8))
    }

    @Test
    fun `Scenario 4 fra rundskrivet - Kontantstøtte ytes fra og med måneden etter at barnet sluttet i barnehagen`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = juli.atDay(1),
                    periodeTom = september.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(25),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(1),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = null,
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(2)

        assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(august.atDay(1))
        assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(september.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater.first().verdi.antallTimer).isEqualTo(BigDecimal.valueOf(25))

        assertThat(forskjøvedeVilkårResultater.last().fom).isEqualTo(oktober.atDay(1))
        assertThat(forskjøvedeVilkårResultater.last().tom).isEqualTo(desember.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater.last().verdi.antallTimer).isNull()
    }

    @Test
    fun `Scenario 5 fra rundskrivet - Reduksjon av barnehageplass skal forskyves riktig`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = januar.atDay(1),
                    periodeTom = august.atEndOfMonth(),
                    antallTimer = null,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = september.atDay(1),
                    periodeTom = september.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(33),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(1),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(15),
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(3)

        assertThat(forskjøvedeVilkårResultater[0].fom).isEqualTo(februar.atDay(1))
        assertThat(forskjøvedeVilkårResultater[0].tom).isEqualTo(august.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater[0].verdi.antallTimer).isNull()

        assertThat(forskjøvedeVilkårResultater[1].fom).isEqualTo(september.atDay(1))
        assertThat(forskjøvedeVilkårResultater[1].tom).isEqualTo((september.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultater[1].verdi.antallTimer).isEqualTo(BigDecimal.valueOf(33))

        assertThat(forskjøvedeVilkårResultater[2].fom).isEqualTo(november.atDay(1))
        assertThat(forskjøvedeVilkårResultater[2].tom).isEqualTo(desember.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater[2].verdi.antallTimer).isEqualTo(BigDecimal.valueOf(15))
    }

    @Test
    fun `Scenario 6 fra rundskrivet - Slutt i barnehage skal forskyves riktig`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = januar.atDay(1),
                    periodeTom = august.atEndOfMonth(),
                    antallTimer = null,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = september.atDay(1),
                    periodeTom = september.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(33),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(1),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = null,
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(3)

        assertThat(forskjøvedeVilkårResultater[0].fom).isEqualTo(februar.atDay(1))
        assertThat(forskjøvedeVilkårResultater[0].tom).isEqualTo(august.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater[0].verdi.antallTimer).isNull()

        assertThat(forskjøvedeVilkårResultater[1].fom).isEqualTo(september.atDay(1))
        assertThat(forskjøvedeVilkårResultater[1].tom).isEqualTo(september.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater[1].verdi.antallTimer).isEqualTo(BigDecimal.valueOf(33))

        assertThat(forskjøvedeVilkårResultater[2].fom).isEqualTo(oktober.atDay(1))
        assertThat(forskjøvedeVilkårResultater[2].tom).isEqualTo(desember.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater[2].verdi.antallTimer).isNull()
    }

    @Test
    fun `Scenario 7 fra rundskrivet - Økt barnehageplass skal forskyves riktig`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = januar.atDay(1),
                    periodeTom = august.atEndOfMonth(),
                    antallTimer = null,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = september.atDay(1),
                    periodeTom = september.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(8),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(1),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(15),
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(3)

        assertThat(forskjøvedeVilkårResultater[0].fom).isEqualTo(februar.atDay(1))
        assertThat(forskjøvedeVilkårResultater[0].tom).isEqualTo(august.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater[0].verdi.antallTimer).isNull()

        assertThat(forskjøvedeVilkårResultater[1].fom).isEqualTo(september.atDay(1))
        assertThat(forskjøvedeVilkårResultater[1].tom).isEqualTo(september.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater[1].verdi.antallTimer).isEqualTo(BigDecimal.valueOf(8))

        assertThat(forskjøvedeVilkårResultater[2].fom).isEqualTo(oktober.atDay(1))
        assertThat(forskjøvedeVilkårResultater[2].tom).isEqualTo(desember.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater[2].verdi.antallTimer).isEqualTo(BigDecimal.valueOf(15))
    }

    @Test
    fun `forskyvBarnehageplassVilkår skal ikke forskyves ved overgang til periode med 33 timer eller mer`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = august.atDay(14),
                    periodeTom = oktober.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(8),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = november.atDay(1),
                    periodeTom = desember.atDay(1),
                    antallTimer = BigDecimal.valueOf(33),
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(2)

        assertThat(forskjøvedeVilkårResultater.first().fom).isEqualTo(september.atDay(1))
        assertThat(forskjøvedeVilkårResultater.first().tom).isEqualTo(oktober.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater.first().verdi.antallTimer).isEqualTo(BigDecimal.valueOf(8))

        assertThat(forskjøvedeVilkårResultater[1].fom).isEqualTo(november.atDay(1))
        assertThat(forskjøvedeVilkårResultater[1].tom).isEqualTo(november.atEndOfMonth())
        assertThat(forskjøvedeVilkårResultater[1].verdi.antallTimer).isEqualTo(BigDecimal.valueOf(33))
    }

    @Test
    fun `Skal forskyve riktig ved opphold av barnehageplass`() {
        // Arrange
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = LocalDate.of(2025, 1, 14),
                    periodeTom = LocalDate.of(2025, 2, 13),
                    antallTimer = BigDecimal.valueOf(8),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = LocalDate.of(2025, 2, 15),
                    periodeTom = LocalDate.of(2025, 4, 14),
                    antallTimer = BigDecimal.valueOf(16),
                ),
            )

        // Act
        val forskjøvedeVilkårResultater = forskyvBarnehageplassVilkår(vilkårResultater)

        // Assert
        assertThat(forskjøvedeVilkårResultater).hasSize(1)

        assertThat(forskjøvedeVilkårResultater.single().fom).isEqualTo(LocalDate.of(2025, 3, 1))
        assertThat(forskjøvedeVilkårResultater.single().tom).isEqualTo(LocalDate.of(2025, 3, 31))
    }
}
