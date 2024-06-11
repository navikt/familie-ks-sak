package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårRegelsett
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

class ForskyvBarnehageplassVilkårTest {
    private val januar = YearMonth.of(2022, 1)
    private val februar = YearMonth.of(2022, 2)
    private val mars = YearMonth.of(2022, 3)
    private val april = YearMonth.of(2022, 4)
    private val juli = YearMonth.of(2022, 7)
    private val august = YearMonth.of(2022, 8)
    private val september = YearMonth.of(2022, 9)
    private val oktober = YearMonth.of(2022, 10)
    private val november = YearMonth.of(2022, 11)
    private val desember = YearMonth.of(2022, 12)

    // Eksempel i src/test/resources/barnehageplassscenarioer
    @Test
    fun `Scenario 1 - Barn går fra ingen barnehageplass til deltids barnehageplass`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = august.atDay(15),
                periodeTom = oktober.atDay(14),
                antallTimer = null,
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = oktober.atDay(15),
                periodeTom = desember.atDay(1),
                antallTimer = BigDecimal.valueOf(8),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
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
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = august.atDay(15),
                periodeTom = oktober.atDay(14),
                antallTimer = BigDecimal.valueOf(8),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = oktober.atDay(15),
                periodeTom = desember.atDay(1),
                antallTimer = null,
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
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
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = august.atDay(14),
                periodeTom = september.atEndOfMonth(),
                antallTimer = BigDecimal.valueOf(8),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = oktober.atDay(1),
                periodeTom = desember.atDay(1),
                antallTimer = BigDecimal.valueOf(17),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
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
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = august.atDay(14),
                periodeTom = oktober.atDay(14),
                antallTimer = BigDecimal.valueOf(33),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = oktober.atDay(15),
                periodeTom = desember.atDay(1),
                antallTimer = BigDecimal.valueOf(17),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
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
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = august.atDay(14),
                periodeTom = september.atEndOfMonth(),
                antallTimer = BigDecimal.valueOf(17),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = oktober.atDay(1),
                periodeTom = desember.atDay(1),
                antallTimer = null,
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
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
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = august.atDay(14),
                periodeTom = september.atEndOfMonth(),
                antallTimer = BigDecimal.valueOf(17),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = oktober.atDay(1),
                periodeTom = desember.atDay(1),
                antallTimer = BigDecimal.valueOf(8),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
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
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = august.atDay(14),
                periodeTom = oktober.atDay(13),
                antallTimer = BigDecimal.valueOf(8),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = oktober.atDay(14),
                periodeTom = desember.atDay(1),
                antallTimer = BigDecimal.valueOf(33),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
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
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = august.atDay(14),
                periodeTom = oktober.atDay(13),
                antallTimer = BigDecimal.valueOf(33),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = oktober.atDay(14),
                periodeTom = desember.atDay(1),
                antallTimer = BigDecimal.valueOf(8),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
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
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = januar.atEndOfMonth(),
                    periodeTom = februar.atDay(12),
                    antallTimer = BigDecimal.valueOf(8),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = februar.atDay(13),
                    periodeTom = februar.atDay(23),
                    antallTimer = BigDecimal.valueOf(32),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = februar.atDay(24),
                    periodeTom = mars.atDay(1),
                    antallTimer = BigDecimal.valueOf(8),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
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
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = januar.atEndOfMonth(),
                    periodeTom = februar.atDay(12),
                    antallTimer = BigDecimal.valueOf(32),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = februar.atDay(13),
                    periodeTom = februar.atDay(23),
                    antallTimer = BigDecimal.valueOf(8),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = februar.atDay(24),
                    periodeTom = april.atDay(1),
                    antallTimer = BigDecimal.valueOf(16),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
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
    fun `Scenario 1 fra rundskrivet - Endring i KS skjer fra og med samme måned som en økning i barnehageplass`() {
        val vilkårResultaterForBarn1 =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = juli.atEndOfMonth(),
                    periodeTom = september.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(17),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(1),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(35),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
            )

        val vilkårResultaterForBarn2 =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = juli.atEndOfMonth(),
                    periodeTom = oktober.atDay(15),
                    antallTimer = BigDecimal.valueOf(24),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(16),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(40),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
            )

        val forskjøvedeVilkårResultaterForBarn1 = vilkårResultaterForBarn1.forskyvBarnehageplassVilkår()

        assertThat(forskjøvedeVilkårResultaterForBarn1.size, Is(2))
        assertThat(forskjøvedeVilkårResultaterForBarn1.first().fom, Is(august.atDay(1)))
        assertThat(forskjøvedeVilkårResultaterForBarn1.first().tom, Is(september.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultaterForBarn1.first().verdi.antallTimer, Is(BigDecimal(17)))

        assertThat(forskjøvedeVilkårResultaterForBarn1.last().fom, Is(oktober.atDay(1)))
        assertThat(forskjøvedeVilkårResultaterForBarn1.last().tom, Is(desember.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultaterForBarn1.last().verdi.antallTimer, Is(BigDecimal(35)))

        val forskjøvedeVilkårResultaterForBarn2 = vilkårResultaterForBarn2.forskyvBarnehageplassVilkår()

        assertThat(forskjøvedeVilkårResultaterForBarn2.size, Is(2))
        assertThat(forskjøvedeVilkårResultaterForBarn2.first().fom, Is(august.atDay(1)))
        assertThat(forskjøvedeVilkårResultaterForBarn2.first().tom, Is(september.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultaterForBarn2.first().verdi.antallTimer, Is(BigDecimal(24)))

        assertThat(forskjøvedeVilkårResultaterForBarn2.last().fom, Is(oktober.atDay(1)))
        assertThat(forskjøvedeVilkårResultaterForBarn2.last().tom, Is(desember.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultaterForBarn2.last().verdi.antallTimer, Is(BigDecimal(40)))
    }

    @Test
    fun `Scenario 2 fra rundskrivet - Kontantstøtte ytes fra og med måneden etter at vilkårene er oppfylt`() {
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = september.atDay(1),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(15),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
            )

        val forskjøvedeVilkårResultater = vilkårResultater.forskyvBarnehageplassVilkår()

        assertThat(forskjøvedeVilkårResultater.size, Is(1))
        assertThat(forskjøvedeVilkårResultater.first().fom, Is(oktober.atDay(1)))
        assertThat(forskjøvedeVilkårResultater.first().tom, Is(desember.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultater.first().verdi.antallTimer, Is(BigDecimal(15)))
    }

    @Test
    fun `Scenario 3 fra rundskrivet - Kontantstøtte opphører fra og med måneden retten til støtte opphører`() {
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = juli.atDay(1),
                    periodeTom = september.atEndOfMonth(),
                    antallTimer = null,
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(1),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = BigDecimal(8),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
            )

        val forskjøvedeVilkårResultater = vilkårResultater.forskyvBarnehageplassVilkår()

        assertThat(forskjøvedeVilkårResultater.size, Is(2))
        assertThat(forskjøvedeVilkårResultater.first().fom, Is(august.atDay(1)))
        assertThat(forskjøvedeVilkårResultater.first().tom, Is(september.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultater.first().verdi.antallTimer, Is(nullValue()))

        assertThat(forskjøvedeVilkårResultater.last().fom, Is(oktober.atDay(1)))
        assertThat(forskjøvedeVilkårResultater.last().tom, Is(desember.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultater.last().verdi.antallTimer, Is(BigDecimal(8)))
    }

    @Test
    fun `Scenario 4 fra rundskrivet - Kontantstøtte ytes fra og med måneden etter at barnet sluttet i barnehagen`() {
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = juli.atDay(1),
                    periodeTom = september.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(25),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(1),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = null,
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
            )

        val forskjøvedeVilkårResultater = vilkårResultater.forskyvBarnehageplassVilkår()

        assertThat(forskjøvedeVilkårResultater.size, Is(2))
        assertThat(forskjøvedeVilkårResultater.first().fom, Is(august.atDay(1)))
        assertThat(forskjøvedeVilkårResultater.first().tom, Is(september.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultater.first().verdi.antallTimer, Is(BigDecimal.valueOf(25)))

        assertThat(forskjøvedeVilkårResultater.last().fom, Is(oktober.atDay(1)))
        assertThat(forskjøvedeVilkårResultater.last().tom, Is(desember.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultater.last().verdi.antallTimer, Is(nullValue()))
    }

    @Test
    fun `Scenario 5 fra rundskrivet - Reduksjon av barnehageplass skal forskyves riktig`() {
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = januar.atDay(1),
                    periodeTom = august.atEndOfMonth(),
                    antallTimer = null,
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = september.atDay(1),
                    periodeTom = september.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(33),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(1),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(15),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
            )

        val forskjøvedeVilkårResultater = vilkårResultater.forskyvBarnehageplassVilkår()

        assertThat(forskjøvedeVilkårResultater.size, Is(3))
        assertThat(forskjøvedeVilkårResultater[0].fom, Is(februar.atDay(1)))
        assertThat(forskjøvedeVilkårResultater[0].tom, Is(august.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultater[0].verdi.antallTimer, Is(nullValue()))

        assertThat(forskjøvedeVilkårResultater[1].fom, Is(september.atDay(1)))
        assertThat(forskjøvedeVilkårResultater[1].tom, Is(september.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultater[1].verdi.antallTimer, Is(BigDecimal.valueOf(33)))

        assertThat(forskjøvedeVilkårResultater[2].fom, Is(november.atDay(1)))
        assertThat(forskjøvedeVilkårResultater[2].tom, Is(desember.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultater[2].verdi.antallTimer, Is(BigDecimal.valueOf(15)))
    }

    @Test
    fun `Scenario 6 fra rundskrivet - Slutt i barnehage skal forskyves riktig`() {
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = januar.atDay(1),
                    periodeTom = august.atEndOfMonth(),
                    antallTimer = null,
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = september.atDay(1),
                    periodeTom = september.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(33),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(1),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = null,
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
            )

        val forskjøvedeVilkårResultater = vilkårResultater.forskyvBarnehageplassVilkår()

        assertThat(forskjøvedeVilkårResultater.size, Is(3))
        assertThat(forskjøvedeVilkårResultater[0].fom, Is(februar.atDay(1)))
        assertThat(forskjøvedeVilkårResultater[0].tom, Is(august.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultater[0].verdi.antallTimer, Is(nullValue()))

        assertThat(forskjøvedeVilkårResultater[1].fom, Is(september.atDay(1)))
        assertThat(forskjøvedeVilkårResultater[1].tom, Is(september.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultater[1].verdi.antallTimer, Is(BigDecimal.valueOf(33)))

        assertThat(forskjøvedeVilkårResultater[2].fom, Is(oktober.atDay(1)))
        assertThat(forskjøvedeVilkårResultater[2].tom, Is(desember.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultater[2].verdi.antallTimer, Is(nullValue()))
    }

    @Test
    fun `Scenario 7 fra rundskrivet - Økt barnehageplass skal forskyves riktig`() {
        val vilkårResultater =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = januar.atDay(1),
                    periodeTom = august.atEndOfMonth(),
                    antallTimer = null,
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = september.atDay(1),
                    periodeTom = september.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(8),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = oktober.atDay(1),
                    periodeTom = desember.atEndOfMonth(),
                    antallTimer = BigDecimal.valueOf(15),
                    regelsett = VilkårRegelsett.LOV_AUGUST_2021,
                ),
            )

        val forskjøvedeVilkårResultater = vilkårResultater.forskyvBarnehageplassVilkår()

        assertThat(forskjøvedeVilkårResultater.size, Is(3))
        assertThat(forskjøvedeVilkårResultater[0].fom, Is(februar.atDay(1)))
        assertThat(forskjøvedeVilkårResultater[0].tom, Is(august.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultater[0].verdi.antallTimer, Is(nullValue()))

        assertThat(forskjøvedeVilkårResultater[1].fom, Is(september.atDay(1)))
        assertThat(forskjøvedeVilkårResultater[1].tom, Is(september.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultater[1].verdi.antallTimer, Is(BigDecimal.valueOf(8)))

        assertThat(forskjøvedeVilkårResultater[2].fom, Is(oktober.atDay(1)))
        assertThat(forskjøvedeVilkårResultater[2].tom, Is(desember.atEndOfMonth()))
        assertThat(forskjøvedeVilkårResultater[2].verdi.antallTimer, Is(BigDecimal.valueOf(15)))
    }

    @Test
    fun `forskyvBarnehageplassVilkår skal ikke forskyves ved overgang til periode med 33 timer eller mer`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = august.atDay(14),
                periodeTom = oktober.atEndOfMonth(),
                antallTimer = BigDecimal.valueOf(8),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = november.atDay(1),
                periodeTom = desember.atDay(1),
                antallTimer = BigDecimal.valueOf(33),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )

        val forskjøvedeVilkårResultater = listOf(vilkårResultat1, vilkårResultat2).forskyvBarnehageplassVilkår()

        Assertions.assertEquals(2, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(september.atDay(1), forskjøvedeVilkårResultater.first().fom)
        Assertions.assertEquals(oktober.atEndOfMonth(), forskjøvedeVilkårResultater.first().tom)
        Assertions.assertEquals(BigDecimal.valueOf(8), forskjøvedeVilkårResultater.first().verdi.antallTimer)

        Assertions.assertEquals(november.atDay(1), forskjøvedeVilkårResultater[1].fom)
        Assertions.assertEquals(november.atEndOfMonth(), forskjøvedeVilkårResultater[1].tom)
        Assertions.assertEquals(BigDecimal.valueOf(33), forskjøvedeVilkårResultater[1].verdi.antallTimer)
    }

    @Test
    fun `Skal forskyve riktig ved opphold av barnehageplass`() {
        val vilkårResultat1 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = LocalDate.of(2022, 1, 14),
                periodeTom = LocalDate.of(2022, 2, 13),
                antallTimer = BigDecimal.valueOf(8),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )
        val vilkårResultat2 =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = LocalDate.of(2022, 2, 15),
                periodeTom = LocalDate.of(2022, 4, 14),
                antallTimer = BigDecimal.valueOf(16),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
            )

        val forskjøvedeVilkårResultater = listOf(vilkårResultat1, vilkårResultat2).forskyvBarnehageplassVilkår()

        Assertions.assertEquals(1, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(LocalDate.of(2022, 3, 1), forskjøvedeVilkårResultater.single().fom)
        Assertions.assertEquals(LocalDate.of(2022, 3, 31), forskjøvedeVilkårResultater.single().tom)
    }
}
