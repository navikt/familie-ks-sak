package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class INasjonalEllerFellesBegrunnelseTest {
    @Test
    fun `Skal ikke være to begrunnelser med samme Apinavn`() {
        val alleBegrunnelserApiNain = (NasjonalEllerFellesBegrunnelse.entries + EØSBegrunnelse.entries).map { it.sanityApiNavn }

        assertThat(alleBegrunnelserApiNain.size).isEqualTo(alleBegrunnelserApiNain.toSet().size)
    }
}
