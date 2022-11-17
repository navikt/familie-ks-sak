package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class forskyvBarnehageplassVilkårTest {

    private val januar = YearMonth.of(2022, 1)
    private val februar = YearMonth.of(2022, 2)
    private val mars = YearMonth.of(2022, 3)
    private val april = YearMonth.of(2022, 4)
    private val august = YearMonth.of(2022, 8)
    private val september = YearMonth.of(2022, 9)
    private val oktober = YearMonth.of(2022, 10)
    private val november = YearMonth.of(2022, 11)
    private val desember = YearMonth.of(2022, 12)

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 1 - Barn går fra ingen barnehageplass til deltids barnehageplass`() {
        val vilkårResultat1 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = august.atDay(15),
            periodeTom = oktober.atDay(14),
            antallTimer = null
        )
        val vilkårResultat2 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = oktober.atDay(15),
            periodeTom = desember.atDay(1),
            antallTimer = BigDecimal.valueOf(8)
        )

        val forskjøvedeVilkårResultater = listOf(vilkårResultat1, vilkårResultat2).forskyvBarnehageplassVilkår()

        Assertions.assertEquals(2, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(september.atDay(1), forskjøvedeVilkårResultater.first().fom)
        Assertions.assertEquals(september.atEndOfMonth(), forskjøvedeVilkårResultater.first().tom)

        Assertions.assertEquals(oktober.atDay(1), forskjøvedeVilkårResultater.last().fom)
        Assertions.assertEquals(november.atEndOfMonth(), forskjøvedeVilkårResultater.last().tom)
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 2 - Barn går fra deltids barnehageplass til ingen barnehageplass`() {
        val vilkårResultat1 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = august.atDay(15),
            periodeTom = oktober.atDay(14),
            antallTimer = BigDecimal.valueOf(8)
        )
        val vilkårResultat2 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = oktober.atDay(15),
            periodeTom = desember.atDay(1),
            antallTimer = null
        )

        val forskjøvedeVilkårResultater = listOf(vilkårResultat1, vilkårResultat2).forskyvBarnehageplassVilkår()

        Assertions.assertEquals(2, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(september.atDay(1), forskjøvedeVilkårResultater.first().fom)
        Assertions.assertEquals(oktober.atEndOfMonth(), forskjøvedeVilkårResultater.first().tom)

        Assertions.assertEquals(november.atDay(1), forskjøvedeVilkårResultater.last().fom)
        Assertions.assertEquals(november.atEndOfMonth(), forskjøvedeVilkårResultater.last().tom)
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 3,0 - Barnet går fra deltids barnehageplass til økt barnehageplass i månedsskifte`() {
        val vilkårResultat1 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = august.atDay(14),
            periodeTom = september.atEndOfMonth(),
            antallTimer = BigDecimal.valueOf(8)
        )
        val vilkårResultat2 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = oktober.atDay(1),
            periodeTom = desember.atDay(1),
            antallTimer = BigDecimal.valueOf(17)
        )

        val forskjøvedeVilkårResultater = listOf(vilkårResultat1, vilkårResultat2).forskyvBarnehageplassVilkår()

        Assertions.assertEquals(2, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(september.atDay(1), forskjøvedeVilkårResultater.first().fom)
        Assertions.assertEquals(september.atEndOfMonth(), forskjøvedeVilkårResultater.first().tom)

        Assertions.assertEquals(oktober.atDay(1), forskjøvedeVilkårResultater.last().fom)
        Assertions.assertEquals(november.atEndOfMonth(), forskjøvedeVilkårResultater.last().tom)
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 3,5 - Barnet går fra fulltids barnehageplass til deltids barnehageplass`() {
        val vilkårResultat1 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = august.atDay(14),
            periodeTom = oktober.atDay(14),
            antallTimer = BigDecimal.valueOf(33)
        )
        val vilkårResultat2 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = oktober.atDay(15),
            periodeTom = desember.atDay(1),
            antallTimer = BigDecimal.valueOf(17)
        )

        val forskjøvedeVilkårResultater = listOf(vilkårResultat1, vilkårResultat2).forskyvBarnehageplassVilkår()

        Assertions.assertEquals(2, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(september.atDay(1), forskjøvedeVilkårResultater.first().fom)
        Assertions.assertEquals(oktober.atEndOfMonth(), forskjøvedeVilkårResultater.first().tom)

        Assertions.assertEquals(november.atDay(1), forskjøvedeVilkårResultater.last().fom)
        Assertions.assertEquals(november.atEndOfMonth(), forskjøvedeVilkårResultater.last().tom)
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Spesialhåndtering 1 - Barnet slutter i barnehage siste dag i september, skal ha full KS fra oktober`() {
        val vilkårResultat1 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = august.atDay(14),
            periodeTom = september.atEndOfMonth(),
            antallTimer = BigDecimal.valueOf(17)
        )
        val vilkårResultat2 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = oktober.atDay(1),
            periodeTom = desember.atDay(1),
            antallTimer = null
        )

        val forskjøvedeVilkårResultater = listOf(vilkårResultat1, vilkårResultat2).forskyvBarnehageplassVilkår()

        Assertions.assertEquals(2, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(september.atDay(1), forskjøvedeVilkårResultater.first().fom)
        Assertions.assertEquals(september.atEndOfMonth(), forskjøvedeVilkårResultater.first().tom)

        Assertions.assertEquals(oktober.atDay(1), forskjøvedeVilkårResultater.last().fom)
        Assertions.assertEquals(november.atEndOfMonth(), forskjøvedeVilkårResultater.last().tom)
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Spesialhåndtering 2 - Barnet reduserer barnehageplass i slutten av september, skal ha mer KS fra oktober`() {
        val vilkårResultat1 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = august.atDay(14),
            periodeTom = september.atEndOfMonth(),
            antallTimer = BigDecimal.valueOf(17)
        )
        val vilkårResultat2 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = oktober.atDay(1),
            periodeTom = desember.atDay(1),
            antallTimer = BigDecimal.valueOf(8)
        )

        val forskjøvedeVilkårResultater = listOf(vilkårResultat1, vilkårResultat2).forskyvBarnehageplassVilkår()

        Assertions.assertEquals(2, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(september.atDay(1), forskjøvedeVilkårResultater.first().fom)
        Assertions.assertEquals(september.atEndOfMonth(), forskjøvedeVilkårResultater.first().tom)

        Assertions.assertEquals(oktober.atDay(1), forskjøvedeVilkårResultater.last().fom)
        Assertions.assertEquals(november.atEndOfMonth(), forskjøvedeVilkårResultater.last().tom)
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 5 - Barnet går fra deltids barnehageplass til full barnehageplass`() {
        val vilkårResultat1 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = august.atDay(14),
            periodeTom = oktober.atDay(13),
            antallTimer = BigDecimal.valueOf(8)
        )
        val vilkårResultat2 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = oktober.atDay(14),
            periodeTom = desember.atDay(1),
            antallTimer = BigDecimal.valueOf(33)
        )

        val forskjøvedeVilkårResultater = listOf(vilkårResultat1, vilkårResultat2).forskyvBarnehageplassVilkår()

        Assertions.assertEquals(2, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(september.atDay(1), forskjøvedeVilkårResultater.first().fom)
        Assertions.assertEquals(september.atEndOfMonth(), forskjøvedeVilkårResultater.first().tom)

        Assertions.assertEquals(oktober.atDay(1), forskjøvedeVilkårResultater.last().fom)
        Assertions.assertEquals(november.atEndOfMonth(), forskjøvedeVilkårResultater.last().tom)
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 6 - Barnet går fra full barnehageplass til deltids barnehageplass`() {
        val vilkårResultat1 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = august.atDay(14),
            periodeTom = oktober.atDay(13),
            antallTimer = BigDecimal.valueOf(33)
        )
        val vilkårResultat2 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = oktober.atDay(14),
            periodeTom = desember.atDay(1),
            antallTimer = BigDecimal.valueOf(8)
        )

        val forskjøvedeVilkårResultater = listOf(vilkårResultat1, vilkårResultat2).forskyvBarnehageplassVilkår()

        Assertions.assertEquals(2, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(september.atDay(1), forskjøvedeVilkårResultater.first().fom)
        Assertions.assertEquals(oktober.atEndOfMonth(), forskjøvedeVilkårResultater.first().tom)

        Assertions.assertEquals(november.atDay(1), forskjøvedeVilkårResultater.last().fom)
        Assertions.assertEquals(november.atEndOfMonth(), forskjøvedeVilkårResultater.last().tom)
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 7,1 - forskyvBarnehageplassVilkår skal støtte flere perioder i en måned`() {
        val vilkårResultater = listOf(
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = januar.atEndOfMonth(),
                periodeTom = februar.atDay(12),
                antallTimer = BigDecimal.valueOf(8)
            ),
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = februar.atDay(13),
                periodeTom = februar.atDay(23),
                antallTimer = BigDecimal.valueOf(32)
            ),
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = februar.atDay(24),
                periodeTom = mars.atDay(1),
                antallTimer = BigDecimal.valueOf(8)
            )
        )

        val forskjøvedeVilkårResultater = vilkårResultater.forskyvBarnehageplassVilkår()

        Assertions.assertEquals(1, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(februar.atDay(1), forskjøvedeVilkårResultater.first().fom)
        Assertions.assertEquals(februar.atEndOfMonth(), forskjøvedeVilkårResultater.first().tom)
        Assertions.assertEquals(BigDecimal.valueOf(32), forskjøvedeVilkårResultater.first().verdi.antallTimer)
    }

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 7,2 - forskyvBarnehageplassVilkår skal støtte flere perioder i en måned`() {
        val vilkårResultater = listOf(
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = januar.atEndOfMonth(),
                periodeTom = februar.atDay(12),
                antallTimer = BigDecimal.valueOf(32)
            ),
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = februar.atDay(13),
                periodeTom = februar.atDay(23),
                antallTimer = BigDecimal.valueOf(8)
            ),
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = februar.atDay(24),
                periodeTom = april.atDay(1),
                antallTimer = BigDecimal.valueOf(16)
            )
        )

        val forskjøvedeVilkårResultater = vilkårResultater.forskyvBarnehageplassVilkår()

        Assertions.assertEquals(2, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(februar.atDay(1), forskjøvedeVilkårResultater.first().fom)
        Assertions.assertEquals(februar.atEndOfMonth(), forskjøvedeVilkårResultater.first().tom)
        Assertions.assertEquals(BigDecimal.valueOf(32), forskjøvedeVilkårResultater.first().verdi.antallTimer)

        Assertions.assertEquals(mars.atDay(1), forskjøvedeVilkårResultater.last().fom)
        Assertions.assertEquals(mars.atEndOfMonth(), forskjøvedeVilkårResultater.last().tom)
        Assertions.assertEquals(BigDecimal.valueOf(16), forskjøvedeVilkårResultater.last().verdi.antallTimer)
    }

    @Test
    fun `Skal forskyve riktig ved opphold av barnehageplass`() {
        val vilkårResultat1 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = LocalDate.of(2022, 1, 14),
            periodeTom = LocalDate.of(2022, 2, 13),
            antallTimer = BigDecimal.valueOf(8)
        )
        val vilkårResultat2 = lagVilkårResultat(
            vilkårType = Vilkår.BARNEHAGEPLASS,
            periodeFom = LocalDate.of(2022, 2, 15),
            periodeTom = LocalDate.of(2022, 4, 14),
            antallTimer = BigDecimal.valueOf(16)
        )

        val forskjøvedeVilkårResultater = listOf(vilkårResultat1, vilkårResultat2).forskyvBarnehageplassVilkår()

        Assertions.assertEquals(1, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(LocalDate.of(2022, 3, 1), forskjøvedeVilkårResultater.single().fom)
        Assertions.assertEquals(LocalDate.of(2022, 3, 31), forskjøvedeVilkårResultater.single().tom)
    }
}
