package no.nav.familie.ks.sak.verdikjedetester

import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.hamcrest.CoreMatchers.`is` as Is

class Verdikjedetest {

    @Test
    fun `Simple Verdikjedetest`() {
        assertThat(10 + 5, Is(15))
    }
}
