package no.nav.familie.ks.sak

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HalloTest {
    @Test
    fun hallo() {
        assertThat("Hallo").isEqualTo(Hallo().hallo())
    }
}
