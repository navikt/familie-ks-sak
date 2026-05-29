package no.nav.familie.ks.sak

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.ApplicationConfig
import no.nav.familie.ks.sak.config.DbContainerInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.builder.SpringApplicationBuilder
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
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

    if (!args.contains("--texasdocker") && System.getProperty("NAIS_TOKEN_ENDPOINT") == null) {
        startTexasContainer()
    }

    springBuilder.run(*args)
}

private fun startTexasContainer() {
    val logger: Logger = LoggerFactory.getLogger("main -> startTexasContainer")
    logger.info("Starter Texas-container via Testcontainers...")

    val tenantId = System.getProperty("AZURE_APP_TENANT_ID")
    val texas =
        GenericContainer("ghcr.io/nais/texas:latest")
            .withExposedPorts(7575)
            .withEnv(
                mapOf(
                    "BIND_ADDRESS" to "0.0.0.0:7575",
                    "AZURE_ENABLED" to "true",
                    "TEXAS_HTTP_CONNECT_TIMEOUT_MILLIS" to "10000",
                    "TEXAS_HTTP_READ_TIMEOUT_MILLIS" to "10000",
                    "TEXAS_HTTP_OVERALL_TIMEOUT_MILLIS" to "10000",
                    "AZURE_APP_CLIENT_ID" to System.getProperty("AZURE_APP_CLIENT_ID"),
                    "AZURE_APP_CLIENT_SECRET" to System.getProperty("AZURE_APP_CLIENT_SECRET"),
                    "AZURE_APP_TENANT_ID" to tenantId,
                    "AZURE_APP_JWK" to System.getProperty("AZURE_APP_JWK"),
                    "AZURE_OPENID_CONFIG_ISSUER" to "https://login.microsoftonline.com/$tenantId/v2.0",
                    "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "https://login.microsoftonline.com/$tenantId/oauth2/v2.0/token",
                    "AZURE_OPENID_CONFIG_JWKS_URI" to "https://login.microsoftonline.com/$tenantId/discovery/v2.0/keys",
                ),
            ).waitingFor(Wait.forListeningPort())
            .apply { start() }

    val port = texas.getMappedPort(7575)
    System.setProperty("NAIS_TOKEN_ENDPOINT", "http://localhost:$port/api/v1/token")
    System.setProperty("NAIS_TOKEN_EXCHANGE_ENDPOINT", "http://localhost:$port/api/v1/token/exchange")

    logger.info("Texas startet på port $port \u2713")
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
    val lines = inputStream.readLines()
    inputStream.close()

    lines.forEach {
        val keyValuePar = it.split("=", limit = 2)
        if (keyValuePar.size == 2) {
            System.setProperty(keyValuePar[0], keyValuePar[1])
        }
    }
    logger.info("Miljøvariabler hentet og satt \u2713")
}
