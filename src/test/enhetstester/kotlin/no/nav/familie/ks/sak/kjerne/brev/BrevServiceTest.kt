package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårsvurderingMedSøkersVilkår
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.shouldNotBeNull
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringRepository
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.SettBehandlingPåVentService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.InnhenteOpplysningerDataDto
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

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
    private lateinit var behandlingRepository: BehandlingRepository

    @MockK
    private lateinit var journalføringRepository: JournalføringRepository

    @MockK
    private lateinit var integrasjonClient: IntegrasjonClient

    @MockK
    private lateinit var loggService: LoggService

    @MockK
    private lateinit var taskService: TaskService

    @MockK
    private lateinit var settBehandlingPåVentService: SettBehandlingPåVentService

    @MockK
    private lateinit var totrinnskontrollService: TotrinnskontrollService

    @MockK
    private lateinit var brevPeriodeService: BrevPeriodeService

    @MockK
    private lateinit var sanityService: SanityService

    @MockK
    private lateinit var vedtaksperiodeService: VedtaksperiodeService

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

    @ParameterizedTest
    @EnumSource(
        value = Brevmal::class,
        names = ["INNHENTE_OPPLYSNINGER", "INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED", "VARSEL_OM_REVURDERING", "VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED", "VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS"],
        mode = EnumSource.Mode.INCLUDE
    )
    fun `genererOgSendBrev - skal journalføre brev med forside for brevmaler som tilsier det`(brevmal: Brevmal) {
        every { personopplysningGrunnlagService.hentSøker(behandlingId = behandling.id) } returns lagPerson(
            lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søker.aktivFødselsnummer()),
            søker
        )
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns ArbeidsfordelingPåBehandling(
            behandlingId = behandling.id,
            behandlendeEnhetNavn = "Behandlende enhet",
            behandlendeEnhetId = "1234"
        )

        every { taskService.save(any()) } returns mockk()

        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling

        every { brevKlient.genererBrev(any(), any()) } returns ByteArray(10)

        val førstesideSlot = slot<Førsteside>()

        every {
            utgåendeJournalføringService.journalførDokument(
                any(),
                any(),
                any(),
                any(),
                any(),
                capture(førstesideSlot),
                any()
            )
        } returns "0"

        every { journalføringRepository.save(any()) } returns mockk()

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns lagVilkårsvurderingMedSøkersVilkår(
            søkerAktør = søker,
            behandling = behandling,
            resultat = Resultat.IKKE_VURDERT
        )

        every { settBehandlingPåVentService.settBehandlingPåVent(any(), any()) } just runs

        brevService.genererOgSendBrev(
            behandling.id,
            ManueltBrevDto(
                brevmal = brevmal,
                mottakerIdent = søker.aktivFødselsnummer(),
                barnasFødselsdager = emptyList()
            )
        )

        førstesideSlot.captured.shouldNotBeNull()
    }

    @ParameterizedTest
    @EnumSource(
        value = Brevmal::class,
        names = ["INFORMASJONSBREV_DELT_BOSTED", "HENLEGGE_TRUKKET_SØKNAD", "SVARTIDSBREV", "FORLENGET_SVARTIDSBREV", "INFORMASJONSBREV_KAN_SØKE", "INFORMASJONSBREV_KAN_SØKE_EØS"],
        mode = EnumSource.Mode.INCLUDE
    )
    fun `genererOgSendBrev - skal journalføre brev uten forside for brevmaler som tilsier det`(brevmal: Brevmal) {
        every { personopplysningGrunnlagService.hentSøker(behandlingId = behandling.id) } returns lagPerson(
            lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søker.aktivFødselsnummer()),
            søker
        )
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns ArbeidsfordelingPåBehandling(
            behandlingId = behandling.id,
            behandlendeEnhetNavn = "Behandlende enhet",
            behandlendeEnhetId = "1234"
        )

        every { taskService.save(any()) } returns mockk()

        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling

        every { brevKlient.genererBrev(any(), any()) } returns ByteArray(10)

        every {
            utgåendeJournalføringService.journalførDokument(
                any(),
                any(),
                any(),
                any(),
                any(),
                null,
                any()
            )
        } returns "0"

        every { journalføringRepository.save(any()) } returns mockk()

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns lagVilkårsvurderingMedSøkersVilkår(
            søkerAktør = søker,
            behandling = behandling,
            resultat = Resultat.IKKE_VURDERT
        )

        every { settBehandlingPåVentService.settBehandlingPåVent(any(), any()) } just runs

        brevService.genererOgSendBrev(
            behandling.id,
            ManueltBrevDto(
                brevmal = brevmal,
                mottakerIdent = søker.aktivFødselsnummer(),
                barnasFødselsdager = emptyList(),
                behandlingKategori = BehandlingKategori.NASJONAL,
                antallUkerSvarfrist = 5
            )
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = Brevmal::class,
        names = ["VEDTAK_FØRSTEGANGSVEDTAK", "VEDTAK_ENDRING", "VEDTAK_OPPHØRT", "VEDTAK_OPPHØR_MED_ENDRING", "VEDTAK_AVSLAG", "VEDTAK_FORTSATT_INNVILGET", "VEDTAK_KORREKSJON_VEDTAKSBREV", "VEDTAK_OPPHØR_DØDSFALL", "AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG", "AUTOVEDTAK_NYFØDT_FØRSTE_BARN", "AUTOVEDTAK_NYFØDT_BARN_FRA_FØR"],
        mode = EnumSource.Mode.INCLUDE
    )
    fun `genererOgSendBrev - skal ikke journalføre brev for brevmaler som ikke kan sendes manuelt`(brevmal: Brevmal) {
        every { personopplysningGrunnlagService.hentSøker(behandlingId = behandling.id) } returns lagPerson(
            lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søker.aktivFødselsnummer()),
            søker
        )
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns ArbeidsfordelingPåBehandling(
            behandlingId = behandling.id,
            behandlendeEnhetNavn = "Behandlende enhet",
            behandlendeEnhetId = "1234"
        )

        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling

        every { brevKlient.genererBrev(any(), any()) } returns ByteArray(10)

        val feil = assertThrows<Feil> {
            brevService.genererOgSendBrev(
                behandling.id,
                ManueltBrevDto(
                    brevmal = brevmal,
                    mottakerIdent = søker.aktivFødselsnummer()
                )
            )
        }
        assertEquals("Kan ikke mappe fra manuel brevrequest til $brevmal.", feil.message)
    }

    @ParameterizedTest
    @EnumSource(
        value = Brevmal::class,
        names = ["INNHENTE_OPPLYSNINGER", "VARSEL_OM_REVURDERING"],
        mode = EnumSource.Mode.INCLUDE
    )
    fun `genererOgSendBrev - skal journalføre brev og legge til AnnenVurdering for søker i vilkårsvurderingen for INNHENTE_OPPLYSNINGER og VARSEL_OM_REVURDERING`(
        brevmal: Brevmal
    ) {
        every { personopplysningGrunnlagService.hentSøker(behandlingId = behandling.id) } returns lagPerson(
            lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søker.aktivFødselsnummer()),
            søker
        )
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns ArbeidsfordelingPåBehandling(
            behandlingId = behandling.id,
            behandlendeEnhetNavn = "Behandlende enhet",
            behandlendeEnhetId = "1234"
        )

        every { taskService.save(any()) } returns mockk()

        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling

        every { brevKlient.genererBrev(any(), any()) } returns ByteArray(10)

        every {
            utgåendeJournalføringService.journalførDokument(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns "0"

        every { journalføringRepository.save(any()) } returns mockk()

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns lagVilkårsvurderingMedSøkersVilkår(
            søkerAktør = søker,
            behandling = behandling,
            resultat = Resultat.IKKE_VURDERT
        )

        every { settBehandlingPåVentService.settBehandlingPåVent(any(), any()) } just runs

        brevService.genererOgSendBrev(
            behandling.id,
            ManueltBrevDto(
                brevmal = brevmal,
                mottakerIdent = søker.aktivFødselsnummer(),
                barnasFødselsdager = emptyList()
            )
        )

        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) }
    }

    @ParameterizedTest
    @EnumSource(
        value = Brevmal::class,
        names = [
            "FORLENGET_SVARTIDSBREV",
            "INNHENTE_OPPLYSNINGER",
            "VARSEL_OM_REVURDERING",
            "INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED",
            "VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS",
            "VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED",
            "SVARTIDSBREV"
        ],
        mode = EnumSource.Mode.INCLUDE
    )
    fun `genererOgSendBrev - skal journalføre brev og sette behandling på vent`(
        brevmal: Brevmal
    ) {
        every { personopplysningGrunnlagService.hentSøker(behandlingId = behandling.id) } returns lagPerson(
            lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søker.aktivFødselsnummer()),
            søker
        )
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns ArbeidsfordelingPåBehandling(
            behandlingId = behandling.id,
            behandlendeEnhetNavn = "Behandlende enhet",
            behandlendeEnhetId = "1234"
        )

        every { taskService.save(any()) } returns mockk()

        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling

        every { brevKlient.genererBrev(any(), any()) } returns ByteArray(10)

        every {
            utgåendeJournalføringService.journalførDokument(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns "0"

        every { journalføringRepository.save(any()) } returns mockk()

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns lagVilkårsvurderingMedSøkersVilkår(
            søkerAktør = søker,
            behandling = behandling,
            resultat = Resultat.IKKE_VURDERT
        )

        every { settBehandlingPåVentService.settBehandlingPåVent(any(), any()) } just runs

        brevService.genererOgSendBrev(
            behandling.id,
            ManueltBrevDto(
                brevmal = brevmal,
                mottakerIdent = søker.aktivFødselsnummer(),
                barnasFødselsdager = emptyList(),
                behandlingKategori = BehandlingKategori.NASJONAL,
                antallUkerSvarfrist = 5
            )
        )

        verify(exactly = 1) { settBehandlingPåVentService.settBehandlingPåVent(any(), any()) }
    }
}
