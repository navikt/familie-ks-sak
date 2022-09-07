package no.nav.familie.ks.sak.enhetstester
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.hamcrest.CoreMatchers.`is` as Is

class Enhetstest {

    @Test
    fun `Simple enhettest`() {
        assertThat(5 + 5, Is(10))
    }
}
