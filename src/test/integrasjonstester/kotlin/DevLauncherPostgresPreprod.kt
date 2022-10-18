package no.nav.familie.ks.sak

import no.nav.familie.ks.sak.config.ApplicationConfig
import no.nav.familie.ks.sak.config.DbContainerInitializer
import org.springframework.boot.builder.SpringApplicationBuilder

class DevLauncherPostgresPreprod

fun main(args: Array<String>) {
    System.setProperty("spring.profiles.active", "dev-postgres-preprod")
    val springBuilder = SpringApplicationBuilder(ApplicationConfig::class.java).profiles(
        "mock-økonomi"
    )

    if (args.contains("--dbcontainer")) {
        springBuilder.initializers(DbContainerInitializer())
    }

    springBuilder.run(*args)
}
