package no.nav.familie.ks.sak.config

import io.mockk.mockk
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.kafka.listener.MessageListenerContainer

class KafkaErrorHandlerTest {
    private val container = mockk<MessageListenerContainer>(relaxed = true)
    private val consumer = mockk<Consumer<*, *>>(relaxed = true)

    private val errorHandler = KafkaErrorHandler()

    @Test
    fun `handle skal stoppe container hvis man mottar feil med en tom liste med records`() {
        val exceptionThrown =
            Assertions.assertThatThrownBy {
                errorHandler.handleRemaining(
                    RuntimeException("Feil i test"),
                    emptyList(),
                    consumer,
                    container,
                )
            }
        exceptionThrown.hasCauseExactlyInstanceOf(Exception::class.java)

        val cause = exceptionThrown.cause()

        cause.hasMessageNotContaining("Feil i test").hasMessageContaining("Sjekk securelogs for mer info")
    }

    @Test
    fun `handle skal stoppe container hvis man mottar feil med en liste med records`() {
        val consumerRecord = ConsumerRecord("topic", 1, 1, 1, "record")
        val exceptionThrown =
            Assertions.assertThatThrownBy {
                errorHandler.handleRemaining(
                    RuntimeException("Feil i test"),
                    listOf(consumerRecord),
                    consumer,
                    container,
                )
            }
        exceptionThrown.hasCauseExactlyInstanceOf(Exception::class.java)

        val cause = exceptionThrown.cause()

        cause.hasMessageNotContaining("Feil i test").hasMessageContaining("Sjekk securelogs for mer info")
    }
}
