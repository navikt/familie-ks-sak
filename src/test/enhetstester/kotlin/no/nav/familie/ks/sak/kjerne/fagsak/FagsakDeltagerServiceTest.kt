package no.nav.familie.ks.sak.kjerne.fagsak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerResponsDto
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerRolle
import no.nav.familie.ks.sak.common.exception.PdlPersonKanIkkeBehandlesIFagSystemÅrsak
import no.nav.familie.ks.sak.common.exception.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.FalskIdentitetPersonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PersonInfo
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FagsakDeltagerServiceTest {
    private val personidentService = mockk<PersonidentService>()
    private val integrasjonService = mockk<IntegrasjonService>(relaxed = true)
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val fagsakRepository = mockk<FagsakRepository>()
    private val personRepository = mockk<PersonRepository>()
    private val behandlingRepository = mockk<BehandlingRepository>()

    private val fagsakDeltagerService =
        FagsakDeltagerService(
            personidentService = personidentService,
            integrasjonService = integrasjonService,
            personopplysningerService = personopplysningerService,
            fagsakRepository = fagsakRepository,
            personRepository = personRepository,
            behandlingRepository = behandlingRepository,
        )

    @Test
    fun `Skal returnere maskert deltaker dersom saksbehandler ikke har tilgang til aktør med bestemt personident`() {
        // Arrange
        every { personidentService.hentAktør(any()) } returns randomAktør()
        every { integrasjonService.sjekkTilgangTilPerson(any()) } returns Tilgang("test", false)
        every { personopplysningerService.hentAdressebeskyttelseSomSystembruker(any()) } returns ADRESSEBESKYTTELSEGRADERING.FORTROLIG

        // Act
        val fagsakdeltakere = fagsakDeltagerService.hentFagsakDeltagere(randomFnr())

        // Assert
        assertThat(1).isEqualTo(fagsakdeltakere.size)
        assertThat(ADRESSEBESKYTTELSEGRADERING.FORTROLIG).isEqualTo(fagsakdeltakere.first().adressebeskyttelseGradering)
    }

    @Test
    fun `Skal returnere søker dersom metode kalles med søkers ident og saksbehandler har tilgang til identen`() {
        // Arrange
        val søkersFødselsdato = LocalDate.of(1985, 5, 1)
        val søkerPersonident = "01058512345"
        val søkerAktør = randomAktør(søkerPersonident)

        val barnPersonident = "01052212345"
        val barnAktør = randomAktør(barnPersonident)

        val barnIdenter = listOf(barnPersonident)

        every { personidentService.hentAktør(any()) } returns søkerAktør
        every { integrasjonService.sjekkTilgangTilPerson(any()) } returns Tilgang("test", true)
        every { personopplysningerService.hentPdlPersonInfoMedRelasjonerOgRegisterinformasjon(any()) } returns
            PdlPersonInfo.Person(
                personInfo =
                    PersonInfo(
                        søkersFødselsdato,
                        forelderBarnRelasjoner = setOf(ForelderBarnRelasjonInfo(barnAktør, FORELDERBARNRELASJONROLLE.BARN)),
                    ),
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

        // Act
        val fagsakdeltakere = fagsakDeltagerService.hentFagsakDeltagere(søkerPersonident)

        // Assert
        assertThat(1).isEqualTo(fagsakdeltakere.size)
        assertThat(søkerPersonident).isEqualTo(fagsakdeltakere.single().ident)
    }

    @Test
    fun `Skal returnere barn og forelder dersom metode kalles med barne-ident og saksbehandler har tilgang til barnet og forelderen`() {
        // Arrange
        val søkersFødselsdato = LocalDate.of(1985, 5, 1)
        val søkerPersonident = "01058512345"
        val søkerAktør = randomAktør(søkerPersonident)

        val barnFødselsdato = LocalDate.of(2022, 5, 1)
        val barnPersonident = "01052212345"
        val barnAktør = randomAktør(barnPersonident)

        every { personidentService.hentAktør(any()) } returns barnAktør
        every { integrasjonService.sjekkTilgangTilPerson(any()) } returns Tilgang("test", true)
        every { personopplysningerService.hentPdlPersonInfoMedRelasjonerOgRegisterinformasjon(any()) } returns
            PdlPersonInfo.Person(
                personInfo =
                    PersonInfo(
                        barnFødselsdato,
                        forelderBarnRelasjoner = setOf(ForelderBarnRelasjonInfo(søkerAktør, FORELDERBARNRELASJONROLLE.FAR)),
                    ),
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
            PersonInfo(
                søkersFødselsdato,
                forelderBarnRelasjoner =
                    setOf(
                        ForelderBarnRelasjonInfo(barnAktør, FORELDERBARNRELASJONROLLE.BARN),
                    ),
            )
        every { fagsakRepository.finnFagsakForAktør(any()) } returns fagsak

        // Act
        val fagsakdeltakere = fagsakDeltagerService.hentFagsakDeltagere(søkerPersonident)

        // Assert
        assertThat(2).isEqualTo(fagsakdeltakere.size)
        val barnDeltaker = fagsakdeltakere.find { it.rolle == FagsakDeltagerRolle.BARN }
        val forelderDeltaker = fagsakdeltakere.find { it.rolle == FagsakDeltagerRolle.FORELDER }
        assertThat(barnPersonident).isEqualTo(barnDeltaker?.ident)
        assertThat(søkerPersonident).isEqualTo(forelderDeltaker?.ident)
    }

    @Test
    fun `Setter korrekt egen ansatt status basert på respons fra integrasjoner`() {
        // Arrange
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
            integrasjonService.sjekkErEgenAnsattBulk(
                match { it.containsAll(listOf(erEgenAnsattIdent, erIkkeEgenAnsattIdent, manglerDataIdent)) },
            )
        } answers {
            mapOf(
                erEgenAnsattIdent to true,
                erIkkeEgenAnsattIdent to false,
            )
        }

        // Act
        val fagsakDeltagereMedEgenAnsattStatus = fagsakDeltagerService.settEgenAnsattStatusPåFagsakDeltagere(fagsakDeltagere)

        // Assert
        assertThat(fagsakDeltagereMedEgenAnsattStatus.map { it.ident to it.erEgenAnsatt })
            .containsExactlyInAnyOrder(
                erEgenAnsattIdent to true,
                erIkkeEgenAnsattIdent to false,
                manglerDataIdent to null,
            )
    }

    @Test
    fun `Dersom feilen PdlPersonKanIkkeBehandlesIFagSystemÅrsak kastes ved henting av forelder uten direkte relasjon skal det logges og ikke feile søket dersom forelder ikke har falsk identitet`() {
        // Arrange
        val personInfo = PersonInfo(LocalDate.now())
        val person = lagPerson()
        val behandling = lagBehandling()

        every {
            personidentService.hentAktør(person.aktør.aktivFødselsnummer())
        } returns person.aktør
        every {
            integrasjonService.sjekkTilgangTilPerson(person.aktør.aktivFødselsnummer())
        } returns Tilgang("test", true)
        every {
            integrasjonService.sjekkTilgangTilPerson(behandling.fagsak.aktør.aktivFødselsnummer())
        } returns Tilgang("test", true)
        every {
            personopplysningerService.hentPdlPersonInfoMedRelasjonerOgRegisterinformasjon(person.aktør)
        } returns PdlPersonInfo.Person(personInfo = personInfo)
        every {
            personRepository.findByAktør(person.aktør)
        } returns listOf(person)
        every {
            behandlingRepository.hentBehandling(behandlingId = person.personopplysningGrunnlag.behandlingId)
        } returns behandling

        every { fagsakRepository.finnFagsakForAktør(person.aktør) } returns null
        every { integrasjonService.sjekkErEgenAnsattBulk(any()) } returns emptyMap()
        every {
            personopplysningerService.hentPdlPersonInfoEnkel(behandling.fagsak.aktør)
        } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)

        // Act og Assert
        val fagsakDeltagere = fagsakDeltagerService.hentFagsakDeltagere(person.aktør.aktivFødselsnummer())
        assertThat(fagsakDeltagere).hasSize(1)
    }

    @Test
    fun `Dersom feilen PdlPersonKanIkkeBehandlesIFagSystemÅrsak kastes ved henting av forelder uten direkte relasjon og forelder har falsk identitet skal person info om falsk identitet returneres`() {
        // Arrange
        val personInfo = PersonInfo(LocalDate.now())
        val person = lagPerson()
        val forelderUtenDirekteRelasjon = lagPerson()
        val behandling = lagBehandling(fagsak = lagFagsak(aktør = forelderUtenDirekteRelasjon.aktør))

        every {
            personidentService.hentAktør(person.aktør.aktivFødselsnummer())
        } returns person.aktør
        every {
            integrasjonService.sjekkTilgangTilPerson(person.aktør.aktivFødselsnummer())
        } returns Tilgang("test", true)
        every {
            integrasjonService.sjekkTilgangTilPerson(behandling.fagsak.aktør.aktivFødselsnummer())
        } returns Tilgang("test", true)
        every {
            personopplysningerService.hentPdlPersonInfoMedRelasjonerOgRegisterinformasjon(person.aktør)
        } returns PdlPersonInfo.Person(personInfo = personInfo)
        every {
            personRepository.findByAktør(person.aktør)
        } returns listOf(person)
        every {
            behandlingRepository.hentBehandling(behandlingId = person.personopplysningGrunnlag.behandlingId)
        } returns behandling

        every { fagsakRepository.finnFagsakForAktør(person.aktør) } returns null
        every { integrasjonService.sjekkErEgenAnsattBulk(any()) } returns emptyMap()
        every {
            personopplysningerService.hentPdlPersonInfoEnkel(behandling.fagsak.aktør)
        } returns
            PdlPersonInfo.FalskPerson(
                falskIdentitetPersonInfo =
                    FalskIdentitetPersonInfo(
                        navn = forelderUtenDirekteRelasjon.navn,
                        fødselsdato = forelderUtenDirekteRelasjon.fødselsdato,
                        kjønn = forelderUtenDirekteRelasjon.kjønn,
                    ),
            )

        // Act og Assert
        val fagsakDeltagere = fagsakDeltagerService.hentFagsakDeltagere(person.aktør.aktivFødselsnummer())
        assertThat(fagsakDeltagere).hasSize(2)
        val forelder = fagsakDeltagere.first { it.rolle == FagsakDeltagerRolle.FORELDER }
        assertThat(forelder.navn).isEqualTo(forelderUtenDirekteRelasjon.navn)
        assertThat(forelder.kjønn).isEqualTo(forelderUtenDirekteRelasjon.kjønn)
        assertThat(forelder.fagsakId).isEqualTo(behandling.fagsak.id)
        assertThat(forelder.harTilgang).isTrue
    }
}
