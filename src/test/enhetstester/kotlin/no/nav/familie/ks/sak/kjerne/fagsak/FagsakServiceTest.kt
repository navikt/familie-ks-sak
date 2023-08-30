package no.nav.familie.ks.sak.kjerne.fagsak

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandling
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsresultatstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsstatus
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerRolle
import no.nav.familie.ks.sak.api.dto.FagsakRequestDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.ks.sak.kjerne.tilbakekreving.TilbakekrevingsbehandlingHentService
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class FagsakServiceTest {
    @MockK
    private lateinit var personidentService: PersonidentService

    @MockK
    private lateinit var integrasjonService: IntegrasjonService

    @MockK
    private lateinit var personopplysningerService: PersonOpplysningerService

    @MockK
    private lateinit var fagsakRepository: FagsakRepository

    @MockK
    private lateinit var personRepository: PersonRepository

    @MockK
    private lateinit var behandlingRepository: BehandlingRepository

    @MockK
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @MockK
    private lateinit var andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService

    @MockK
    private lateinit var taskService: TaskService

    @MockK
    private lateinit var tilbakekrevingsbehandlingHentService: TilbakekrevingsbehandlingHentService

    @MockK
    private lateinit var vedtakRepository: VedtakRepository

    @InjectMockKs
    private lateinit var fagsakService: FagsakService

    @Test
    fun `hentFagsakDeltagere - skal returnere maskert deltaker dersom saksbehandler ikke har tilgang til aktør med bestemt personident`() {
        every { personidentService.hentAktør(any()) } returns randomAktør()
        every { integrasjonService.sjekkTilgangTilPerson(any()) } returns Tilgang("test", false)
        every { personopplysningerService.hentAdressebeskyttelseSomSystembruker(any()) } returns ADRESSEBESKYTTELSEGRADERING.FORTROLIG

        val fagsakdeltakere = fagsakService.hentFagsakDeltagere(randomFnr())
        assertEquals(1, fagsakdeltakere.size)
        assertEquals(ADRESSEBESKYTTELSEGRADERING.FORTROLIG, fagsakdeltakere.first().adressebeskyttelseGradering)
    }

    @Test
    fun `hentFagsakDeltagere - skal returnere søker dersom metode kalles med søkers ident og saksbehandler har tilgang til identen`() {
        val søkersFødselsdato = LocalDate.of(1985, 5, 1)
        val søkerPersonident = "01058512345"
        val søkerAktør = randomAktør(søkerPersonident)

        val barnPersonident = "01052212345"
        val barnAktør = randomAktør(barnPersonident)

        val barnIdenter = listOf(barnPersonident)

        every { personidentService.hentAktør(any()) } returns søkerAktør
        every { integrasjonService.sjekkTilgangTilPerson(any()) } returns Tilgang("test", true)
        every { personopplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(any()) } returns PdlPersonInfo(
            søkersFødselsdato,
            forelderBarnRelasjoner = setOf(ForelderBarnRelasjonInfo(barnAktør, FORELDERBARNRELASJONROLLE.BARN)),
        )
        every { personRepository.findByAktør(any()) } returns listOf(
            Person(
                aktør = søkerAktør,
                type = PersonType.SØKER,
                fødselsdato = søkersFødselsdato,
                kjønn = Kjønn.MANN,
                personopplysningGrunnlag = lagPersonopplysningGrunnlag(1, søkerPersonident, barnIdenter),
            ),
        )
        val fagsak = lagFagsak(søkerAktør)
        every { behandlingRepository.hentBehandling(any()) } returns lagBehandling(
            fagsak = fagsak,
            opprettetÅrsak = BehandlingÅrsak.SØKNAD,
        )
        every { fagsakRepository.finnFagsakForAktør(any()) } returns fagsak

        val fagsakdeltakere = fagsakService.hentFagsakDeltagere(søkerPersonident)
        assertEquals(1, fagsakdeltakere.size)
        assertEquals(søkerPersonident, fagsakdeltakere.single().ident)
    }

    @Test
    fun `hentFagsakDeltagere - skal returnere barn og forelder dersom metode kalles med barne-ident og saksbehandler har tilgang til barnet og forelderen`() {
        val søkersFødselsdato = LocalDate.of(1985, 5, 1)
        val søkerPersonident = "01058512345"
        val søkerAktør = randomAktør(søkerPersonident)

        val barnFødselsdato = LocalDate.of(2022, 5, 1)
        val barnPersonident = "01052212345"
        val barnAktør = randomAktør(barnPersonident)

        every { personidentService.hentAktør(any()) } returns barnAktør
        every { integrasjonService.sjekkTilgangTilPerson(any()) } returns Tilgang("test", true)
        every { personopplysningerService.hentPersonInfoMedRelasjonerOgRegisterinformasjon(any()) } returns PdlPersonInfo(
            barnFødselsdato,
            forelderBarnRelasjoner = setOf(ForelderBarnRelasjonInfo(søkerAktør, FORELDERBARNRELASJONROLLE.FAR)),
        )
        every { personRepository.findByAktør(any()) } returns listOf(
            Person(
                aktør = barnAktør,
                type = PersonType.BARN,
                fødselsdato = barnFødselsdato,
                kjønn = Kjønn.MANN,
                personopplysningGrunnlag = lagPersonopplysningGrunnlag(1, barnPersonident, emptyList()),
            ),
        )
        val fagsak = lagFagsak(søkerAktør)
        every { behandlingRepository.hentBehandling(any()) } returns lagBehandling(
            fagsak = fagsak,
            opprettetÅrsak = BehandlingÅrsak.SØKNAD,
        )
        every { personopplysningerService.hentPersoninfoEnkel(any()) } returns PdlPersonInfo(
            søkersFødselsdato,
            forelderBarnRelasjoner = setOf(
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
    fun `hentEllerOpprettFagsak - skal returnere eksisterende fagsak når forespurt personIdent eller aktørId har fagsak i db`() {
        val fødselsnummer = randomFnr()
        val aktør = randomAktør(fødselsnummer)
        val fagsak = lagFagsak(aktør)

        every { personidentService.hentOgLagreAktør(aktør.aktørId, true) } returns aktør
        every { personidentService.hentOgLagreAktør(fødselsnummer, true) } returns aktør
        every { fagsakRepository.finnFagsakForAktør(aktør) } returns fagsak
        every { behandlingRepository.findByFagsakAndAktiv(fagsak.id) } returns lagBehandling(
            fagsak,
            opprettetÅrsak = BehandlingÅrsak.SØKNAD,
        )
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any()) } returns emptyList()
        every { personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(any()) } returns lagPersonopplysningGrunnlag()

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
    fun `hentEllerOpprettFagsak - skal returnere ny fagsak når forespurt personIdent eller aktørId ikke har fagsak i db`() {
        val fødselsnummer = randomFnr()
        val aktør = randomAktør(fødselsnummer)
        val fagsak = lagFagsak(aktør)

        every { personidentService.hentOgLagreAktør(aktør.aktørId, true) } returns aktør
        every { personidentService.hentOgLagreAktør(fødselsnummer, true) } returns aktør
        every { fagsakRepository.finnFagsakForAktør(aktør) } returns null
        every { fagsakRepository.save(fagsak) } returns fagsak
        every { behandlingRepository.findByFagsakAndAktiv(fagsak.id) } returns null
        every { taskService.save(any()) } returns mockk()

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
    fun `hentEllerOpprettFagsak - skal kaste Feil dersom verken personident eller aktørId er satt i FagsakRequestDto`() {
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

    @Test
    fun `hentMinimalFagsak - skal returnere fagsak med tilhørende behandlinger når forespurt fagsakId finnes i db`() {
        val fagsak = lagFagsak(randomAktør())
        val barnehagelisteBehandling =
            lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.BARNEHAGELISTE).apply { aktiv = true }

        every { fagsakRepository.finnFagsak(any()) } returns fagsak
        every { behandlingRepository.finnBehandlinger(fagsak.id) } returns listOf(
            lagBehandling(
                fagsak,
                opprettetÅrsak = BehandlingÅrsak.SØKNAD,
            ).apply { aktiv = false },
            barnehagelisteBehandling,
        )
        every { behandlingRepository.findByFagsakAndAktiv(fagsak.id) } returns barnehagelisteBehandling
        every { vedtakRepository.findByBehandlingAndAktivOptional(any()) } returns mockk(relaxed = true)
        every { tilbakekrevingsbehandlingHentService.hentTilbakekrevingsbehandlinger(fagsak.id) } returns
            listOf(
                Behandling(
                    behandlingId = UUID.randomUUID(),
                    opprettetTidspunkt = LocalDateTime.now(),
                    aktiv = true,
                    årsak = null,
                    type = Behandlingstype.TILBAKEKREVING,
                    status = Behandlingsstatus.UTREDES,
                    vedtaksdato = null,
                    resultat = Behandlingsresultatstype.IKKE_FASTSATT,
                ),
            )

        val fagsakResponse = fagsakService.hentMinimalFagsak(fagsak.id)

        assertEquals(fagsak.id, fagsakResponse.id)
        assertEquals(2, fagsakResponse.behandlinger.size)
        assertEquals(1, fagsakResponse.tilbakekrevingsbehandlinger.size)
    }

    @Test
    fun `hentMinimalFagsak - skal kaste FunksjonellFeil dersom fagsak med fagsakId ikke finnes i db`() {
        every { fagsakRepository.finnFagsak(any()) } returns null

        val funksjonellFeil = assertThrows<FunksjonellFeil> { fagsakService.hentMinimalFagsak(404L) }

        assertEquals("Finner ikke fagsak med id 404", funksjonellFeil.message)
    }

    @Test
    fun `hentFagsak - skal returnere fagsak når det finnes en fagsak med forespurt fagsakId i db`() {
        val fagsak = lagFagsak(randomAktør())
        every { fagsakRepository.finnFagsak(any()) } returns fagsak

        val hentetFagsak = fagsakService.hentFagsak(fagsak.id)

        assertEquals(fagsak.id, hentetFagsak.id)
    }

    @Test
    fun `hentFagsak - skal kaste Funksjonell feil dersom fagsak med fagsakId ikke finnes i db`() {
        every { fagsakRepository.finnFagsak(any()) } returns null

        val funksjonellFeil = assertThrows<FunksjonellFeil> { fagsakService.hentFagsak(404L) }

        assertEquals("Finner ikke fagsak med id 404", funksjonellFeil.message)
    }

    @Test
    fun `hentFagsakForPeson - skal returnere fagsak dersom fagsak tilknyttet aktør med forespurt personident finnes i db`() {
        val aktør = randomAktør()
        val fagsak = lagFagsak(aktør)

        every { personidentService.hentOgLagreAktør(any(), any()) } returns aktør
        every { fagsakRepository.finnFagsakForAktør(any()) } returns fagsak

        val fagsakForPerson = fagsakService.hentFagsakForPerson(aktør)

        assertEquals(fagsak.id, fagsakForPerson.id)
        assertEquals(fagsak.aktør, fagsakForPerson.aktør)
    }

    @Test
    fun `hentFagsakForPeson - skal kaste Feil dersom fagsak tilknyttet aktør med forespurt personident ikke finnes i db`() {
        val aktør = randomAktør()

        every { personidentService.hentOgLagreAktør(any(), any()) } returns aktør
        every { fagsakRepository.finnFagsakForAktør(any()) } returns null

        val feil = assertThrows<Feil> { fagsakService.hentFagsakForPerson(aktør) }

        assertEquals("Fant ikke fagsak på person", feil.message)
    }
}
