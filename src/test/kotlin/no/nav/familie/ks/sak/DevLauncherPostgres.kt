package no.nav.familie.ks.sak

import no.nav.familie.ks.sak.config.ApplicationConfig
import no.nav.familie.ks.sak.testconfig.DbContainerInitializer
import org.springframework.boot.builder.SpringApplicationBuilder

fun main(args: Array<String>) {
    System.setProperty("spring.profiles.active", "postgres")
    val springBuilder = SpringApplicationBuilder(ApplicationConfig::class.java).profiles(
        "dev",
        "postgres",
        "mock-brev-klient",
        "mock-økonomi",
        "mock-infotrygd-feed",
        "mock-infotrygd-barnetrygd",
        "mock-pdl",
        "mock-ident-client",
        "mock-tilbakekreving-klient",
        "task-scheduling"
    )

    if (args.contains("--dbcontainer")) {
        springBuilder.initializers(DbContainerInitializer())
    }

    springBuilder.run(*args)
}
