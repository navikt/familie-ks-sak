package no.nav.familie.ks.sak.common.util

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class UtilsTest {
    @Test
    fun `konverterEnumsTilString - skal konvertere liste av enum verdier til semikolon-separert string`() {
        val enums: List<Vilkår> = listOf(Vilkår.MEDLEMSKAP, Vilkår.BARNEHAGEPLASS, Vilkår.BOR_MED_SØKER)
        val enumString = konverterEnumsTilString(enums)
        assertEquals("${Vilkår.MEDLEMSKAP};${Vilkår.BARNEHAGEPLASS};${Vilkår.BOR_MED_SØKER}", enumString)
    }

    @Test
    fun `konverterStringTilEnums - skal konvertere semikolon-separert enumString til liste av enum verdier`() {
        val enumString = "${Vilkår.MEDLEMSKAP};${Vilkår.BARNEHAGEPLASS};${Vilkår.BOR_MED_SØKER}"
        val enums = konverterStringTilEnums<Vilkår>(enumString)
        assertEquals(listOf(Vilkår.MEDLEMSKAP, Vilkår.BARNEHAGEPLASS, Vilkår.BOR_MED_SØKER), enums)
    }

    @Test
    fun `Navn i uppercase med mellomrom og bindestrek blir formatert korrekt`() = assertEquals("Hense-Ravnen Hopp", "HENSE-RAVNEN HOPP".storForbokstavIAlleNavn())

    @Nested
    inner class TilEtterfølgendePar {
        @Test
        fun `skal plukke ut to og to etterfølgende elementer inkludert det siste elementet`() {
            val liste = listOf(1, 2, 3, 4, 5)
            val par = liste.tilEtterfølgendePar { a, b -> a to b }
            assertThat(par).hasSize(5)
            assertThat(par[0]).isEqualTo(Pair(1, 2))
            assertThat(par[1]).isEqualTo(Pair(2, 3))
            assertThat(par[2]).isEqualTo(Pair(3, 4))
            assertThat(par[3]).isEqualTo(Pair(4, 5))
            assertThat(par[4]).isEqualTo(Pair(5, null))
        }
    }
}
