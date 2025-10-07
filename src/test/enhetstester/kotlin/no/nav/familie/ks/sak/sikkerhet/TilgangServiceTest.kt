package no.nav.familie.ks.sak.sikkerhet

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.RolleConfig
import no.nav.familie.ks.sak.data.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ks.sak.data.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.Personident
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.log.mdc.MDCConstants
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.slf4j.MDC
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import java.time.LocalDate

class TilgangServiceTest {
    private val mockIntegrasjonService = mockk<IntegrasjonService>()
    private val mockBehandlingRepository = mockk<BehandlingRepository>()
    private val mockFagsakRepository = mockk<FagsakRepository>()
    private val personidentService = mockk<PersonidentService>()
    private val mockPersonopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()

    private val cacheManager = ConcurrentMapCacheManager()

    private val rolleConfig =
        RolleConfig(
            BehandlerRolle.BESLUTTER.name,
            BehandlerRolle.SAKSBEHANDLER.name,
            BehandlerRolle.VEILEDER.name,
            BehandlerRolle.FORVALTER.name,
            KODE6 = "kode6",
            KODE7 = "kode7",
        )
    private val auditLogger = AuditLogger("familie-ks-sak")
    private val tilgangService =
        TilgangService(
            integrasjonService = mockIntegrasjonService,
            behandlingRepository = mockBehandlingRepository,
            personopplysningGrunnlagRepository = mockPersonopplysningGrunnlagRepository,
            rolleConfig = rolleConfig,
            cacheManager = cacheManager,
            auditLogger = auditLogger,
            personidentService = personidentService,
            fagsakRepository = mockFagsakRepository,
        )

    private val fagsak = lagFagsak()
    private val behandling = lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val aktør = fagsak.aktør
    private val personopplysningGrunnlag =
        lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = aktør.aktivFødselsnummer(),
            barnasIdenter = emptyList(),
        )
    private val defaultHandling = "gjøre handling"

    @BeforeEach
    internal fun beforeEach() {
        clearAllMocks()
        clearCache(cacheManager)
        MDC.put(MDCConstants.MDC_CALL_ID, "00001111")
        mockBrukerContext(groups = listOf(BehandlerRolle.SAKSBEHANDLER.name))
        every { mockFagsakRepository.finnFagsak(fagsak.id) } returns fagsak
        every { mockBehandlingRepository.hentBehandling(any()) } returns behandling
        every { mockBehandlingRepository.finnBehandlinger(fagsak.id) } returns listOf(behandling)
        every { mockPersonopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns personopplysningGrunnlag
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    private fun clearCache(cacheManager: CacheManager) =
        cacheManager.cacheNames.parallelStream().forEach {
            cacheManager.getCache(it)?.clear()
        }

    @ParameterizedTest
    @EnumSource(value = BehandlerRolle::class, names = ["SAKSBEHANDLER", "VEILEDER"])
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler eller veileder forsøker å gjøre handling som krever BESLUTTER-rolle`(
        behandlerRolle: BehandlerRolle,
    ) {
        mockBrukerContext(groups = listOf(behandlerRolle.name))

        val rolleTilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilHandling(BehandlerRolle.BESLUTTER, defaultHandling)
            }
        assertEquals(
            "A med rolle $behandlerRolle har ikke tilgang til å $defaultHandling. Krever ${BehandlerRolle.BESLUTTER}.",
            rolleTilgangskontrollFeil.melding,
        )
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom veileder forsøker å gjøre handling som krever SAKSBEHANDLER-rolle`() {
        mockBrukerContext(groups = listOf(BehandlerRolle.VEILEDER.name))

        val rolleTilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilHandling(BehandlerRolle.SAKSBEHANDLER, defaultHandling)
            }
        assertEquals(
            "A med rolle ${BehandlerRolle.VEILEDER} har ikke tilgang til å $defaultHandling. Krever ${BehandlerRolle.SAKSBEHANDLER}.",
            rolleTilgangskontrollFeil.melding,
        )
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom bruker verken er veileder saksbehandler eller beslutter`() {
        mockBrukerContext(groups = emptyList())

        val rolleTilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilHandling(BehandlerRolle.VEILEDER, defaultHandling)
            }
        assertEquals(
            "A med rolle UKJENT har ikke tilgang til å $defaultHandling. Krever ${BehandlerRolle.VEILEDER}.",
            rolleTilgangskontrollFeil.melding,
        )
    }

    @Test
    internal fun `skal ikke kaste RolleTilgangskontrollFeil dersom beslutter beslutter forsøker å gjøre handling som krever BESLUTTER-rolle`() {
        mockBrukerContext(groups = listOf(BehandlerRolle.BESLUTTER.name))
        tilgangService.validerTilgangTilHandling(BehandlerRolle.BESLUTTER, "")
    }

    @ParameterizedTest
    @EnumSource(value = BehandlerRolle::class, names = ["SAKSBEHANDLER", "BESLUTTER"])
    internal fun `skal ikke kaste RolleTilgangskontrollFeil dersom saksbehandler eller beslutter forsøker å gjøre handling som krever SAKSBEHANDLER-rolle`(
        behandlerRolle: BehandlerRolle,
    ) {
        mockBrukerContext(groups = listOf(behandlerRolle.name))
        tilgangService.validerTilgangTilHandling(BehandlerRolle.SAKSBEHANDLER, "")
    }

    @ParameterizedTest
    @EnumSource(value = BehandlerRolle::class, names = ["BESLUTTER", "SAKSBEHANDLER", "VEILEDER"])
    internal fun `skal ikke kaste RolleTilgangskontrollFeil dersom saksbehandler beslutter eller veileder forsøker å gjøre handling som krever VEILEDER-rolle`(
        behandlerRolle: BehandlerRolle,
    ) {
        mockBrukerContext(groups = listOf(behandlerRolle.name))
        tilgangService.validerTilgangTilHandling(BehandlerRolle.VEILEDER, "")
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til person`() {
        every { mockIntegrasjonService.sjekkTilgangTilPersoner(any()) } returns
            listOf(
                Tilgang(aktør.aktivFødselsnummer(), false, "Bruker mangler rolle 'TEST_ROLLE'"),
            )

        val personIdenter = listOf(aktør.aktivFødselsnummer())

        val rolleTilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilHandlingOgPersoner(
                    personIdenter,
                    AuditLoggerEvent.ACCESS,
                    BehandlerRolle.SAKSBEHANDLER,
                    "",
                )
            }
        assertEquals(
            "Saksbehandler A har ikke tilgang til å behandle $personIdenter. Bruker mangler rolle 'TEST_ROLLE'.",
            rolleTilgangskontrollFeil.melding,
        )
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til person`() {
        every { mockIntegrasjonService.sjekkTilgangTilPersoner(any()) } returns
            listOf(
                Tilgang(aktør.aktivFødselsnummer(), true),
            )

        tilgangService.validerTilgangTilHandlingOgPersoner(
            listOf(aktør.aktivFødselsnummer()),
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "",
        )
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til behandling`() {
        every { mockIntegrasjonService.sjekkTilgangTilPersoner(any()) } returns
            listOf(
                Tilgang(aktør.aktivFødselsnummer(), false, "Bruker mangler rolle 'TEST_ROLLE'"),
            )

        val rolleTilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
                    behandling.id,
                    AuditLoggerEvent.ACCESS,
                    BehandlerRolle.SAKSBEHANDLER,
                    "hente behandling",
                )
            }
        assertEquals(
            "Saksbehandler A har ikke tilgang til fagsak=${fagsak.id}. Bruker mangler rolle 'TEST_ROLLE'.",
            rolleTilgangskontrollFeil.melding,
        )
        assertEquals(
            "Fagsaken inneholder personer som krever ytterligere tilganger. Bruker mangler rolle 'TEST_ROLLE'.",
            rolleTilgangskontrollFeil.frontendFeilmelding,
        )
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til behandling`() {
        every { mockIntegrasjonService.sjekkTilgangTilPersoner(any()) } returns
            listOf(
                Tilgang(aktør.aktivFødselsnummer(), true),
            )

        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandling.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "hente behandling",
        )
    }

    @Test
    internal fun `validerTilgangTilPersoner - hvis samme saksbehandler kaller skal den ha cachet resultatet`() {
        every { mockIntegrasjonService.sjekkTilgangTilPersoner(any()) } returns
            listOf(
                Tilgang(aktør.aktivFødselsnummer(), true),
            )

        mockBrukerContext("A", groups = listOf(BehandlerRolle.SAKSBEHANDLER.name))
        val ident = "12345678910"

        tilgangService.validerTilgangTilHandlingOgPersoner(
            listOf(ident),
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "",
        )
        tilgangService.validerTilgangTilHandlingOgPersoner(
            listOf(ident),
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "",
        )
        verify(exactly = 1) {
            mockIntegrasjonService.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilPersoner - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        every { mockIntegrasjonService.sjekkTilgangTilPersoner(any()) } returns
            listOf(
                Tilgang(aktør.aktivFødselsnummer(), true),
            )

        val roller = listOf(BehandlerRolle.SAKSBEHANDLER.name)

        mockBrukerContext("A", roller)
        val ident = "12345678910"

        tilgangService.validerTilgangTilHandlingOgPersoner(
            listOf(ident),
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "",
        )
        mockBrukerContext("B", roller)
        tilgangService.validerTilgangTilHandlingOgPersoner(
            listOf(ident),
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "",
        )

        verify(exactly = 2) {
            mockIntegrasjonService.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis samme saksbehandler kaller skal den ha cachet resultatet`() {
        every { mockIntegrasjonService.sjekkTilgangTilPersoner(any()) } returns
            listOf(
                Tilgang(aktør.aktivFødselsnummer(), true),
            )

        mockBrukerContext("A", listOf(BehandlerRolle.SAKSBEHANDLER.name))

        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandling.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "",
        )
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandling.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "",
        )

        verify(exactly = 1) {
            mockIntegrasjonService.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        every { mockIntegrasjonService.sjekkTilgangTilPersoner(any()) } returns
            listOf(
                Tilgang(aktør.aktivFødselsnummer(), true),
            )

        val roller = listOf(BehandlerRolle.SAKSBEHANDLER.name)

        mockBrukerContext("A", roller)
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandling.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "",
        )
        mockBrukerContext("B", roller)
        tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
            behandling.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "",
        )

        verify(exactly = 2) {
            mockIntegrasjonService.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til fagsak`() {
        every { mockIntegrasjonService.sjekkTilgangTilPersoner(any()) } returns
            listOf(
                Tilgang(aktør.aktivFødselsnummer(), false, "Bruker mangler rolle 'TEST_ROLLE'"),
            )

        val rolleTilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilHandlingOgFagsak(
                    fagsak.id,
                    AuditLoggerEvent.ACCESS,
                    BehandlerRolle.SAKSBEHANDLER,
                    "hente behandling",
                )
            }
        assertEquals("Saksbehandler A har ikke tilgang til fagsak=${fagsak.id}. Bruker mangler rolle 'TEST_ROLLE'.", rolleTilgangskontrollFeil.melding)
        assertEquals(
            "Fagsaken inneholder personer som krever ytterligere tilganger. Bruker mangler rolle 'TEST_ROLLE'.",
            rolleTilgangskontrollFeil.frontendFeilmelding,
        )
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til fagsak`() {
        every { mockIntegrasjonService.sjekkTilgangTilPersoner(any()) } returns
            listOf(
                Tilgang(aktør.aktivFødselsnummer(), true),
            )

        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsak.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "hente behandling",
        )
    }

    @Test
    internal fun `skal ikke feile når fagsak ikke eksisterer`() {
        every { mockFagsakRepository.finnFagsak(fagsak.id) } returns null
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsak.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "hente behandling",
        )
        verify(exactly = 0) { mockBehandlingRepository.finnBehandlinger(fagsak.id) }
    }

    @Test
    internal fun `validerTilgangTilFagsak - hvis samme saksbehandler kaller skal den ha cachet resultatet`() {
        every { mockIntegrasjonService.sjekkTilgangTilPersoner(any()) } returns
            listOf(
                Tilgang(aktør.aktivFødselsnummer(), true),
            )

        mockBrukerContext("A", listOf(BehandlerRolle.SAKSBEHANDLER.name))

        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsak.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "",
        )
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsak.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "",
        )

        verify(exactly = 1) {
            mockIntegrasjonService.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilFagsak - hvis to ulike saksbehandlere kaller skal den sjekke tilgang på nytt`() {
        every { mockIntegrasjonService.sjekkTilgangTilPersoner(any()) } returns
            listOf(
                Tilgang(aktør.aktivFødselsnummer(), true),
            )

        val roller = listOf(BehandlerRolle.SAKSBEHANDLER.name)

        mockBrukerContext("A", roller)
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsak.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "",
        )
        mockBrukerContext("B", roller)
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsak.id,
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "",
        )

        verify(exactly = 2) {
            mockIntegrasjonService.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilFagsakForPerson - skal kaste RolleTilgangskontrollFeil dersom saksbehandler ikke har tilgang til fagsak`() {
        val aktør = randomAktør("12345678910")
        every { mockIntegrasjonService.sjekkTilgangTilPersoner(any()) } returns
            listOf(
                Tilgang(aktør.aktivFødselsnummer(), false, "Bruker mangler rolle 'TEST_ROLLE'"),
            )
        every { personidentService.hentOgLagreAktør("12345678910", any()) } returns aktør
        every { mockFagsakRepository.finnFagsakForAktør(aktør) } returns fagsak

        val rolleTilgangskontrollFeil =
            assertThrows<RolleTilgangskontrollFeil> {
                tilgangService.validerTilgangTilHandlingOgFagsakForPerson(
                    "12345678910",
                    AuditLoggerEvent.ACCESS,
                    BehandlerRolle.SAKSBEHANDLER,
                    "hente behandling",
                )
            }
        assertEquals("Saksbehandler A har ikke tilgang til fagsak=${fagsak.id}. Bruker mangler rolle 'TEST_ROLLE'.", rolleTilgangskontrollFeil.melding)
        assertEquals(
            "Fagsaken inneholder personer som krever ytterligere tilganger. Bruker mangler rolle 'TEST_ROLLE'.",
            rolleTilgangskontrollFeil.frontendFeilmelding,
        )
    }

    @Test
    internal fun `validerTilgangTilFagsakForPerson - skal ikke feile når saksbehandler har tilgang til fagsak`() {
        val aktør = randomAktør("12345678910")
        every { mockIntegrasjonService.sjekkTilgangTilPersoner(any()) } returns
            listOf(
                Tilgang(aktør.aktivFødselsnummer(), true),
            )
        every { personidentService.hentOgLagreAktør("12345678910", any()) } returns aktør
        every { mockFagsakRepository.finnFagsakForAktør(aktør) } returns fagsak

        tilgangService.validerTilgangTilHandlingOgFagsakForPerson(
            "12345678910",
            AuditLoggerEvent.ACCESS,
            BehandlerRolle.SAKSBEHANDLER,
            "hente behandling",
        )
    }

    @Test
    fun `validerTilgangTilFagsak - skal kaste feil dersom søker eller et eller flere av barna har diskresjonskode og saksbehandler mangler tilgang`() {
        every { mockBehandlingRepository.finnBehandlinger(fagsak.id) }.returns(listOf(behandling))
        every { mockPersonopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) }.returns(
            PersonopplysningGrunnlag(
                behandlingId = behandling.id,
                personer =
                    mutableSetOf(
                        Person(
                            aktør =
                                Aktør(
                                    aktørId = "6543456372112",
                                    personidenter =
                                        mutableSetOf(
                                            Personident(
                                                fødselsnummer = "65434563721",
                                                aktiv = true,
                                                aktør = Aktør("6543456372112", mutableSetOf()),
                                            ),
                                        ),
                                ),
                            type = PersonType.SØKER,
                            fødselsdato = LocalDate.now(),
                            kjønn = Kjønn.MANN,
                            personopplysningGrunnlag =
                                PersonopplysningGrunnlag(
                                    behandlingId = behandling.id,
                                    personer = mutableSetOf(),
                                    aktiv = true,
                                ),
                        ),
                        Person(
                            aktør =
                                Aktør(
                                    aktørId = "1234567891012",
                                    personidenter =
                                        mutableSetOf(
                                            Personident(
                                                fødselsnummer = "12345678910",
                                                aktiv = true,
                                                aktør = Aktør("1234567891012", mutableSetOf()),
                                            ),
                                        ),
                                ),
                            type = PersonType.BARN,
                            fødselsdato = LocalDate.now(),
                            kjønn = Kjønn.MANN,
                            personopplysningGrunnlag =
                                PersonopplysningGrunnlag(
                                    behandlingId = behandling.id,
                                    personer = mutableSetOf(),
                                    aktiv = true,
                                ),
                        ),
                    ),
            ),
        )
        every {
            mockIntegrasjonService.sjekkTilgangTilPersoner(
                listOf(
                    "65434563721",
                    "12345678910",
                ),
            )
        } returns
            listOf(
                Tilgang("65434563721", false),
                Tilgang("12345678910", false),
            )

        assertThrows<RolleTilgangskontrollFeil> {
            tilgangService.validerTilgangTilHandlingOgFagsak(
                fagsak.id,
                AuditLoggerEvent.ACCESS,
                BehandlerRolle.SAKSBEHANDLER,
                "",
            )
        }
    }
}
