package no.nav.familie.ks.sak.kjerne.personident

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.config.PersonInfoQuery
import no.nav.familie.ks.sak.cucumber.mocking.mockTaskService
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.data.tilPersonEnkel
import no.nav.familie.ks.sak.integrasjon.pdl.PdlClient
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlFødselsDato
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlIdent
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonData
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

internal class HåndterNyIdentServiceTest {
    private val aktørRepository: AktørRepository = mockk()
    private val fagsakService: FagsakService = mockk()
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()
    private val pdlClient: PdlClient = mockk()
    private val behandlingService: BehandlingService = mockk()

    @Nested
    inner class OpprettMergeIdentTaskTest {
        private val personIdentService: PersonidentService = mockk()
        private val håndterNyIdentService =
            HåndterNyIdentService(
                aktørIdRepository = aktørRepository,
                fagsakService = fagsakService,
                personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
                pdlClient = pdlClient,
                personIdentService = personIdentService,
                behandlingService = behandlingService,
            )

        val gammelFødselsdato = LocalDate.of(2000, 1, 1)
        val gammeltFnr = randomFnr(gammelFødselsdato)
        val gammelAktør = randomAktør(gammeltFnr)
        val gammelPerson = lagPerson(aktør = gammelAktør, fødselsdato = gammelFødselsdato)

        val nyFødselsdato = LocalDate.of(2000, 2, 2)
        val nyttFnr = randomFnr(nyFødselsdato)
        val nyAktør = randomAktør(nyttFnr)

        val gammelBehandling = lagBehandling()

        val identInformasjonFraPdl =
            listOf(
                PdlIdent(nyAktør.aktørId, false, "AKTORID"),
                PdlIdent(nyttFnr, false, "FOLKEREGISTERIDENT"),
                PdlIdent(gammelAktør.aktørId, true, "AKTORID"),
                PdlIdent(gammeltFnr, true, "FOLKEREGISTERIDENT"),
            )

        @BeforeEach
        fun init() {
            clearMocks(answers = true, firstMock = fagsakService)
            every { personopplysningGrunnlagRepository.finnSøkerOgBarnAktørerTilFagsak(any()) } returns setOf(gammelPerson.tilPersonEnkel())
            every { personIdentService.hentIdenter(any(), true) } returns identInformasjonFraPdl
            every { aktørRepository.findByAktørId(nyAktør.aktørId) } returns null
            every { aktørRepository.findByAktørId(gammelAktør.aktørId) } returns gammelAktør
            every { fagsakService.hentFagsakerPåPerson(any()) } returns listOf(Fagsak(id = 0, aktør = randomAktør()))
            every { behandlingService.hentSisteBehandlingSomErVedtatt(any()) } returns gammelBehandling
            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns
                PersonopplysningGrunnlag(
                    behandlingId = gammelBehandling.id,
                    personer = mutableSetOf(gammelPerson),
                )
            mockkObject(PatchMergetIdentTask)
            every { PatchMergetIdentTask.opprettTask(any()) } returns Task("", "")
        }

        @Test
        fun `håndterNyIdent dropper merging av identer når det ikke eksisterer en fagsak for identer`() {
            // arrange
            every { fagsakService.hentFagsakerPåPerson(any()) } returns emptyList()

            // act
            val aktør = håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))

            // assert
            verify(exactly = 0) { PatchMergetIdentTask.opprettTask(any()) }
            assertThat(aktør).isNull()
        }

        @Test
        fun `håndterNyIdent skal ikke kaste feil når det eksisterer flere fagsaker for identer`() {
            // arrange
            every { fagsakService.hentFagsakerPåPerson(any()) } returns
                listOf(
                    Fagsak(id = 1, aktør = gammelAktør),
                    Fagsak(id = 2, aktør = gammelAktør),
                )
            every { behandlingRepository.finnBehandlinger(any<Long>()) } returns listOf(gammelBehandling)
            every { behandlingRepository.finnAktivtFødselsnummerForBehandlinger(any()) } returns listOf(1L to gammeltFnr)

            val aktør = håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))

            assertThat(aktør).isNull()
            verify(exactly = 1) { PatchMergetIdentTask.opprettTask(any()) }
        }

        @Test
        fun `håndterNyIdent kaster ikke Feil når fødselsdato er endret innenfor samme måned`() {
            // arrange
            every { pdlClient.hentPerson(nyttFnr, PersonInfoQuery.ENKEL) } returns
                PdlPersonData(
                    folkeregisteridentifikator = emptyList(),
                    foedselsdato = listOf(PdlFødselsDato(gammelFødselsdato.sisteDagIMåned().toString())),
                    bostedsadresse = emptyList(),
                )
            every { behandlingRepository.finnBehandlinger(any<Long>()) } returns listOf(gammelBehandling)
            every { behandlingRepository.finnAktivtFødselsnummerForBehandlinger(any()) } returns listOf(1L to gammeltFnr)

            // act & assert
            assertDoesNotThrow {
                håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))
            }
        }

        @Test
        fun `håndterNyIdent kaster ikke Feil når fødselsdato er endret for søker`() {
            // arrange
            every { pdlClient.hentPerson(nyttFnr, PersonInfoQuery.ENKEL) } returns
                PdlPersonData(
                    folkeregisteridentifikator = emptyList(),
                    foedselsdato = listOf(PdlFødselsDato(nyFødselsdato.toString())),
                    bostedsadresse = emptyList(),
                )
            every { fagsakService.hentFagsakerPåPerson(any()) } returns
                listOf(
                    Fagsak(id = 1, aktør = gammelAktør),
                )
            every { behandlingRepository.finnBehandlinger(any<Long>()) } returns listOf(gammelBehandling)
            every { behandlingRepository.finnAktivtFødselsnummerForBehandlinger(any()) } returns listOf(1L to gammeltFnr)

            // act & assert
            assertDoesNotThrow {
                håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))
            }
        }

        @Test
        fun `håndterNyIdent kaster Feil når fødselsdato er endret for identer`() {
            // arrange
            every { pdlClient.hentPerson(nyttFnr, PersonInfoQuery.ENKEL) } returns
                PdlPersonData(
                    folkeregisteridentifikator = emptyList(),
                    foedselsdato = listOf(PdlFødselsDato(nyFødselsdato.toString())),
                    bostedsadresse = emptyList(),
                )

            // act & assert
            val feil =
                assertThrows<Feil> {
                    håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))
                }

            assertThat(feil.message).startsWith("Fødselsdato er forskjellig fra forrige behandling. Må patche ny ident manuelt.")
        }

        @Test
        fun `håndterNyIdent lager en PatchMergetIdent task ved endret fødselsdato, hvis det ikke er en vedtatt behandling`() {
            // arrange
            every { pdlClient.hentPerson(nyttFnr, PersonInfoQuery.ENKEL) } returns
                PdlPersonData(
                    folkeregisteridentifikator = emptyList(),
                    foedselsdato = listOf(PdlFødselsDato(nyFødselsdato.toString())),
                    bostedsadresse = emptyList(),
                )
            every { behandlingRepository.finnBehandlinger(any<Long>()) } returns listOf(gammelBehandling)
            every { behandlingRepository.finnAktivtFødselsnummerForBehandlinger(any()) } returns listOf(1L to gammeltFnr)
            every { behandlingService.hentSisteBehandlingSomErVedtatt(any()) } returns null

            // act & assert
            val aktør = håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))

            assertThat(aktør).isNull()
            verify(exactly = 1) { PatchMergetIdentTask.opprettTask(any()) }
        }

        @Test
        fun `håndterNyIdent lager en PatchMergetIdent task ved endret fødselsdato, hvis aktør ikke er med i forrige vedtatte behandling`() {
            // arrange
            every { pdlClient.hentPerson(any<String>(), PersonInfoQuery.ENKEL) } returns
                PdlPersonData(
                    folkeregisteridentifikator = emptyList(),
                    foedselsdato = listOf(PdlFødselsDato(nyFødselsdato.toString())),
                    bostedsadresse = emptyList(),
                )
            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns PersonopplysningGrunnlag(behandlingId = gammelBehandling.id)
            every { behandlingRepository.finnBehandlinger(any<Long>()) } returns listOf(gammelBehandling)
            every { behandlingRepository.finnAktivtFødselsnummerForBehandlinger(any()) } returns listOf(1L to gammeltFnr)

            // act & assert
            val aktør = håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))

            assertThat(aktør).isNull()
            verify(exactly = 1) { PatchMergetIdentTask.opprettTask(any()) }
        }

        @Test
        fun `håndterNyIdent lager en PatchMergetIdent task hvis fødselsdato er uendret`() {
            // arrange
            every { pdlClient.hentPerson(any<String>(), PersonInfoQuery.ENKEL) } returns
                PdlPersonData(
                    folkeregisteridentifikator = emptyList(),
                    foedselsdato = listOf(PdlFødselsDato(gammelFødselsdato.toString())),
                    bostedsadresse = emptyList(),
                )
            every { behandlingRepository.finnBehandlinger(any<Long>()) } returns listOf(gammelBehandling)
            every { behandlingRepository.finnAktivtFødselsnummerForBehandlinger(any()) } returns listOf(1L to gammeltFnr)

            // act & assert
            val aktør = håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))

            assertThat(aktør).isNull()
            verify(exactly = 1) { PatchMergetIdentTask.opprettTask(any()) }
        }
    }

    @Nested
    inner class HåndterNyIdentTest {
        private val pdlClient: PdlClient = mockk(relaxed = true)
        private val personidentRepository: PersonidentRepository = mockk()

        private val personidentAktiv = randomFnr()
        private val aktørAktiv = randomAktør(personidentAktiv)
        private val personidentHistorisk = randomFnr()

        private val personIdentSlot = slot<Personident>()
        private val aktørSlot = slot<Aktør>()

        private val personidentService =
            PersonidentService(
                personidentRepository = personidentRepository,
                aktørRepository = aktørRepository,
                pdlClient = pdlClient,
                taskService = mockTaskService(),
            )

        private val håndterNyIdentService =
            HåndterNyIdentService(
                aktørIdRepository = aktørRepository,
                fagsakService = fagsakService,
                personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
                pdlClient = pdlClient,
                personIdentService = personidentService,
                behandlingService = behandlingService,
            )

        @BeforeEach
        fun init() {
            clearMocks(answers = true, firstMock = aktørRepository)
            clearMocks(answers = true, firstMock = personidentRepository)

            every { fagsakService.hentFagsakerPåPerson(any()) } returns listOf(Fagsak(id = 0, aktør = aktørAktiv))

            every { personidentRepository.saveAndFlush(capture(personIdentSlot)) } answers {
                personIdentSlot.captured
            }

            every { aktørRepository.saveAndFlush(capture(aktørSlot)) } answers {
                aktørSlot.captured
            }

            every { pdlClient.hentIdenter(personidentAktiv, false) } answers {
                listOf(
                    PdlIdent(aktørAktiv.aktørId, false, "AKTORID"),
                    PdlIdent(personidentAktiv, false, "FOLKEREGISTERIDENT"),
                )
            }
            every { pdlClient.hentIdenter(personidentHistorisk, false) } answers {
                listOf(
                    PdlIdent(aktørAktiv.aktørId, false, "AKTORID"),
                    PdlIdent(personidentAktiv, false, "FOLKEREGISTERIDENT"),
                )
            }
        }

        @Test
        fun `Skal legge til ny ident på aktør som finnes i systemet`() {
            val personIdentSomFinnes = randomFnr()
            val personIdentSomSkalLeggesTil = randomFnr()
            val historiskIdent = randomFnr()
            val historiskAktør = randomAktør(historiskIdent)
            val aktørIdSomFinnes = randomAktør(personIdentSomFinnes)
            val fødselsdato = LocalDate.now().minusYears(4)

            every { pdlClient.hentPerson(personIdentSomSkalLeggesTil, PersonInfoQuery.ENKEL) } returns
                PdlPersonData(
                    folkeregisteridentifikator = emptyList(),
                    foedselsdato = listOf(PdlFødselsDato(fødselsdato.toString())),
                    bostedsadresse = emptyList(),
                )

            every { behandlingService.hentSisteBehandlingSomErVedtatt(any()) } returns lagBehandling()

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns
                PersonopplysningGrunnlag(
                    behandlingId = 1L,
                    personer =
                        mutableSetOf(
                            lagPerson(
                                aktør = aktørIdSomFinnes,
                                fødselsdato = fødselsdato,
                            ),
                        ),
                )

            every { pdlClient.hentIdenter(personIdentSomFinnes, false) } answers {
                listOf(
                    PdlIdent(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                    PdlIdent(personIdentSomFinnes, false, "FOLKEREGISTERIDENT"),
                )
            }

            every { pdlClient.hentIdenter(personIdentSomSkalLeggesTil, true) } answers {
                listOf(
                    PdlIdent(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                    PdlIdent(personIdentSomSkalLeggesTil, false, "FOLKEREGISTERIDENT"),
                    PdlIdent(historiskAktør.aktørId, true, "AKTORID"),
                    PdlIdent(historiskIdent, true, "FOLKEREGISTERIDENT"),
                )
            }

            every { personidentRepository.findByFødselsnummerOrNull(personIdentSomFinnes) }.answers {
                Personident(fødselsnummer = randomFnr(), aktør = aktørIdSomFinnes, aktiv = true)
            }

            every { aktørRepository.findByAktørId(aktørIdSomFinnes.aktørId) }.answers {
                aktørIdSomFinnes
            }

            every { aktørRepository.findByAktørId(historiskAktør.aktørId) }.answers {
                null
            }
            every { personidentRepository.findByFødselsnummerOrNull(personIdentSomSkalLeggesTil) }.answers {
                null
            }

            val aktør = håndterNyIdentService.håndterNyIdent(nyIdent = PersonIdent(personIdentSomSkalLeggesTil))

            assertThat(aktør?.personidenter?.size).isEqualTo(2)
            assertThat(personIdentSomSkalLeggesTil).isEqualTo(aktør!!.aktivFødselsnummer())
            assertThat(
                aktør.personidenter
                    .first { !it.aktiv }
                    .gjelderTil!!
                    .isBefore(LocalDateTime.now()),
            )
            assertThat(
                aktør.personidenter
                    .first { !it.aktiv }
                    .gjelderTil!!
                    .isBefore(LocalDateTime.now()),
            )
            verify(exactly = 2) { aktørRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
        }

        @Test
        fun `Skal kaste feil når vi prøver legge til ny ident på aktør som finnes i systemet og som har endret fødselsdato`() {
            val personIdentSomFinnes = randomFnr()
            val personIdentSomSkalLeggesTil = randomFnr()
            val historiskIdent = randomFnr()
            val historiskAktør = randomAktør(historiskIdent)
            val aktørIdSomFinnes = randomAktør(personIdentSomFinnes)
            val fødselsdato = LocalDate.now().minusYears(4)

            every { pdlClient.hentPerson(personIdentSomSkalLeggesTil, PersonInfoQuery.ENKEL) } returns
                PdlPersonData(
                    folkeregisteridentifikator = emptyList(),
                    foedselsdato = listOf(PdlFødselsDato(fødselsdato.minusMonths(2).toString())),
                    bostedsadresse = emptyList(),
                )

            every { pdlClient.hentPerson(personIdentSomFinnes, PersonInfoQuery.ENKEL) } returns
                PdlPersonData(
                    folkeregisteridentifikator = emptyList(),
                    foedselsdato = listOf(PdlFødselsDato(fødselsdato.minusMonths(2).toString())),
                    bostedsadresse = emptyList(),
                )

            every { behandlingService.hentSisteBehandlingSomErVedtatt(any()) } returns lagBehandling()

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns
                PersonopplysningGrunnlag(
                    behandlingId = 1L,
                    personer =
                        mutableSetOf(
                            lagPerson(
                                aktør = aktørIdSomFinnes,
                                fødselsdato = fødselsdato,
                            ),
                        ),
                )

            every { pdlClient.hentIdenter(personIdentSomSkalLeggesTil, true) } answers {
                listOf(
                    PdlIdent(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                    PdlIdent(personIdentSomSkalLeggesTil, false, "FOLKEREGISTERIDENT"),
                    PdlIdent(historiskAktør.aktørId, true, "AKTORID"),
                    PdlIdent(historiskIdent, true, "FOLKEREGISTERIDENT"),
                )
            }

            every { aktørRepository.findByAktørId(aktørIdSomFinnes.aktørId) }.answers {
                aktørIdSomFinnes
            }

            every { aktørRepository.findByAktørId(historiskAktør.aktørId) }.answers {
                null
            }

            val exception =
                assertThrows<Feil> {
                    håndterNyIdentService.håndterNyIdent(nyIdent = PersonIdent(personIdentSomSkalLeggesTil))
                }

            assertThat(exception.message).startsWith("Fødselsdato er forskjellig fra forrige behandling")
        }

        @Test
        fun `Skal ikke legge til ny ident på aktør som allerede har denne identen registert i systemet`() {
            val personIdentSomFinnes = randomFnr()
            val aktørIdSomFinnes = randomAktør(personIdentSomFinnes)

            every { pdlClient.hentIdenter(personIdentSomFinnes, true) } answers {
                listOf(
                    PdlIdent(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                    PdlIdent(personIdentSomFinnes, false, "FOLKEREGISTERIDENT"),
                )
            }

            every { aktørRepository.findByAktørId(aktørIdSomFinnes.aktørId) }.answers { aktørIdSomFinnes }
            every { personidentRepository.findByFødselsnummerOrNull(personIdentSomFinnes) }.answers {
                randomAktør(
                    personIdentSomFinnes,
                ).personidenter.first()
            }

            val aktør = håndterNyIdentService.håndterNyIdent(nyIdent = PersonIdent(personIdentSomFinnes))

            assertThat(aktørIdSomFinnes.aktørId).isEqualTo(aktør?.aktørId)
            assertThat(aktør?.personidenter?.size).isEqualTo(1)
            assertThat(personIdentSomFinnes).isEqualTo(aktør?.personidenter?.single()?.fødselsnummer)
            verify(exactly = 0) { aktørRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
        }

        @Test
        fun `Hendelse på en ident hvor gammel ident1 er merget med ny ident2 skal ikke kaste feil når bruker har alt bruker ny ident`() {
            val fnrIdent1 = randomFnr()
            val aktørIdent1 = randomAktør(fnrIdent1)
            val aktivFnrIdent2 = randomFnr()
            val aktivAktørIdent2 = randomAktør(aktivFnrIdent2)

            every { pdlClient.hentIdenter(aktivFnrIdent2, true) } answers {
                listOf(
                    PdlIdent(aktivAktørIdent2.aktørId, false, "AKTORID"),
                    PdlIdent(aktivFnrIdent2, false, "FOLKEREGISTERIDENT"),
                    PdlIdent(aktørIdent1.aktørId, true, "AKTORID"),
                    PdlIdent(fnrIdent1, true, "FOLKEREGISTERIDENT"),
                )
            }

            every { aktørRepository.findByAktørId(aktivAktørIdent2.aktørId) }.answers {
                aktivAktørIdent2
            }
            every { aktørRepository.findByAktørId(aktørIdent1.aktørId) }.answers {
                null
            }

            val aktør = håndterNyIdentService.håndterNyIdent(nyIdent = PersonIdent(aktivFnrIdent2))
            assertThat(aktivAktørIdent2.aktørId).isEqualTo(aktør?.aktørId)
            assertThat(aktør?.personidenter?.size).isEqualTo(1)
            assertThat(aktivFnrIdent2).isEqualTo(aktør?.personidenter?.single()?.fødselsnummer)
            verify(exactly = 0) { aktørRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
        }
    }
}
