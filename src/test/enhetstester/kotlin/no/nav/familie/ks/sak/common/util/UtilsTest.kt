package no.nav.familie.ks.sak.common.util

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions.assertEquals
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
}
