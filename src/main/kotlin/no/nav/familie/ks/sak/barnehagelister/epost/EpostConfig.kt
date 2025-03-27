package no.nav.familie.ks.sak.barnehagelister.epost

import com.azure.identity.ClientSecretCredentialBuilder
import com.microsoft.graph.serviceclient.GraphServiceClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class EpostConfig(
    @Value("\${AZURE_APP_CLIENT_ID}") private val clientId: String,
    @Value("\${AZURE_APP_CLIENT_SECRET}") private val clientSecret: String,
    @Value("\${AZURE_APP_TENANT_ID}") private val tenantId: String,
    val environment: Environment,
) {
    private val scope = "https://graph.microsoft.com/.default"

    @Bean
    fun graphServiceClient(): GraphServiceClient {
        val tenantErSatt = tenantId.isNotBlank()
        val secretErSatt = clientSecret.isNotBlank()
        val idErSatt = clientId.isNotBlank()

        val tenantErSattIEnv = environment.getProperty("AZURE_APP_TENANT_ID").isNullOrBlank()

        val envTenantErLikTenant = tenantId == environment.getProperty("AZURE_APP_TENANT_ID")

        secureLogger.info("GraphService har disse env-variablene satt\n tenant: $tenantErSatt secret: $secretErSatt id: $idErSatt tenantIEnv: $tenantErSattIEnv envTenantErLikTenant: $envTenantErLikTenant")

        val credential =
            ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build()
        return GraphServiceClient(credential, scope)
    }

    companion object {
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
