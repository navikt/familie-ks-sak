package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.kjerne.regelverk.Regelverk
import org.springframework.stereotype.Component

@Component
class AndelGeneratorFactory(
    private val andelGeneratorer: List<AndelGenerator>,
) {
    fun hentGeneratorForRegelverk(regelverk: Regelverk) = andelGeneratorer.single { it.regelverk == regelverk }
}
