package no.nav.familie.ks.sak.common

import java.time.Clock

fun interface ClockProvider {
    fun get(): Clock
}
