package no.nav.familie.ks.sak.data

fun <T> T?.shouldNotBeNull(): T = this ?: throw AssertionError("$this kan ikke være null")
