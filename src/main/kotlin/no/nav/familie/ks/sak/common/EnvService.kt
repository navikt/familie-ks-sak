package no.nav.familie.ks.sak.common

import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class EnvService(private val environment: Environment) {
    fun erProd(): Boolean = environment.activeProfiles.any { it == "prod" }

    fun erPreprod(): Boolean = environment.activeProfiles.any { it == "preprod" }

    fun erLokal(): Boolean = environment.activeProfiles.any { it == "dev-postgres-preprod" || it == "postgres" }
}
