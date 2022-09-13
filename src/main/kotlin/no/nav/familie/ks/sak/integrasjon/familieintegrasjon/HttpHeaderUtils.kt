package no.nav.familie.ks.sak.integrasjon.familieintegrasjon

import org.springframework.http.HttpHeaders

fun HttpHeaders.medContentTypeJsonUTF8(): HttpHeaders =
    this.apply {
        add("Content-Type", "application/json;charset=UTF-8")
        acceptCharset = listOf(Charsets.UTF_8)
    }
