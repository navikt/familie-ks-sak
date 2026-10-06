package no.nav.familie.ks.sak.config

import io.mockk.mockk
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.kafka.listener.MessageListenerContainer

class KafkaErrorHandlerTest {
    private val container = mockk<MessageListenerContainer>(relaxed = true)
    private val consumer = mockk<Consumer<*, *>>(relaxed = true)

    private val errorHandler = KafkaErrorHandler()

    @Test
    fun `handle skal stoppe container hvis man mottar feil med en tom liste med records`() {
        // Arrange & Act
        val exceptionThrown =
            Assertions.assertThatThrownBy {
                errorHandler.handleRemaining(
                    RuntimeException("Feil i test"),
                    emptyList(),
                    consumer,
                    container,
                )
            }

        // Assert
        exceptionThrown.hasCauseExactlyInstanceOf(Exception::class.java)

        val cause = exceptionThrown.cause()

        cause.hasMessageNotContaining("Feil i test").hasMessageContaining("Sjekk securelogs for mer info")
    }

    @Test
    fun `handle skal stoppe container hvis man mottar feil med en liste med records`() {
        // Arrange
        val consumerRecord = ConsumerRecord("topic", 1, 1, 1, "record")

        // Act & Assert
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

    @Test
    fun `skal stoppe container og håndtere feil for en batch`() {
        // Arrange
        val partition = TopicPartition("topic", 1)
        val records = ConsumerRecords(mapOf(partition to listOf(ConsumerRecord("topic", 1, 1, 1, "record"))), emptyMap())

        // Act & Assert
        val exceptionThrown =
            Assertions.assertThatThrownBy {
                errorHandler.handleBatch(RuntimeException("Feil i test"), records, consumer, container, Runnable {})
            }
        exceptionThrown.hasCauseExactlyInstanceOf(Exception::class.java)
        exceptionThrown.cause().hasMessageNotContaining("Feil i test").hasMessageContaining("Sjekk securelogs for mer info")
    }
}
