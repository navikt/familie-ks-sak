package no.nav.familie.ks.sak.config

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.Profiles
import org.testcontainers.containers.PostgreSQLContainer

class DbContainerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        // Only start Postgres when not running in CI
        if (!applicationContext.environment.acceptsProfiles(Profiles.of("ci"))) {
            postgres.start()
            TestPropertyValues.of(
                "spring.datasource.url=${postgres.jdbcUrl}",
                "spring.datasource.username=familie-ks-sak",
                "spring.datasource.password=WyFzSS3FmhwPCi85VRayzXnMhZRkxkD8SKjkW_BJVmM",
            ).applyTo(applicationContext.environment)
        }
    }

    companion object {
        // Lazy because we only want it to be initialized when accessed
        private val postgres: KPostgreSQLContainer by lazy {
            KPostgreSQLContainer("postgres:14.5")
                .withDatabaseName("databasename")
                .withUsername("familie-ks-sak")
                .withPassword("WyFzSS3FmhwPCi85VRayzXnMhZRkxkD8SKjkW_BJVmM")
        }
    }
}

// Hack needed because testcontainers use of generics confuses Kotlin
class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)
