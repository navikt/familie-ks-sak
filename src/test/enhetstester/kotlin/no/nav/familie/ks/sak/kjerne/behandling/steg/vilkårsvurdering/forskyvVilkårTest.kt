import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvVilkårResultater
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class forskyvVilkårTest {

    val august = YearMonth.of(2022, 8)
    val september = YearMonth.of(2022, 9)
    val oktober = YearMonth.of(2022, 10)
    val november = YearMonth.of(2022, 11)
    val desember = YearMonth.of(2022, 12)

    @Test
    fun `forskyvVilkårResultater skal ikke lage opphold i vilkår som ligger back to back`() {
        val vilkårResultat1 = lagVilkårResultat(
            vilkårType = Vilkår.BARNETS_ALDER,
            periodeFom = august.atDay(15),
            periodeTom = oktober.atDay(14)
        )
        val vilkårResultat2 = lagVilkårResultat(
            vilkårType = Vilkår.BARNETS_ALDER,
            periodeFom = oktober.atDay(15),
            periodeTom = desember.atDay(1)
        )

        val forskjøvedeVilkårResultater =
            forskyvVilkårResultater(Vilkår.BARNETS_ALDER, listOf(vilkårResultat1, vilkårResultat2))

        Assertions.assertEquals(2, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(september.atDay(1), forskjøvedeVilkårResultater.first().fom)
        Assertions.assertEquals(oktober.atEndOfMonth(), forskjøvedeVilkårResultater.first().tom)

        Assertions.assertEquals(november.atDay(1), forskjøvedeVilkårResultater.last().fom)
        Assertions.assertEquals(november.atEndOfMonth(), forskjøvedeVilkårResultater.last().tom)
    }

    @Test
    fun `forskyvVilkårResultater skal lage opphold i vilkårene ved perioder som ikke er back to back`() {
        val vilkårResultat1 = lagVilkårResultat(
            vilkårType = Vilkår.BARNETS_ALDER,
            periodeFom = august.atDay(15),
            periodeTom = oktober.atDay(13)
        )
        val vilkårResultat2 = lagVilkårResultat(
            vilkårType = Vilkår.BARNETS_ALDER,
            periodeFom = oktober.atDay(15),
            periodeTom = desember.atDay(1)
        )

        val forskjøvedeVilkårResultater =
            forskyvVilkårResultater(Vilkår.BARNETS_ALDER, listOf(vilkårResultat1, vilkårResultat2))

        Assertions.assertEquals(2, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(september.atDay(1), forskjøvedeVilkårResultater.first().fom)
        Assertions.assertEquals(september.atEndOfMonth(), forskjøvedeVilkårResultater.first().tom)

        Assertions.assertEquals(november.atDay(1), forskjøvedeVilkårResultater.last().fom)
        Assertions.assertEquals(november.atEndOfMonth(), forskjøvedeVilkårResultater.last().tom)
    }

    @Test
    fun `forskyvVilkårResultater skal ikke lage opphold i vilkår som ligger back to back i månedsskifte`() {
        val vilkårResultat1 = lagVilkårResultat(
            vilkårType = Vilkår.BARNETS_ALDER,
            periodeFom = august.atDay(15),
            periodeTom = august.atEndOfMonth()
        )
        val vilkårResultat2 = lagVilkårResultat(
            vilkårType = Vilkår.BARNETS_ALDER,
            periodeFom = september.atDay(1),
            periodeTom = desember.atDay(1)
        )

        val forskjøvedeVilkårResultater =
            forskyvVilkårResultater(Vilkår.BARNETS_ALDER, listOf(vilkårResultat1, vilkårResultat2))

        Assertions.assertEquals(2, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(september.atDay(1), forskjøvedeVilkårResultater.first().fom)
        Assertions.assertEquals(september.atEndOfMonth(), forskjøvedeVilkårResultater.first().tom)

        Assertions.assertEquals(oktober.atDay(1), forskjøvedeVilkårResultater.last().fom)
        Assertions.assertEquals(november.atEndOfMonth(), forskjøvedeVilkårResultater.last().tom)
    }

    @Test
    fun `forskyvVilkårResultater skal lage opphold i vilkår som ikke ligger back to back i månedsskifte`() {
        val vilkårResultat1 = lagVilkårResultat(
            vilkårType = Vilkår.BARNETS_ALDER,
            periodeFom = august.atDay(15),
            periodeTom = september.atEndOfMonth()
        )
        val vilkårResultat2 = lagVilkårResultat(
            vilkårType = Vilkår.BARNETS_ALDER,
            periodeFom = oktober.atDay(2),
            periodeTom = desember.atDay(1)
        )

        val forskjøvedeVilkårResultater =
            forskyvVilkårResultater(Vilkår.BARNETS_ALDER, listOf(vilkårResultat1, vilkårResultat2))

        Assertions.assertEquals(2, forskjøvedeVilkårResultater.size)

        Assertions.assertEquals(september.atDay(1), forskjøvedeVilkårResultater.first().fom)
        Assertions.assertEquals(september.atEndOfMonth(), forskjøvedeVilkårResultater.first().tom)

        Assertions.assertEquals(november.atDay(1), forskjøvedeVilkårResultater.last().fom)
        Assertions.assertEquals(november.atEndOfMonth(), forskjøvedeVilkårResultater.last().tom)
    }

    @Test
    fun `forskyvVilkårResultater skal filtrere bort peroder som ikke gjelder for noen måneder`() {
        val vilkårResultat1 = lagVilkårResultat(
            vilkårType = Vilkår.BARNETS_ALDER,
            periodeFom = august.atDay(15),
            periodeTom = september.atEndOfMonth()
        )
        val forskjøvedeVilkårResultater =
            forskyvVilkårResultater(Vilkår.BARNETS_ALDER, listOf(vilkårResultat1))

        Assertions.assertEquals(0, forskjøvedeVilkårResultater.size)
    }
}
