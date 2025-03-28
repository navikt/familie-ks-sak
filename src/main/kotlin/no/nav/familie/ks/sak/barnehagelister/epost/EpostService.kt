package no.nav.familie.ks.sak.barnehagelister.epost

import com.microsoft.graph.models.BodyType
import com.microsoft.graph.models.EmailAddress
import com.microsoft.graph.models.ItemBody
import com.microsoft.graph.models.Message
import com.microsoft.graph.models.Recipient
import com.microsoft.graph.serviceclient.GraphServiceClient
import com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody
import no.nav.familie.ks.sak.common.exception.Feil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EpostService(
    val graphServiceClient: GraphServiceClient,
) {
    fun sendEpostVarslingBarnehagelister(
        epostadresse: String,
        kommuner: Set<String>,
    ) {
        val message =
            Message().also {
                it.subject = "Barnehagelister"
                it.body =
                    ItemBody().also { body ->
                        body.contentType = BodyType.Text
                        body.content = lagBarnehagelistemelding(kommuner)
                    }
                it.toRecipients =
                    listOf(
                        Recipient().also { recipient ->
                            recipient.emailAddress =
                                EmailAddress().also { epostAdresse ->
                                    epostAdresse.address = epostadresse
                                }
                        },
                    )
            }

        val sendMailPostRequestBody =
            SendMailPostRequestBody().also {
                it.message = message
                it.saveToSentItems = false
            }

        try {
            graphServiceClient
                .users()
                .byUserId(`BAKS_E-POST`)
                .sendMail()
                .post(sendMailPostRequestBody)
            logger.info("Epost sendt via Azure")
        } catch (e: Exception) {
            logger.error("Feilmelding fra Microsoft Graph Api: ${e.message}")
            throw Feil("Feilmelding fra Microsoft Graph Api: ${e.message}")
        }
    }

    private fun lagBarnehagelistemelding(kommuner: Set<String>): String =
        """
        Følgende kommuner har mottatt barnehagelister i løpet av det siste døgnet:
        ${kommuner.joinToString(" ")}
        """.trimIndent()

    companion object {
        private val logger = LoggerFactory.getLogger(EpostService::class.java)
        private val `BAKS_E-POST` = "ikke.svar.kontantstotte@nav.no"
    }
}
