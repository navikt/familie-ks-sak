package no.nav.familie.ks.sak.barnehagelister.epost

import com.azure.identity.ClientSecretCredentialBuilder
import com.microsoft.graph.serviceclient.GraphServiceClient
import no.nav.familie.ks.sak.integrasjon.pdl.secureLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EpostConfig(
    @Value("\${AZURE_APP_CLIENT_ID}") private val clientId: String,
    @Value("\${AZURE_APP_CLIENT_SECRET}") private val clientSecret: String,
    @Value("\${AZURE_APP_TENANT_ID}") private val tenantId: String,
) {
    private val scope = "https://graph.microsoft.com/.default"

    @Bean
    fun graphServiceClient(): GraphServiceClient {
        val tenantErSatt = tenantId.isNotBlank()
        val idErSatt = clientId.isNotBlank()
        val tenantErSattIProperty = System.getProperty("AZURE_APP_TENANT_ID").isNotBlank()
        val tenantErSattIEnv = System.getenv("AZURE_APP_TENANT_ID").isNotBlank()

        secureLogger.info("GraphService har disse env-variablene satt\n tenant: $tenantErSatt id: $idErSatt tenantIProperty: $tenantErSattIProperty tenantIEnv: $tenantErSattIEnv")

        val credential =
            ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build()
        return GraphServiceClient(credential, scope)
    }
}
