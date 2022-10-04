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
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
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
@Tag("integrationTest")
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

    @Autowired
    private lateinit var aktørRepository: AktørRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    lateinit var søker: Aktør

    lateinit var fagsak: Fagsak

    lateinit var behandling: Behandling

    @LocalServerPort
    var port: Int = 0

    @BeforeEach
    fun beforeEach() {
        søker = lagreAktør(randomAktør())
        fagsak = lagreFagsak(lagFagsak(aktør = søker))
        behandling = lagreBehandling(lagBehandling(fagsak = fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD))
    }

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

    fun lagreAktør(aktør: Aktør): Aktør = aktørRepository.saveAndFlush(aktør)

    fun lagreFagsak(fagsak: Fagsak): Fagsak = fagsakRepository.saveAndFlush(fagsak)

    fun lagreBehandling(behandling: Behandling): Behandling =
        behandlingRepository.saveAndFlush(behandling)

    companion object {
        protected fun initLoggingEventListAppender(): ListAppender<ILoggingEvent> =
            ListAppender<ILoggingEvent>().apply { start() }
    }
}
