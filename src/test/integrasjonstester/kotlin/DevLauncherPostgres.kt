package no.nav.familie.ks.sak

import no.nav.familie.ks.sak.config.ApplicationConfig
import no.nav.familie.ks.sak.config.DbContainerInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication
class DevLauncherPostgres

fun main(args: Array<String>) {
    System.setProperty("spring.profiles.active", "postgres")
    val springBuilder =
        SpringApplicationBuilder(ApplicationConfig::class.java).profiles(
            "local",
            "mock-pdl",
            "mock-oauth",
            "mock-oppgave",
            "mock-integrasjoner",
            "mock-økonomi",
        )

    if (args.contains("--dbcontainer")) {
        springBuilder.initializers(DbContainerInitializer())
    }

    springBuilder.run(*args)
}
