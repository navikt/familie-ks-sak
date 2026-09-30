package no.nav.familie.ks.sak.task

import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.SlettetBatch
import no.nav.familie.ks.sak.task.SlettInaktivePersonopplysningsgrunnlagScheduler.Companion.summerAntallSlettedeRaderPerTabell
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test

class SlettInaktivePersonopplysningsgrunnlagSchedulerTest {
    @Test
    fun `skal summere antall slettede rader per tabell over alle batcher og bevare rekkefølgen på tabellene`() {
        // Arrange
        val førsteBatch =
            SlettetBatch(
                grunnlagIder = listOf(1L, 2L),
                antallSlettedeRaderPerTabell = linkedMapOf("gr_personopplysninger" to 2L, "po_person" to 5L, "po_statsborgerskap" to 7L),
            )
        val andreBatch =
            SlettetBatch(
                grunnlagIder = listOf(3L),
                antallSlettedeRaderPerTabell = linkedMapOf("gr_personopplysninger" to 1L, "po_person" to 3L, "po_statsborgerskap" to 0L),
            )

        // Act
        val summert = summerAntallSlettedeRaderPerTabell(listOf(førsteBatch, andreBatch))

        // Assert
        assertThat(summert).containsExactly(
            entry("gr_personopplysninger", 3L),
            entry("po_person", 8L),
            entry("po_statsborgerskap", 7L),
        )
    }

    @Test
    fun `skal returnere tomt resultat når det ikke er noen batcher å summere`() {
        // Act
        val summert = summerAntallSlettedeRaderPerTabell(emptyList())

        // Assert
        assertThat(summert).isEmpty()
    }
}
