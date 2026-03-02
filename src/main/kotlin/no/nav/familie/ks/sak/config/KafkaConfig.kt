package no.nav.familie.ks.sak.config

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import no.nav.familie.kontrakter.felles.Applikasjon
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties

@Configuration
@EnableKafka
@Profile("preprod", "prod")
class KafkaConfig(
    @Value("\${KAFKA_BROKERS:localhost}") private val kafkaBrokers: String,
    @Value("\${KAFKA_TRUSTSTORE_PATH}") private val kafkaTruststorePath: String,
    @Value("\${KAFKA_CREDSTORE_PASSWORD}") private val kafkaCredstorePassword: String,
    @Value("\${KAFKA_KEYSTORE_PATH}") private val kafkaKeystorePath: String,
) {
    @Bean
    fun producerFactory(): ProducerFactory<String, String> = DefaultKafkaProducerFactory(producerConfigs())

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> = KafkaTemplate(producerFactory())

    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> = DefaultKafkaConsumerFactory(consumerConfigs())

    @Bean
    fun earliestConsumerFactory(): ConsumerFactory<String, String> = DefaultKafkaConsumerFactory(consumerConfigsEarliest())

    @Bean
    fun earliestConsumerFactoryAvro(): ConsumerFactory<String, String> = DefaultKafkaConsumerFactory(consumerConfigsEarliestAvro())

    @Bean
    fun concurrentKafkaListenerContainerFactory(
        kafkaErrorHandler: KafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setConcurrency(1)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            setConsumerFactory(consumerFactory())
            setCommonErrorHandler(kafkaErrorHandler)
        }

    @Bean
    fun earliestConcurrentKafkaListenerContainerFactory(
        kafkaErrorHandler: KafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setConcurrency(1)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            setConsumerFactory(earliestConsumerFactory())
            setCommonErrorHandler(kafkaErrorHandler)
        }

    @Bean
    fun kafkaAivenHendelseListenerAvroLatestContainerFactory(kafkaErrorHandler: KafkaErrorHandler): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
            setConsumerFactory(DefaultKafkaConsumerFactory(consumerConfigsLatestAvro()))
            setCommonErrorHandler(kafkaErrorHandler)
        }

    @Bean
    fun earliestConcurrentKafkaListenerContainerFactoryAvro(
        kafkaErrorHandler: KafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setConcurrency(1)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            setConsumerFactory(earliestConsumerFactoryAvro())
            setCommonErrorHandler(kafkaErrorHandler)
        }

    private fun consumerConfigsEarliestAvro(): Map<String, Any> {
        val kafkaBrokers = System.getenv("KAFKA_BROKERS") ?: "http://localhost:9092"
        val schemaRegistry = System.getenv("KAFKA_SCHEMA_REGISTRY") ?: "http://localhost:9093"
        val schemaRegistryUser = System.getenv("KAFKA_SCHEMA_REGISTRY_USER") ?: "mangler i pod"
        val schemaRegistryPassword = System.getenv("KAFKA_SCHEMA_REGISTRY_PASSWORD") ?: "mangler i pod"
        val consumerConfigs =
            mutableMapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
                "schema.registry.url" to schemaRegistry,
                "basic.auth.credentials.source" to "USER_INFO",
                "basic.auth.user.info" to "$schemaRegistryUser:$schemaRegistryPassword",
                "specific.avro.reader" to true,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
                ConsumerConfig.CLIENT_ID_CONFIG to "consumer-familie-ks-sak-1",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            )
        return consumerConfigs.toMap() + securityConfig()
    }

    private fun consumerConfigsLatestAvro(): Map<String, Any> {
        val kafkaBrokers = System.getenv("KAFKA_BROKERS") ?: "http://localhost:9092"
        val schemaRegistry = System.getenv("KAFKA_SCHEMA_REGISTRY") ?: "http://localhost:9093"
        val schemaRegistryUser = System.getenv("KAFKA_SCHEMA_REGISTRY_USER") ?: "mangler i pod"
        val schemaRegistryPassword = System.getenv("KAFKA_SCHEMA_REGISTRY_PASSWORD") ?: "mangler i pod"
        val consumerConfigs =
            mutableMapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
                "schema.registry.url" to schemaRegistry,
                "basic.auth.credentials.source" to "USER_INFO",
                "basic.auth.user.info" to "$schemaRegistryUser:$schemaRegistryPassword",
                "specific.avro.reader" to true,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
                ConsumerConfig.CLIENT_ID_CONFIG to "consumer-familie-ks-sak-2",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
            )
        return consumerConfigs.toMap() + securityConfig()
    }

    private fun producerConfigs() =
        mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            // Den sikrer rekkefølge
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            // Den sikrer at data ikke mistes
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.CLIENT_ID_CONFIG to Applikasjon.FAMILIE_KS_SAK.name,
        ) + securityConfig()

    fun consumerConfigs() =
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to "familie-ks-sak",
            ConsumerConfig.CLIENT_ID_CONFIG to "consumer-familie-ks-sak-3",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
            CommonClientConfigs.RETRIES_CONFIG to 10,
            CommonClientConfigs.RETRY_BACKOFF_MS_CONFIG to 100,
        ) + securityConfig()

    fun consumerConfigsEarliest() =
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to "familie-ks-sak",
            ConsumerConfig.CLIENT_ID_CONFIG to "consumer-familie-ks-sak-4",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            CommonClientConfigs.RETRIES_CONFIG to 10,
            CommonClientConfigs.RETRY_BACKOFF_MS_CONFIG to 100,
        ) + securityConfig()

    private fun securityConfig() =
        mapOf(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
            // Disable server host name verification
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to kafkaTruststorePath,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to kafkaKeystorePath,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to kafkaCredstorePassword,
        )

    companion object {
        const val BARNEHAGELISTE_AAPEN_TOPIC = "alf.aapen-altinn-barnehageliste-mottatt"
        const val BARNEHAGELISTE_TOPIC = "teamfamilie.privat-kontantstotte-barnehagelister"
        const val BEHANDLING_TOPIC = "teamfamilie.aapen-kontantstotte-saksstatistikk-behandling-v1"
        const val SISTE_TILSTAND_BEHANDLING_TOPIC = "teamfamilie.aapen-kontantstotte-saksstatistikk-siste-tilstand-behandling-v1"
        const val SAK_TOPIC = "teamfamilie.aapen-kontantstotte-saksstatistikk-sak-v1"
        const val VEDTAK_TOPIC = "teamfamilie.aapen-kontantstotte-vedtak-v1"
        const val KONTANTSTØTTE_FEED_TOPIC = "teamfamilie.aapen-feed-kontantstotte-v1"
        const val PDL_AKTOR_V2_TOPIC = "pdl.aktor-v2"
    }
}
