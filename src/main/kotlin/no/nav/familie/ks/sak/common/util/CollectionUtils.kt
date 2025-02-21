package no.nav.familie.ks.sak.common.util

fun <T> Collection<T>.containsExactly(vararg elements: T): Boolean {
    if (this.size != elements.size) {
        return false
    }
    this.forEachIndexed { index, element ->
        if (element != elements[index]) {
            return false
        }
    }
    return true
}
