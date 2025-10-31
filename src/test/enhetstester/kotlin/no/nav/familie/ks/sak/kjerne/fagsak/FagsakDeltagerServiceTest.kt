package no.nav.familie.ks.sak.kjerne.fagsak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerResponsDto
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerRolle
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
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

    private val fagsakService =
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
        val fagsakdeltakere = fagsakService.hentFagsakDeltagere(randomFnr())

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

        // Act
        val fagsakdeltakere = fagsakService.hentFagsakDeltagere(søkerPersonident)

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

        // Act
        val fagsakdeltakere = fagsakService.hentFagsakDeltagere(søkerPersonident)

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
