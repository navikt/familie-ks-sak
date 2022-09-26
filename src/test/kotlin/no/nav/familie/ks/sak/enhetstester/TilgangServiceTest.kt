package no.nav.familie.ks.sak.enhetstester

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.RolleConfig
import no.nav.familie.ks.sak.data.Testdata.defaultFagsak
import no.nav.familie.ks.sak.data.Testdata.lagBehandling
import no.nav.familie.ks.sak.data.Testdata.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.Personident
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.ks.sak.sikkerhet.AuditLogger
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import no.nav.familie.ks.sak.utils.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ks.sak.utils.BrukerContextUtil.mockBrukerContext
import no.nav.familie.log.mdc.MDCConstants
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.MDC
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class TilgangServiceTest {

    @MockK
    private lateinit var integrasjonService: IntegrasjonService

    @MockK
    private lateinit var behandlingRepository: BehandlingRepository

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    private val cacheManager = ConcurrentMapCacheManager()
    private val kode6Gruppe = "kode6"
    private val kode7Gruppe = "kode7"
    private val rolleConfig = RolleConfig("", "", "", KODE6 = kode6Gruppe, KODE7 = kode7Gruppe)
    private val auditLogger = AuditLogger("familie-ks-sak")
    private lateinit var tilgangService: TilgangService

    private val fagsak = defaultFagsak()
    private val behandling = lagBehandling(fagsak)
    private val aktør = fagsak.aktør
    private val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
        behandlingId = behandling.id,
        søkerPersonIdent = aktør.aktivFødselsnummer(),
        barnasIdenter = emptyList()
    )
    private val olaIdent = "4567"

    @BeforeAll
    internal fun beforeAll() {
        tilgangService = TilgangService(
            integrasjonService = integrasjonService,
            behandlingRepository = behandlingRepository,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            fagsakService = fagsakService,
            rolleConfig = rolleConfig,
            cacheManager = cacheManager,
            auditLogger = auditLogger
        )
    }

    @BeforeEach
    internal fun beforeEach() {
        MDC.put(MDCConstants.MDC_CALL_ID, "00001111")
        mockBrukerContext()
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { behandlingRepository.finnAktivBehandling(any()) } returns behandling
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns personopplysningGrunnlag
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til person`() {
        every { integrasjonService.sjekkTilgangTilPersoner(any()) } returns Tilgang(false)

        assertThrows<RolleTilgangskontrollFeil> {
            tilgangService.validerTilgangTilPersoner(
                listOf(aktør.aktivFødselsnummer()),
                AuditLoggerEvent.ACCESS,
                BehandlerRolle.SAKSBEHANDLER,
                ""
            )
        }
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til person`() {
        every { integrasjonService.sjekkTilgangTilPersoner(any()) } returns Tilgang(true)

        tilgangService.validerTilgangTilPersoner(
            listOf(aktør.aktivFødselsnummer()),
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            ""
        )
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til behandling`() {
        every { integrasjonService.sjekkTilgangTilPersoner(any()) } returns Tilgang(false)

        assertThrows<RolleTilgangskontrollFeil> {
            tilgangService.validerTilgangTilBehandling(
                behandling.id,
                AuditLoggerEvent.ACCESS,
                BehandlerRolle.SAKSBEHANDLER,
                "hente behandling"
            )
        }
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til behandling`() {
        every { integrasjonService.sjekkTilgangTilPersoner(any()) } returns Tilgang(true)

        tilgangService.validerTilgangTilBehandling(
            behandling.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "hente behandling"
        )
    }

    @Test
    internal fun `validerTilgangTilPersoner - hvis samme saksbehandler kaller skal den ha cachet`() {
        every { integrasjonService.sjekkTilgangTilPersoner(any()) } returns Tilgang(true)

        mockBrukerContext("A")
        tilgangService.validerTilgangTilPersoner(
            listOf(olaIdent),
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            ""
        )
        tilgangService.validerTilgangTilPersoner(
            listOf(olaIdent),
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            ""
        )
        verify(exactly = 1) {
            integrasjonService.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilPersoner - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        every { integrasjonService.sjekkTilgangTilPersoner(any()) } returns Tilgang(true)

        mockBrukerContext("A")
        tilgangService.validerTilgangTilPersoner(
            listOf(olaIdent),
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            ""
        )
        mockBrukerContext("B")
        tilgangService.validerTilgangTilPersoner(
            listOf(olaIdent),
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            ""
        )

        verify(exactly = 2) {
            integrasjonService.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis samme saksbehandler kaller skal den ha cachet`() {
        every { integrasjonService.sjekkTilgangTilPersoner(any()) } returns Tilgang(true)

        mockBrukerContext("A")

        tilgangService.validerTilgangTilBehandling(
            behandling.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            ""
        )
        tilgangService.validerTilgangTilBehandling(
            behandling.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            ""
        )

        verify(exactly = 1) {
            integrasjonService.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        every { integrasjonService.sjekkTilgangTilPersoner(any()) } returns Tilgang(true)

        mockBrukerContext("A")
        tilgangService.validerTilgangTilBehandling(
            behandling.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            ""
        )
        mockBrukerContext("B")
        tilgangService.validerTilgangTilBehandling(
            behandling.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            ""
        )

        verify(exactly = 2) {
            integrasjonService.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    fun `validerTilgangTilFagsak - skal kaste feil dersom søker eller et eller flere av barna har diskresjonskode og saksbehandler mangler tilgang`() {
        every { fagsakService.hentFagsak(fagsak.id) }.returns(fagsak)
        every { behandlingRepository.finnBehandlinger(fagsak.id) }.returns(listOf(behandling))
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) }.returns(
            PersonopplysningGrunnlag(
                behandlingId = behandling.id,
                personer = mutableSetOf(
                    Person(
                        aktør = Aktør(
                            aktørId = "6543456372112",
                            personidenter = mutableSetOf(
                                Personident(
                                    fødselsnummer = "65434563721",
                                    aktiv = true,
                                    aktør = Aktør("6543456372112", mutableSetOf())
                                )
                            )
                        ),
                        type = PersonType.SØKER,
                        fødselsdato = LocalDate.now(),
                        kjønn = Kjønn.MANN,
                        personopplysningGrunnlag = PersonopplysningGrunnlag(
                            behandlingId = behandling.id,
                            personer = mutableSetOf(),
                            aktiv = true
                        )
                    ),
                    Person(
                        aktør = Aktør(
                            aktørId = "1234567891012",
                            personidenter = mutableSetOf(
                                Personident(
                                    fødselsnummer = "12345678910",
                                    aktiv = true,
                                    aktør = Aktør("1234567891012", mutableSetOf())
                                )
                            )
                        ),
                        type = PersonType.BARN,
                        fødselsdato = LocalDate.now(),
                        kjønn = Kjønn.MANN,
                        personopplysningGrunnlag = PersonopplysningGrunnlag(
                            behandlingId = behandling.id,
                            personer = mutableSetOf(),
                            aktiv = true
                        )
                    )
                )
            )
        )
        every {
            integrasjonService.sjekkTilgangTilPersoner(
                listOf(
                    "65434563721",
                    "12345678910"
                )
            )
        }.returns(
            Tilgang(false, null)
        )
        mockBrukerContext("A")
        assertThrows<RolleTilgangskontrollFeil> {
            tilgangService.validerTilgangTilFagsak(
                fagsak.id,
                AuditLoggerEvent.ACCESS,
                BehandlerRolle.SAKSBEHANDLER,
                ""
            )
        }
    }
}
