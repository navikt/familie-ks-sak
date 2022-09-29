package no.nav.familie.ks.sak.kjerne.fagsak

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class FagsakServiceTest {
    @MockK
    private lateinit var personidentService: PersonidentService

    @MockK
    private lateinit var integrasjonClient: IntegrasjonClient

    @MockK
    private lateinit var personopplysningerService: PersonOpplysningerService

    @MockK
    private lateinit var fagsakRepository: FagsakRepository

    @MockK
    private lateinit var personRepository: PersonRepository

    @MockK
    private lateinit var behandlingRepository: BehandlingRepository

    @InjectMockKs
    private lateinit var fagsakService: FagsakService

    @Test
    fun `hentFagsakDeltagere - skal returnere maskert deltaker dersom saksbehandler ikke har tilgang til aktør med bestemt personident`() {
        every { personidentService.hentAktør(any()) } returns randomAktør()
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(false)
        every { personopplysningerService.hentAdressebeskyttelseSomSystembruker(any()) } returns ADRESSEBESKYTTELSEGRADERING.FORTROLIG

        val fagsakdeltakere = fagsakService.hentFagsakDeltagere(randomFnr())
        assertEquals(1, fagsakdeltakere.size)
        assertEquals(ADRESSEBESKYTTELSEGRADERING.FORTROLIG, fagsakdeltakere.first().adressebeskyttelseGradering)
    }

    @Test
    fun `hentFagsakDeltagere - skal returnere assosierte fagsakdeltakere derosom saksbehandlerhar tilgang til aktør med bestemt personident`() {
        val søkersFødselsdato = LocalDate.of(1985, 5, 1)
        val søkerPersonident = "01058512345"
        val søkerAktør = randomAktør(søkerPersonident)

        val barnPersonident = "01052212345"
        val barnAktør = randomAktør(barnPersonident)

        val assosiertDeltagerFødselsdato = LocalDate.now().minusMonths(3)
        val barnIdenter = listOf(barnPersonident)

        every { personidentService.hentAktør(any()) } returns søkerAktør
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true)
        every { personopplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(any()) } returns PdlPersonInfo(
            søkersFødselsdato,
            forelderBarnRelasjoner = setOf(ForelderBarnRelasjonInfo(barnAktør, FORELDERBARNRELASJONROLLE.BARN))
        )
        every { personRepository.findByAktør(any()) } returns listOf(
            Person(
                aktør = søkerAktør,
                type = PersonType.SØKER,
                fødselsdato = søkersFødselsdato,
                kjønn = Kjønn.MANN,
                personopplysningGrunnlag = lagPersonopplysningGrunnlag(1, søkerPersonident, barnIdenter)
            )
        )
        val fagsak = lagFagsak(søkerAktør)
        every { behandlingRepository.hentAktivBehandling(any()) } returns lagBehandling(
            fagsak = fagsak,
            opprettetÅrsak = BehandlingÅrsak.SØKNAD
        )
        every { fagsakRepository.finnFagsakForAktør(any()) } returns fagsak

        val fagsakdeltakere = fagsakService.hentFagsakDeltagere(søkerPersonident)
        assertEquals(1, fagsakdeltakere.size)
    }

    fun `hentEllerOpprettFagsak - skal`() {
    }

    fun `hentEllerOpprettFagsak - skal kaste`() {
    }

    fun `hentMinimalFagsak - skal`() {
    }

    fun `hentFagsak - skal`() {
    }

    fun `hentFagsak - skal kaste`() {
    }

    fun `hentFagsakForPeson - skal`() {
    }

    fun `hentFagsakForPeson - skal kaste`() {
    }
}
