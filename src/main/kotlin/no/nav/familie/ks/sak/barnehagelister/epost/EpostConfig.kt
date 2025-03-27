package no.nav.familie.ks.sak.barnehagelister.epost

import com.azure.identity.ClientSecretCredentialBuilder
import com.microsoft.graph.serviceclient.GraphServiceClient
import org.slf4j.LoggerFactory
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
        secureLogger.info("GraphService har tenantId med disse tegnene\n tenant: ${tenantId.first()} id: ${clientId.first()} secret: ${clientSecret.first()}")

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

data class LoginParams(
    val clientId: String,
    val clientSecret: String,
    val tenantId: String,
)
