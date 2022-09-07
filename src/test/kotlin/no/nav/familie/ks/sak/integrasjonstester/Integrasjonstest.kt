package no.nav.familie.ks.sak.integrasjonstester

import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.hamcrest.CoreMatchers.`is` as Is

class Integrasjonstest {

    @Test
    fun `Simple integrasjonstest`() {
        assertThat(10 + 5, Is(15))
    }
}
