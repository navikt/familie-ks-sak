package no.nav.familie.ks.sak.kjerne.brev.mottaker

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagTestPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ValiderBrevmottakerServiceTest {
    private val brevmottakerRepository = mockk<BrevmottakerRepository>()
    private val persongrunnlagService = mockk<PersonopplysningGrunnlagService>()
    private val personOpplysningerService = mockk<PersonopplysningerService>()
    private val fagsakRepository = mockk<FagsakRepository>()
    val validerBrevmottakerService =
        ValiderBrevmottakerService(
            brevmottakerRepository = brevmottakerRepository,
            persongrunnlagService = persongrunnlagService,
            personOpplysningerService = personOpplysningerService,
            fagsakRepository = fagsakRepository,
        )

    private val behandlingId = 0L
    val brevmottaker =
        BrevmottakerDb(
            behandlingId = behandlingId,
            type = MottakerType.DØDSBO,
            navn = "Donald Duck",
            adresselinje1 = "Andebyveien 1",
            postnummer = "0000",
            poststed = "OSLO",
            landkode = "NO",
        )
    val søker = tilfeldigPerson(personType = PersonType.SØKER)

    @Test
    fun `Skal ikke kaste funksjonell feil når en behandling ikke inneholder noen manuelle brevmottakere`() {
        every { brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId) } returns emptyList()

        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
            behandlingId,
            ekstraBarnLagtTilIBrev = emptyList(),
        )

        verify(exactly = 1) { brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId) }
        verify(exactly = 0) { persongrunnlagService.finnAktivPersonopplysningGrunnlag(any()) }
        verify(exactly = 0) {
            personOpplysningerService.hentIdenterMedStrengtFortroligAdressebeskyttelse(
                any(),
            )
        }
    }

    @Test
    fun `Skal kaste en FunksjonellFeil exception når en behandling inneholder minst en strengt fortrolig person og minst en manuell brevmottaker`() {
        every { brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId) } returns listOf(brevmottaker)
        every { persongrunnlagService.finnAktivPersonopplysningGrunnlag(behandlingId) } returns
            lagTestPersonopplysningGrunnlag(
                behandlingId,
                søker,
            )
        every { personOpplysningerService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any()) } returns
            listOf(
                søker.aktør.aktivFødselsnummer(),
            )

        assertThatThrownBy {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
                behandlingId,
                ekstraBarnLagtTilIBrev = emptyList(),
            )
        }.isInstanceOf(FunksjonellFeil::class.java)
            .hasMessageContaining("strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere")
    }

    @Test
    fun `Skal ikke kaste funksjonell feil når behandling ikke inneholder noen strengt fortrolige personer og inneholder minst en manuell brevmottaker`() {
        every { brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId) } returns listOf(brevmottaker)
        every { persongrunnlagService.finnAktivPersonopplysningGrunnlag(behandlingId) } returns
            lagTestPersonopplysningGrunnlag(
                behandlingId,
                søker,
            )
        every { personOpplysningerService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any()) } returns emptyList()

        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
            behandlingId,
            ekstraBarnLagtTilIBrev = emptyList(),
        )
        verify(exactly = 1) {
            personOpplysningerService.hentIdenterMedStrengtFortroligAdressebeskyttelse(
                any(),
            )
        }
    }

    @Test
    fun `Skal ikke kaste en exception når en behandling inneholder minst en strengt fortrolig person og ingen manuelle brevmottakere`() {
        every { brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId) } returns emptyList()
        every { persongrunnlagService.finnAktivPersonopplysningGrunnlag(behandlingId) } returns
            lagTestPersonopplysningGrunnlag(
                behandlingId,
                søker,
            )
        every { personOpplysningerService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any()) } returns
            listOf(
                søker.aktør.aktivFødselsnummer(),
            )

        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
            behandlingId,
            ekstraBarnLagtTilIBrev = emptyList(),
        )
    }

    @Test
    fun `Skal kaste en FunksjonellFeil exception når en behandling inneholder minst en strengt fortrolig person og det blir forsøkt lagt til en ny manuell brevmottaker`() {
        every { brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId) } returns emptyList()
        every { persongrunnlagService.finnAktivPersonopplysningGrunnlag(behandlingId) } returns
            lagTestPersonopplysningGrunnlag(
                behandlingId,
                søker,
            )
        every { personOpplysningerService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any()) } returns
            listOf(
                søker.aktør.aktivFødselsnummer(),
            )

        assertThatThrownBy {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
                behandlingId,
                brevmottaker,
                ekstraBarnLagtTilIBrev = emptyList(),
            )
        }.isInstanceOf(FunksjonellFeil::class.java)
            .hasMessageContaining("strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere")
    }
}
