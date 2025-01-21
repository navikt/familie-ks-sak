package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.kjerne.beregning.lovverkFebruar2025.RegelverkLovendringFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.lovverkFørFebruar2025.LovverkFørFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.regelverk.Lovverk
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

class AndelGeneratorLookupTest(
    @Autowired private val andelGeneratorLookup: AndelGenerator.Lookup,
) : OppslagSpringRunnerTest() {
    @ParameterizedTest
    @EnumSource(value = Lovverk::class)
    fun `skal finne alle implementasjoner av AndelGenerator`(lovverk: Lovverk) {
        // Act
        val andelGenerator = andelGeneratorLookup.hentGeneratorForRegelverk(lovverk)

        // Assert
        assertNotNull(andelGenerator)
    }

    @Test
    fun `skal finne RegelverkFørFebruar2025AndelGenerator når regelverk er FØR_LOVENDRING_2025`() {
        // Act
        val andelGenerator = andelGeneratorLookup.hentGeneratorForRegelverk(Lovverk.FØR_LOVENDRING_2025)

        // Assert
        assertNotNull(andelGenerator)
        assertInstanceOf(LovverkFørFebruar2025AndelGenerator::class.java, andelGenerator)
    }

    @Test
    fun `skal finne RegelverkLovendringFebruar2025AndelGenerator når regelverk er LOVENDRING_FEBRUAR_2025`() {
        // Act
        val andelGenerator = andelGeneratorLookup.hentGeneratorForRegelverk(Lovverk.LOVENDRING_FEBRUAR_2025)

        // Assert
        assertNotNull(andelGenerator)
        assertInstanceOf(RegelverkLovendringFebruar2025AndelGenerator::class.java, andelGenerator)
    }
}
