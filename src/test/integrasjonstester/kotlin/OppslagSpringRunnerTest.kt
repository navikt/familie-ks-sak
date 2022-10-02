package no.nav.familie.ks.sak

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.nimbusds.jose.JOSEObjectType
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.DatabaseCleanupService
import no.nav.familie.ks.sak.config.DbContainerInitializer
import no.nav.familie.ks.sak.config.RolleConfig
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional

@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@SpringBootTest(classes = [DevLauncherLocal::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres")
@EnableMockOAuth2Server
abstract class OppslagSpringRunnerTest {

    private val listAppender = initLoggingEventListAppender()
    protected var loggingEvents: MutableList<ILoggingEvent> = listAppender.list

    @Autowired
    private lateinit var databaseCleanupService: DatabaseCleanupService

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @LocalServerPort
    var port: Int = 0

    @AfterEach
    @Transactional
    fun reset() {
        loggingEvents.clear()
        resetTableForAllEntityClass()
        clearCaches()
        resetWiremockServers()
    }

    protected fun lokalTestToken(
        issuerId: String = "azuread",
        subject: String = "subject1",
        behandlerRolle: BehandlerRolle? = null
    ): String {

        val behandlerRolle = when (behandlerRolle) {
            BehandlerRolle.VEILEDER -> rolleConfig.VEILEDER_ROLLE
            BehandlerRolle.SAKSBEHANDLER -> rolleConfig.SAKSBEHANDLER_ROLLE
            BehandlerRolle.BESLUTTER -> rolleConfig.BESLUTTER_ROLLE
            else -> ""
        }

        return mockOAuth2Server.issueToken(
            issuerId,
            "theclientid",
            DefaultOAuth2TokenCallback(
                issuerId,
                subject,
                JOSEObjectType.JWT.type,
                listOf("familie-ks-sak-test"),
                mapOf(Pair("NAVident", "test"), Pair("groups", listOf(behandlerRolle))),
                3600
            )
        ).serialize()
    }

    private fun resetWiremockServers() =
        applicationContext.getBeansOfType(WireMockServer::class.java).values.forEach(WireMockServer::resetRequests)

    private fun clearCaches() =
        cacheManager.cacheNames.mapNotNull { cacheManager.getCache(it) }
            .forEach { it.clear() }

    private fun resetTableForAllEntityClass() = databaseCleanupService.truncate()

    companion object {
        protected fun initLoggingEventListAppender(): ListAppender<ILoggingEvent> =
            ListAppender<ILoggingEvent>().apply { start() }
    }
}
