package no.nav.familie.ks.sak.kjerne.fagsak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerResponsDto
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerRolle
import no.nav.familie.ks.sak.api.dto.FagsakRequestDto
import no.nav.familie.ks.sak.common.ClockProvider
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class FagsakServiceTest {
    private val personidentService = mockk<PersonidentService>()
    private val integrasjonService = mockk<IntegrasjonService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val fagsakRepository = mockk<FagsakRepository>()
    private val personRepository = mockk<PersonRepository>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService = mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()
    private val taskService = mockk<TaskService>()
    private val vedtakRepository = mockk<VedtakRepository>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val clockProvider = mockk<ClockProvider>()
    private val adopsjonService = mockk<AdopsjonService>()
    private val integrasjonClient = mockk<IntegrasjonClient>(relaxed = true)

    private val fagsakService =
        FagsakService(
            personidentService = personidentService,
            integrasjonService = integrasjonService,
            personopplysningerService = personopplysningerService,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            fagsakRepository = fagsakRepository,
            personRepository = personRepository,
            behandlingRepository = behandlingRepository,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            taskService = taskService,
            vedtakRepository = vedtakRepository,
            andelerTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            clockProvider = clockProvider,
            adopsjonService = adopsjonService,
            integrasjonClient = integrasjonClient,
        )

    @BeforeEach
    fun setup() {
        every { adopsjonService.hentAlleAdopsjonerForBehandling(any()) } returns emptyList()
    }

    @Nested
    inner class HentFagsakDeltagere {
        @Test
        fun `Skal returnere maskert deltaker dersom saksbehandler ikke har tilgang til aktør med bestemt personident`() {
            every { personidentService.hentAktør(any()) } returns randomAktør()
            every { integrasjonService.sjekkTilgangTilPerson(any()) } returns Tilgang("test", false)
            every { personopplysningerService.hentAdressebeskyttelseSomSystembruker(any()) } returns ADRESSEBESKYTTELSEGRADERING.FORTROLIG

            val fagsakdeltakere = fagsakService.hentFagsakDeltagere(randomFnr())
            assertEquals(1, fagsakdeltakere.size)
            assertEquals(ADRESSEBESKYTTELSEGRADERING.FORTROLIG, fagsakdeltakere.first().adressebeskyttelseGradering)
        }

        @Test
        fun `Skal returnere søker dersom metode kalles med søkers ident og saksbehandler har tilgang til identen`() {
            val søkersFødselsdato = LocalDate.of(1985, 5, 1)
            val søkerPersonident = "01058512345"
            val søkerAktør = randomAktør(søkerPersonident)

            val barnPersonident = "01052212345"
            val barnAktør = randomAktør(barnPersonident)

            val barnIdenter = listOf(barnPersonident)

            every { personidentService.hentAktør(any()) } returns søkerAktør
            every { integrasjonService.sjekkTilgangTilPerson(any()) } returns Tilgang("test", true)
            every { personopplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(any()) } returns
                PdlPersonInfo(
                    søkersFødselsdato,
                    forelderBarnRelasjoner = setOf(ForelderBarnRelasjonInfo(barnAktør, FORELDERBARNRELASJONROLLE.BARN)),
                )
            every { personRepository.findByAktør(any()) } returns
                listOf(
                    Person(
                        aktør = søkerAktør,
                        type = PersonType.SØKER,
                        fødselsdato = søkersFødselsdato,
                        kjønn = Kjønn.MANN,
                        personopplysningGrunnlag = lagPersonopplysningGrunnlag(1, søkerPersonident, barnIdenter),
                    ),
                )
            val fagsak = lagFagsak(søkerAktør)
            every { behandlingRepository.hentBehandling(any()) } returns
                lagBehandling(
                    fagsak = fagsak,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                )
            every { fagsakRepository.finnFagsakForAktør(any()) } returns fagsak

            val fagsakdeltakere = fagsakService.hentFagsakDeltagere(søkerPersonident)
            assertEquals(1, fagsakdeltakere.size)
            assertEquals(søkerPersonident, fagsakdeltakere.single().ident)
        }

        @Test
        fun `Skal returnere barn og forelder dersom metode kalles med barne-ident og saksbehandler har tilgang til barnet og forelderen`() {
            val søkersFødselsdato = LocalDate.of(1985, 5, 1)
            val søkerPersonident = "01058512345"
            val søkerAktør = randomAktør(søkerPersonident)

            val barnFødselsdato = LocalDate.of(2022, 5, 1)
            val barnPersonident = "01052212345"
            val barnAktør = randomAktør(barnPersonident)

            every { personidentService.hentAktør(any()) } returns barnAktør
            every { integrasjonService.sjekkTilgangTilPerson(any()) } returns Tilgang("test", true)
            every { personopplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(any()) } returns
                PdlPersonInfo(
                    barnFødselsdato,
                    forelderBarnRelasjoner = setOf(ForelderBarnRelasjonInfo(søkerAktør, FORELDERBARNRELASJONROLLE.FAR)),
                )
            every { personRepository.findByAktør(any()) } returns
                listOf(
                    Person(
                        aktør = barnAktør,
                        type = PersonType.BARN,
                        fødselsdato = barnFødselsdato,
                        kjønn = Kjønn.MANN,
                        personopplysningGrunnlag = lagPersonopplysningGrunnlag(1, barnPersonident, emptyList()),
                    ),
                )
            val fagsak = lagFagsak(søkerAktør)
            every { behandlingRepository.hentBehandling(any()) } returns
                lagBehandling(
                    fagsak = fagsak,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                )
            every { personopplysningerService.hentPersoninfoEnkel(any()) } returns
                PdlPersonInfo(
                    søkersFødselsdato,
                    forelderBarnRelasjoner =
                        setOf(
                            ForelderBarnRelasjonInfo(barnAktør, FORELDERBARNRELASJONROLLE.BARN),
                        ),
                )
            every { fagsakRepository.finnFagsakForAktør(any()) } returns fagsak

            val fagsakdeltakere = fagsakService.hentFagsakDeltagere(søkerPersonident)
            assertEquals(2, fagsakdeltakere.size)

            val barnDeltaker = fagsakdeltakere.find { it.rolle == FagsakDeltagerRolle.BARN }
            val forelderDeltaker = fagsakdeltakere.find { it.rolle == FagsakDeltagerRolle.FORELDER }

            assertEquals(barnPersonident, barnDeltaker?.ident)
            assertEquals(søkerPersonident, forelderDeltaker?.ident)
        }

        @Test
        fun `Setter korrekt egen ansatt status basert på respons fra integrasjoner`() {
            // Arrange
            val erEgenAnsattIdent = randomFnr()
            val erIkkeEgenAnsattIdent = randomFnr()
            val manglerDataIdent = randomFnr()

            val fagsakDeltagere =
                mutableListOf(
                    FagsakDeltagerResponsDto(
                        ident = erEgenAnsattIdent,
                        rolle = FagsakDeltagerRolle.FORELDER,
                    ),
                    FagsakDeltagerResponsDto(
                        ident = erIkkeEgenAnsattIdent,
                        rolle = FagsakDeltagerRolle.BARN,
                    ),
                    FagsakDeltagerResponsDto(
                        ident = manglerDataIdent,
                        rolle = FagsakDeltagerRolle.FORELDER,
                    ),
                )

            every {
                integrasjonClient.sjekkErEgenAnsattBulk(
                    match { it.containsAll(listOf(erEgenAnsattIdent, erIkkeEgenAnsattIdent, manglerDataIdent)) },
                )
            } answers {
                mapOf(
                    erEgenAnsattIdent to true,
                    erIkkeEgenAnsattIdent to false,
                )
            }

            // Act
            val fagsakDeltagereMedEgenAnsattStatus = fagsakService.settEgenAnsattStatusPåFagsakDeltagere(fagsakDeltagere)

            // Assert
            assertThat(fagsakDeltagereMedEgenAnsattStatus.map { it.ident to it.erEgenAnsatt })
                .containsExactlyInAnyOrder(
                    erEgenAnsattIdent to true,
                    erIkkeEgenAnsattIdent to false,
                    manglerDataIdent to null,
                )
        }
    }

    @Nested
    inner class HentEllerOpprettFagsak {
        @Test
        fun `Skal returnere eksisterende fagsak når forespurt personIdent eller aktørId har fagsak i db`() {
            val fødselsnummer = randomFnr()
            val aktør = randomAktør(fødselsnummer)
            val fagsak = lagFagsak(aktør)

            every { personidentService.hentOgLagreAktør(aktør.aktørId, true) } returns aktør
            every { personidentService.hentOgLagreAktør(fødselsnummer, true) } returns aktør
            every { fagsakRepository.finnFagsakForAktør(aktør) } returns fagsak
            every { behandlingRepository.findByFagsakAndAktiv(fagsak.id) } returns
                lagBehandling(
                    fagsak,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                )
            every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any()) } returns emptyList()
            every { personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(any()) } returns lagPersonopplysningGrunnlag()
            every { behandlingRepository.finnBehandlinger(fagsak.id) } returns emptyList()

            var minimalFagsak =
                fagsakService.hentEllerOpprettFagsak(FagsakRequestDto(personIdent = null, aktørId = aktør.aktørId))

            assertEquals(fagsak.id, minimalFagsak.id)
            assertEquals(fagsak.aktør.aktivFødselsnummer(), minimalFagsak.søkerFødselsnummer)

            minimalFagsak =
                fagsakService.hentEllerOpprettFagsak(FagsakRequestDto(personIdent = fødselsnummer))

            assertEquals(fagsak.id, minimalFagsak.id)
            assertEquals(fagsak.aktør.aktivFødselsnummer(), minimalFagsak.søkerFødselsnummer)
        }

        @Test
        fun `Skal returnere eksisterende fagsak med behandlinger når forespurt personIdent eller aktørId har fagsak i db`() {
            val fødselsnummer = randomFnr()
            val aktør = randomAktør(fødselsnummer)
            val fagsak = lagFagsak(aktør)

            val behandling =
                lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD).apply { aktiv = true }

            every { personidentService.hentOgLagreAktør(aktør.aktørId, true) } returns aktør
            every { personidentService.hentOgLagreAktør(fødselsnummer, true) } returns aktør
            every { fagsakRepository.finnFagsakForAktør(aktør) } returns fagsak
            every { behandlingRepository.findByFagsakAndAktiv(fagsak.id) } returns
                lagBehandling(
                    fagsak,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                )
            every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any()) } returns emptyList()
            every { personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(any()) } returns lagPersonopplysningGrunnlag()
            every { behandlingRepository.finnBehandlinger(fagsak.id) } returns listOf(behandling)
            every { vedtakRepository.findByBehandlingAndAktivOptional(any()) } returns mockk(relaxed = true)

            var minimalFagsak =
                fagsakService.hentEllerOpprettFagsak(FagsakRequestDto(personIdent = null, aktørId = aktør.aktørId))

            assertEquals(fagsak.id, minimalFagsak.id)
            assertEquals(fagsak.aktør.aktivFødselsnummer(), minimalFagsak.søkerFødselsnummer)

            minimalFagsak =
                fagsakService.hentEllerOpprettFagsak(FagsakRequestDto(personIdent = fødselsnummer))

            assertEquals(fagsak.id, minimalFagsak.id)
            assertEquals(fagsak.aktør.aktivFødselsnummer(), minimalFagsak.søkerFødselsnummer)
            assertEquals(minimalFagsak.behandlinger.size, 1)
            assertEquals(behandling.id, minimalFagsak.behandlinger[0].behandlingId)
        }

        @Test
        fun `Skal returnere ny fagsak når forespurt personIdent eller aktørId ikke har fagsak i db`() {
            val fødselsnummer = randomFnr()
            val aktør = randomAktør(fødselsnummer)
            val fagsak = lagFagsak(aktør)

            every { personidentService.hentOgLagreAktør(aktør.aktørId, true) } returns aktør
            every { personidentService.hentOgLagreAktør(fødselsnummer, true) } returns aktør
            every { fagsakRepository.finnFagsakForAktør(aktør) } returns null
            every { fagsakRepository.save(fagsak) } returns fagsak
            every { behandlingRepository.findByFagsakAndAktiv(fagsak.id) } returns null
            every { taskService.save(any()) } returns mockk()
            every { behandlingRepository.finnBehandlinger(fagsak.id) } returns emptyList()

            var minimalFagsak =
                fagsakService.hentEllerOpprettFagsak(FagsakRequestDto(personIdent = null, aktørId = aktør.aktørId))

            assertEquals(fagsak.id, minimalFagsak.id)
            assertEquals(fagsak.aktør.aktivFødselsnummer(), minimalFagsak.søkerFødselsnummer)

            minimalFagsak =
                fagsakService.hentEllerOpprettFagsak(FagsakRequestDto(personIdent = fødselsnummer))

            assertEquals(fagsak.id, minimalFagsak.id)
            assertEquals(fagsak.aktør.aktivFødselsnummer(), minimalFagsak.søkerFødselsnummer)
        }

        @Test
        fun `Skal kaste Feil dersom verken personident eller aktørId er satt i FagsakRequestDto`() {
            val feil =
                assertThrows<Feil> {
                    fagsakService.hentEllerOpprettFagsak(
                        FagsakRequestDto(
                            personIdent = null,
                            aktørId = null,
                        ),
                    )
                }

            assertEquals(
                "Hverken aktørid eller personident er satt på fagsak-requesten. Klarer ikke opprette eller hente fagsak.",
                feil.message,
            )
            assertEquals(
                "Fagsak er forsøkt opprettet uten ident. Dette er en systemfeil, vennligst ta kontakt med systemansvarlig.",
                feil.frontendFeilmelding,
            )
        }
    }

    @Nested
    inner class HentMinimalFagsak {
        @Test
        fun `Skal returnere fagsak med tilhørende behandlinger når forespurt fagsakId finnes i db`() {
            val fagsak = lagFagsak(randomAktør())
            val barnehagelisteBehandling =
                lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.BARNEHAGELISTE).apply { aktiv = true }

            every { fagsakRepository.finnFagsak(any()) } returns fagsak
            every { behandlingRepository.finnBehandlinger(fagsak.id) } returns
                listOf(
                    lagBehandling(
                        fagsak,
                        opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                    ).apply { aktiv = false },
                    barnehagelisteBehandling,
                )
            every { behandlingRepository.findByFagsakAndAktiv(fagsak.id) } returns barnehagelisteBehandling
            every { vedtakRepository.findByBehandlingAndAktivOptional(any()) } returns mockk(relaxed = true)
            val fagsakResponse = fagsakService.hentMinimalFagsak(fagsak.id)

            assertEquals(fagsak.id, fagsakResponse.id)
            assertEquals(2, fagsakResponse.behandlinger.size)
        }

        @Test
        fun `Skal kaste FunksjonellFeil dersom fagsak med fagsakId ikke finnes i db`() {
            every { fagsakRepository.finnFagsak(any()) } returns null

            val funksjonellFeil = assertThrows<FunksjonellFeil> { fagsakService.hentMinimalFagsak(404L) }

            assertEquals("Finner ikke fagsak med id 404", funksjonellFeil.message)
        }
    }

    @Nested
    inner class HentFagsak {
        @Test
        fun `Skal returnere fagsak når det finnes en fagsak med forespurt fagsakId i db`() {
            val fagsak = lagFagsak(randomAktør())
            every { fagsakRepository.finnFagsak(any()) } returns fagsak

            val hentetFagsak = fagsakService.hentFagsak(fagsak.id)

            assertEquals(fagsak.id, hentetFagsak.id)
        }

        @Test
        fun `Skal kaste Funksjonell feil dersom fagsak med fagsakId ikke finnes i db`() {
            every { fagsakRepository.finnFagsak(any()) } returns null

            val funksjonellFeil = assertThrows<FunksjonellFeil> { fagsakService.hentFagsak(404L) }

            assertEquals("Finner ikke fagsak med id 404", funksjonellFeil.message)
        }
    }

    @Nested
    inner class HentFagsakForPeson {
        @Test
        fun `Skal returnere fagsak dersom fagsak tilknyttet aktør med forespurt personident finnes i db`() {
            val aktør = randomAktør()
            val fagsak = lagFagsak(aktør)

            every { personidentService.hentOgLagreAktør(any(), any()) } returns aktør
            every { fagsakRepository.finnFagsakForAktør(any()) } returns fagsak

            val fagsakForPerson = fagsakService.hentFagsakForPerson(aktør)

            assertEquals(fagsak.id, fagsakForPerson.id)
            assertEquals(fagsak.aktør, fagsakForPerson.aktør)
        }

        @Test
        fun `Skal kaste Feil dersom fagsak tilknyttet aktør med forespurt personident ikke finnes i db`() {
            val aktør = randomAktør()

            every { personidentService.hentOgLagreAktør(any(), any()) } returns aktør
            every { fagsakRepository.finnFagsakForAktør(any()) } returns null

            val feil = assertThrows<Feil> { fagsakService.hentFagsakForPerson(aktør) }

            assertEquals("Fant ikke fagsak på person", feil.message)
        }
    }
}
