package no.nav.familie.ks.sak

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.ApplicationConfig
import no.nav.familie.ks.sak.config.DbContainerInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.builder.SpringApplicationBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

class DevLauncherPostgresPreprod

fun main(args: Array<String>) {
    System.setProperty("spring.profiles.active", "dev-postgres-preprod")
    val springBuilder =
        SpringApplicationBuilder(ApplicationConfig::class.java).profiles(
            "mock-økonomi",
            "mock-infotrygd-replika",
        )

    if (args.contains("--dbcontainer")) {
        springBuilder.initializers(DbContainerInitializer())
    }

    if (System.getProperty("AZURE_APP_CLIENT_ID") == null) {
        settClientIdOgSecret()
    }

    springBuilder.run(*args)
}

private fun settClientIdOgSecret() {
    val cmd = "./hentMiljøvariabler.sh"

    val logger: Logger = LoggerFactory.getLogger("main -> settClientIdOgSecret")
    logger.info("Henter miljøvariabler fra Kubernetes...")
    val process = ProcessBuilder(cmd).start()

    if (process.waitFor() == 1) {
        val inputStream = BufferedReader(InputStreamReader(process.inputStream))
        inputStream.lines().forEach { println(it) }
        inputStream.close()
        throw Feil("Klarte ikke hente variabler fra Nais. Er du logget på Naisdevice og gcloud?")
    }

    val inputStream = BufferedReader(InputStreamReader(process.inputStream))
    inputStream.readLine() // "Switched to context dev-gcp"
    val clientIdOgSecret = inputStream.readLine().split(";")
    inputStream.close()

    clientIdOgSecret.forEach {
        val keyValuePar = it.split("=")
        System.setProperty(keyValuePar[0], keyValuePar[1])
    }
    logger.info("Miljøvariabler hentet og satt \u2713")
}
