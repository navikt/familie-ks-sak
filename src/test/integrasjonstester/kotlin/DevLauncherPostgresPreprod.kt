package no.nav.familie.ks.sak

import no.nav.familie.ks.sak.config.ApplicationConfig
import no.nav.familie.ks.sak.config.DbContainerInitializer
import org.springframework.boot.builder.SpringApplicationBuilder
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

class DevLauncherPostgresPreprod

fun main(args: Array<String>) {
    System.setProperty("spring.profiles.active", "dev-postgres-preprod")
    val springBuilder = SpringApplicationBuilder(ApplicationConfig::class.java).profiles(
        "mock-økonomi"
    )

    if (args.contains("--dbcontainer")) {
        springBuilder.initializers(DbContainerInitializer())
    }

    settClientIdOgSecret()

    springBuilder.run(*args)
}

private fun settClientIdOgSecret() {
    val cmd = "./hentMiljøvariabler.sh"

    val process = ProcessBuilder(cmd).start()

    val status = process.waitFor()
    if (status == 1) {
        error("Klarte ikke hente variabler fra Nais. Er du logget på Naisdevice og gcloud?")
        exitProcess(1)
    } else if (status == 2) {
        error("Feil context satt for kubectl, du må bruke dev-gcp?")
        exitProcess(2)
    }

    val inputStream = BufferedReader(InputStreamReader(process.inputStream))
    val clientIdOgSecret = inputStream.readLine().split(";")
    inputStream.close()

    clientIdOgSecret.forEach {
        val keyValuePar = it.split("=")
        System.setProperty(keyValuePar[0], keyValuePar[1])
    }
}
