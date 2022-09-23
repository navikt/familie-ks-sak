package no.nav.familie.ks.sak

import no.nav.familie.ks.sak.config.ApplicationConfig
import no.nav.familie.ks.sak.config.DbContainerInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication
class DevLauncherLocal

fun main(args: Array<String>) {
    System.setProperty(
        "spring.profiles.active",
        "local, mock-pdl, mock-oauth, mock-oppgave, mock-integrasjoner, mock-økonomi"
    )

    SpringApplicationBuilder(ApplicationConfig::class.java)
        .initializers(DbContainerInitializer())
        .profiles("local", "mock-pdl", "mock-oauth", "mock-oppgave", "mock-integrasjoner", "mock-økonomi")
        .run(*args)
}
