package no.nav.familie.ks.sak.config

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.kafka.listener.MessageListenerContainer

@ExtendWith(MockKExtension::class)
class KafkaErrorHandlerTest {
    @MockK(relaxed = true)
    lateinit var container: MessageListenerContainer

    @MockK(relaxed = true)
    lateinit var consumer: Consumer<*, *>

    @InjectMockKs
    lateinit var errorHandler: KafkaErrorHandler

    @Test
    fun `handle skal stoppe container hvis man mottar feil med en tom liste med records`() {
        val exceptionThrown = Assertions.assertThatThrownBy {
            errorHandler.handleRemaining(
                RuntimeException("Feil i test"),
                emptyList(),
                consumer,
                container
            )
        }
        exceptionThrown.hasCauseExactlyInstanceOf(Exception::class.java)

        val cause = exceptionThrown.cause()

        cause.hasMessageNotContaining("Feil i test").hasMessageContaining("Sjekk securelogs for mer info")
    }

    @Test
    fun `handle skal stoppe container hvis man mottar feil med en liste med records`() {
        val consumerRecord = ConsumerRecord("topic", 1, 1, 1, "record")
        val exceptionThrown = Assertions.assertThatThrownBy {
            errorHandler.handleRemaining(
                RuntimeException("Feil i test"),
                listOf(consumerRecord),
                consumer,
                container
            )
        }
        exceptionThrown.hasCauseExactlyInstanceOf(Exception::class.java)

        val cause = exceptionThrown.cause()

        cause.hasMessageNotContaining("Feil i test").hasMessageContaining("Sjekk securelogs for mer info")
    }
}
