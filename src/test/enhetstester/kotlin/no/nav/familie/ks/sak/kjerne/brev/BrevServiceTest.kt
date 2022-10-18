package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.shouldNotBeNull
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringRepository
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InnhenteOpplysningerDataDto
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.settpåvent.SettPåVentService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BrevServiceTest {
    @MockK
    private lateinit var brevKlient: BrevKlient

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockK
    private lateinit var utgåendeJournalføringService: UtgåendeJournalføringService

    @MockK
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @MockK
    private lateinit var settPåVentService: SettPåVentService

    @MockK
    private lateinit var behandlingRepository: BehandlingRepository

    @MockK
    private lateinit var fagsakRepository: FagsakRepository

    @MockK
    private lateinit var journalføringRepository: JournalføringRepository

    @InjectMockKs
    private lateinit var brevService: BrevService

    private val søker = randomAktør()
    private val fagsak = lagFagsak(søker)
    private val behandling = lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val manueltBrevDto =
        ManueltBrevDto(
            brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
            mottakerIdent = søker.aktivFødselsnummer(),
            multiselectVerdier = listOf("Dokumentasjon som viser når barna kom til Norge.")
        )

    @Test
    fun `hentForhåndsvisningAvBrev - skal hente pdf i form av en ByteArray fra BrevKlient`() {
        val brevSlot = slot<BrevDto>()

        every { personopplysningGrunnlagService.hentSøker(behandlingId = behandling.id) } returns lagPerson(
            lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søker.aktivFødselsnummer()),
            søker
        )
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns ArbeidsfordelingPåBehandling(
            behandlingId = behandling.id,
            behandlendeEnhetNavn = "Behandlende enhet",
            behandlendeEnhetId = "1234"
        )

        every { brevKlient.genererBrev(any(), capture(brevSlot)) } returns ByteArray(10)

        brevService.hentForhåndsvisningAvBrev(behandlingId = behandling.id, manueltBrevDto).shouldNotBeNull()

        val brev = brevSlot.captured

        assertEquals(Brevmal.INNHENTE_OPPLYSNINGER, brev.mal)

        val innhenteOpplysningerData = brev.data as InnhenteOpplysningerDataDto

        assertEquals("Behandlende enhet", innhenteOpplysningerData.delmalData.signatur.enhet?.first())
        assertEquals(søker.aktivFødselsnummer(), innhenteOpplysningerData.flettefelter.fodselsnummer?.first())
        assertEquals(
            "Dokumentasjon som viser når barna kom til Norge.",
            innhenteOpplysningerData.flettefelter.dokumentliste?.first()
        )
    }

    @Test
    fun `hentForhåndsvisningAvBrev - skal kaste feil dersom kall mot 'familie-brev' feiler`() {
        every { personopplysningGrunnlagService.hentSøker(behandlingId = behandling.id) } returns lagPerson(
            lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søker.aktivFødselsnummer()),
            søker
        )
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns ArbeidsfordelingPåBehandling(
            behandlingId = behandling.id,
            behandlendeEnhetNavn = "Behandlende enhet",
            behandlendeEnhetId = "1234"
        )

        every {
            brevKlient.genererBrev(
                any(),
                any()
            )
        } throws Exception("Kall mot familie-brev feilet")

        val feil =
            assertThrows<Feil> { brevService.hentForhåndsvisningAvBrev(behandlingId = behandling.id, manueltBrevDto) }
        assertEquals(
            "Klarte ikke generere brev for ${manueltBrevDto.brevmal}. Kall mot familie-brev feilet",
            feil.message
        )
        assertEquals(
            "Det har skjedd en feil. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
            feil.frontendFeilmelding
        )
    }
}
