package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import no.nav.familie.ks.sak.api.dto.BrevmottakerDto
import no.nav.familie.ks.sak.api.dto.Bruker
import no.nav.familie.ks.sak.api.dto.FullmektigEllerVerge
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.api.dto.utvidManueltBrevDtoMedEnhetOgMottaker
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagBrevmottakerDto
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårsvurderingMedSøkersVilkår
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.shouldNotBeNull
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringRepository
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.SettBehandlingPåVentService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ks.sak.kjerne.brev.mottaker.MottakerType
import no.nav.familie.ks.sak.kjerne.brev.mottaker.ValiderBrevmottakerService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BrevServiceTest {
    private val genererBrevService = mockk<GenererBrevService>()
    private val personopplysningGrunnlagService = mockk<PersonopplysningGrunnlagService>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val utgåendeJournalføringService = mockk<UtgåendeJournalføringService>()
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val journalføringRepository = mockk<JournalføringRepository>()
    private val integrasjonKlient = mockk<IntegrasjonKlient>()
    private val loggService = mockk<LoggService>()
    private val taskService = mockk<TaskRepositoryWrapper>()
    private val settBehandlingPåVentService = mockk<SettBehandlingPåVentService>(relaxed = true)
    private val validerBrevmottakerService = mockk<ValiderBrevmottakerService>(relaxed = true)
    private val brevmottakerService =
        spyk(
            BrevmottakerService(
                brevmottakerRepository = mockk(relaxed = true),
                loggService = mockk(),
                validerBrevmottakerService = mockk(),
            ),
        )
    private val featureToggleService = mockk<FeatureToggleService>()

    private val brevService =
        BrevService(
            integrasjonKlient = integrasjonKlient,
            loggService = loggService,
            taskService = taskService,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
            arbeidsfordelingService = arbeidsfordelingService,
            utgåendeJournalføringService = utgåendeJournalføringService,
            vilkårsvurderingService = vilkårsvurderingService,
            behandlingRepository = behandlingRepository,
            journalføringRepository = journalføringRepository,
            settBehandlingPåVentService = settBehandlingPåVentService,
            genererBrevService = genererBrevService,
            brevmottakerService = brevmottakerService,
            validerBrevmottakerService = validerBrevmottakerService,
            featureToggleService = featureToggleService,
        )

    private val søker = randomAktør()
    private val fagsak = lagFagsak(søker)
    private val behandling = lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val manueltBrevDto =
        ManueltBrevDto(
            brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
            mottakerIdent = søker.aktivFødselsnummer(),
            multiselectVerdier = listOf("Dokumentasjon som viser når barna kom til Norge."),
        )

    @BeforeEach
    fun setup() {
        every { featureToggleService.isEnabled(any<FeatureToggle>()) } returns false
    }

    @Test
    fun `hentForhåndsvisningAvBrev - skal hente pdf i form av en ByteArray fra genererBrevService`() {
        every { personopplysningGrunnlagService.hentSøker(behandlingId = behandling.id) } returns
            lagPerson(
                lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søker.aktivFødselsnummer()),
                søker,
            )
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling.id,
                behandlendeEnhetNavn = "Behandlende enhet",
                behandlendeEnhetId = "1234",
            )

        every { genererBrevService.genererManueltBrev(any(), any()) } returns ByteArray(10)

        brevService
            .hentForhåndsvisningAvBrev(
                manueltBrevDto.utvidManueltBrevDtoMedEnhetOgMottaker(
                    behandling.id,
                    personopplysningGrunnlagService,
                    arbeidsfordelingService,
                ),
            ).shouldNotBeNull()
    }

    @ParameterizedTest
    @EnumSource(
        value = Brevmal::class,
        names = ["INNHENTE_OPPLYSNINGER", "INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED", "VARSEL_OM_REVURDERING", "VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED", "VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `genererOgSendBrev - skal journalføre brev med forside for brevmaler som tilsier det`(brevmal: Brevmal) {
        every { personopplysningGrunnlagService.hentSøker(behandlingId = behandling.id) } returns
            lagPerson(
                lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søker.aktivFødselsnummer()),
                søker,
            )
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling.id,
                behandlendeEnhetNavn = "Behandlende enhet",
                behandlendeEnhetId = "1234",
            )

        every { vilkårsvurderingService.finnAktivVilkårsvurdering(any()) } returns
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.IKKE_VURDERT,
            )

        every { taskService.save(any()) } returns mockk()

        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling

        every { genererBrevService.genererManueltBrev(any(), any()) } returns ByteArray(10)

        val førstesideSlot = slot<Førsteside>()

        every {
            utgåendeJournalføringService.journalførDokument(
                any(),
                any(),
                any(),
                any(),
                any(),
                capture(førstesideSlot),
                any(),
                any(),
            )
        } returns "0"

        every { journalføringRepository.save(any()) } returns mockk()

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.IKKE_VURDERT,
            )

        every {
            settBehandlingPåVentService.settBehandlingPåVent(
                any(),
                any(),
                any(),
            )
        } just runs

        brevService.genererOgSendBrev(
            behandling.id,
            ManueltBrevDto(
                brevmal = brevmal,
                mottakerIdent = søker.aktivFødselsnummer(),
                barnasFødselsdager = emptyList(),
            ),
        )

        førstesideSlot.captured.shouldNotBeNull()
    }

    @ParameterizedTest
    @EnumSource(
        value = Brevmal::class,
        names = ["INFORMASJONSBREV_DELT_BOSTED", "HENLEGGE_TRUKKET_SØKNAD", "SVARTIDSBREV", "FORLENGET_SVARTIDSBREV", "INFORMASJONSBREV_KAN_SØKE", "INFORMASJONSBREV_KAN_SØKE_EØS"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `genererOgSendBrev - skal journalføre brev uten forside for brevmaler som tilsier det`(brevmal: Brevmal) {
        every { personopplysningGrunnlagService.hentSøker(behandlingId = behandling.id) } returns
            lagPerson(
                lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søker.aktivFødselsnummer()),
                søker,
            )
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling.id,
                behandlendeEnhetNavn = "Behandlende enhet",
                behandlendeEnhetId = "1234",
            )

        every { taskService.save(any()) } returns mockk()

        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling

        every { genererBrevService.genererManueltBrev(any(), any()) } returns ByteArray(10)

        every {
            utgåendeJournalføringService.journalførDokument(
                any(),
                any(),
                any(),
                any(),
                any(),
                null,
                any(),
                any(),
            )
        } returns "0"

        every { journalføringRepository.save(any()) } returns mockk()

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.IKKE_VURDERT,
            )

        every {
            settBehandlingPåVentService.settBehandlingPåVent(
                any(),
                any(),
                any(),
            )
        } just runs

        brevService.genererOgSendBrev(
            behandling.id,
            ManueltBrevDto(
                brevmal = brevmal,
                mottakerIdent = søker.aktivFødselsnummer(),
                barnasFødselsdager = emptyList(),
                behandlingKategori = BehandlingKategori.NASJONAL,
                antallUkerSvarfrist = 5,
            ),
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = Brevmal::class,
        names = ["INNHENTE_OPPLYSNINGER", "VARSEL_OM_REVURDERING"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `genererOgSendBrev - skal journalføre brev og legge til AnnenVurdering for søker i vilkårsvurderingen for INNHENTE_OPPLYSNINGER og VARSEL_OM_REVURDERING`(
        brevmal: Brevmal,
    ) {
        every { personopplysningGrunnlagService.hentSøker(behandlingId = behandling.id) } returns
            lagPerson(
                lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søker.aktivFødselsnummer()),
                søker,
            )
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling.id,
                behandlendeEnhetNavn = "Behandlende enhet",
                behandlendeEnhetId = "1234",
            )

        every { vilkårsvurderingService.finnAktivVilkårsvurdering(any()) } returns
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.IKKE_VURDERT,
            )

        every { taskService.save(any()) } returns mockk()

        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling

        every { genererBrevService.genererManueltBrev(any(), any()) } returns ByteArray(10)

        every {
            utgåendeJournalføringService.journalførDokument(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns "0"

        every { journalføringRepository.save(any()) } returns mockk()

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.IKKE_VURDERT,
            )

        every {
            settBehandlingPåVentService.settBehandlingPåVent(
                any(),
                any(),
                any(),
            )
        } just runs

        brevService.genererOgSendBrev(
            behandling.id,
            ManueltBrevDto(
                brevmal = brevmal,
                mottakerIdent = søker.aktivFødselsnummer(),
                barnasFødselsdager = emptyList(),
            ),
        )

        verify(exactly = 1) { vilkårsvurderingService.finnAktivVilkårsvurdering(any()) }
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
            "SVARTIDSBREV",
        ],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `genererOgSendBrev - skal journalføre brev og sette behandling på vent`(brevmal: Brevmal) {
        every { personopplysningGrunnlagService.hentSøker(behandlingId = behandling.id) } returns
            lagPerson(
                lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søker.aktivFødselsnummer()),
                søker,
            )
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling.id,
                behandlendeEnhetNavn = "Behandlende enhet",
                behandlendeEnhetId = "1234",
            )

        every { taskService.save(any()) } returns mockk()

        every { behandlingRepository.hentBehandling(behandlingId = behandling.id) } returns behandling

        every { genererBrevService.genererManueltBrev(any(), any()) } returns ByteArray(10)

        every {
            utgåendeJournalføringService.journalførDokument(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns "0"

        every { journalføringRepository.save(any()) } returns mockk()

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.IKKE_VURDERT,
            )

        every { vilkårsvurderingService.finnAktivVilkårsvurdering(any()) } returns
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.IKKE_VURDERT,
            )

        every {
            settBehandlingPåVentService.settBehandlingPåVent(
                any(),
                any(),
                any(),
            )
        } just runs

        brevService.genererOgSendBrev(
            behandling.id,
            ManueltBrevDto(
                brevmal = brevmal,
                mottakerIdent = søker.aktivFødselsnummer(),
                barnasFødselsdager = emptyList(),
                behandlingKategori = BehandlingKategori.NASJONAL,
                antallUkerSvarfrist = 5,
            ),
        )

        verify(exactly = 1) {
            settBehandlingPåVentService.settBehandlingPåVent(
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `sendBrev skal sende brev til bruker og manuelt registrert FULLMEKTIG på behandling`() {
        val behandling = lagBehandling()
        val søkersident = behandling.fagsak.aktør.aktivFødselsnummer()
        val manueltBrevRequest = ManueltBrevDto(mottakerIdent = søkersident, brevmal = Brevmal.SVARTIDSBREV)
        val avsenderMottaker = slot<AvsenderMottaker>()

        every { behandlingRepository.hentBehandling(any()) } returns behandling
        every { genererBrevService.genererManueltBrev(any(), any()) } returns ByteArray(10)
        every { brevmottakerService.hentBrevmottakere(behandling.id) } returns
            listOf(
                BrevmottakerDto(
                    id = 1L,
                    type = MottakerType.FULLMEKTIG,
                    navn = "Fullmektig navn",
                    adresselinje1 = "Test adresse",
                    postnummer = "0000",
                    poststed = "Oslo",
                    landkode = "NO",
                ),
            )
        every { brevmottakerService.lagMottakereFraBrevMottakere(any()) } answers { callOriginal() }
        every {
            utgåendeJournalføringService.journalførDokument(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                førsteside = any(),
                avsenderMottaker = any(),
                eksternReferanseId = any(),
            )
        } returns "mockJournalPostId" andThen "mockJournalPostId1"

        every { journalføringRepository.save(any()) } returns mockk()
        every { taskService.save(any()) } returns mockk()

        brevService.sendBrev(behandling.fagsak, behandling.id, manueltBrevRequest)

        verify(exactly = 2) { journalføringRepository.save(any()) }
        verify(exactly = 2) { taskService.save(any()) }
        verify(exactly = 1) {
            utgåendeJournalføringService.journalførDokument(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                førsteside = any(),
                avsenderMottaker = null,
                eksternReferanseId = any(),
            )
        }
        verify(exactly = 1) {
            utgåendeJournalføringService.journalførDokument(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                førsteside = any(),
                avsenderMottaker = capture(avsenderMottaker),
                eksternReferanseId = any(),
            )
        }
        assertEquals("Fullmektig navn", avsenderMottaker.captured.navn)
    }

    @Test
    fun `sendBrev skal sende brev til FULLMEKTIG og bruker ved manuell brevmottaker på fagsak`() {
        val aktør = randomAktør()
        val fagsak = Fagsak(aktør = aktør)
        val søkersident = aktør.aktivFødselsnummer()
        val brevmottakere =
            listOf(
                BrevmottakerDto(
                    id = null,
                    type = MottakerType.FULLMEKTIG,
                    navn = "Fullmektig navn",
                    adresselinje1 = "Test adresse",
                    postnummer = "0000",
                    poststed = "Oslo",
                    landkode = "NO",
                ),
            )
        val manueltBrevDto =
            ManueltBrevDto(
                mottakerIdent = søkersident,
                brevmal = Brevmal.SVARTIDSBREV,
                manuelleBrevmottakere = brevmottakere,
            )
        val avsenderMottaker = slot<AvsenderMottaker>()

        every { genererBrevService.genererManueltBrev(any(), any()) } returns ByteArray(10)
        every {
            utgåendeJournalføringService.journalførDokument(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                førsteside = any(),
                avsenderMottaker = any(),
                eksternReferanseId = any(),
            )
        } returns "mockJournalPostId" andThen "mockJournalPostId1"

        every { journalføringRepository.save(any()) } returns mockk()
        every { taskService.save(any()) } returns mockk()

        brevService.sendBrev(fagsak, behandlingId = null, manueltBrevDto)

        verify(exactly = 2) { taskService.save(any()) }
        verify(exactly = 1) {
            utgåendeJournalføringService.journalførDokument(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                førsteside = any(),
                avsenderMottaker = capture(avsenderMottaker),
                eksternReferanseId = any(),
            )
        }
        verify(exactly = 1) {
            utgåendeJournalføringService.journalførDokument(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                førsteside = any(),
                avsenderMottaker = null,
                eksternReferanseId = any(),
            )
        }

        assertEquals("Fullmektig navn", avsenderMottaker.captured.navn)
    }

    @Test
    fun `sendBrev skal kaste feil dersom manuelle brevmottakere er ugyldige`() {
        // Arrange
        val aktør = randomAktør()
        val fagsak = Fagsak(aktør = aktør)
        val søkersident = aktør.aktivFødselsnummer()
        val brevmottakere =
            listOf(
                lagBrevmottakerDto(id = 1234, postnummer = "0661", poststed = "Stockholm", landkode = "SE"),
            )
        val manueltBrevDto =
            ManueltBrevDto(
                mottakerIdent = søkersident,
                brevmal = Brevmal.SVARTIDSBREV,
                manuelleBrevmottakere = brevmottakere,
            )

        every { genererBrevService.genererManueltBrev(any(), any()) } returns ByteArray(10)

        // Act & assert
        val exception =
            assertThrows<FunksjonellFeil> {
                brevService.sendBrev(fagsak, behandlingId = null, manueltBrevDto)
            }

        assertThat(exception.message).isEqualTo("Det finnes ugyldige brevmottakere i utsending av manuelt brev")
    }

    @Nested
    inner class SendBrevNy {
        @Test
        fun `skal lage journalfør manuelt brev task for bruker og manuelt registrert fullmektig på behandling`() {
            // Arrange
            val behandling = lagBehandling()
            val manueltBrevDto = ManueltBrevDto(mottakerIdent = behandling.fagsak.aktør.aktivFødselsnummer(), brevmal = Brevmal.SVARTIDSBREV)
            val brevmottakerDto = lagBrevmottakerDto(type = MottakerType.FULLMEKTIG)

            val tasks = mutableListOf<Task>()
            every { behandlingRepository.hentBehandling(any()) } returns behandling
            every { brevmottakerService.hentBrevmottakere(behandling.id) } returns listOf(brevmottakerDto)
            every { brevmottakerService.lagMottakereFraBrevMottakere(any()) } answers { callOriginal() }
            every { taskService.save(capture(tasks)) } returnsArgument 0

            // Act
            brevService.sendBrevNy(behandling.fagsak, behandling.id, manueltBrevDto)

            // Assert
            verify(exactly = 2) { taskService.save(any()) }
            assertThat(tasks).hasSize(2)
            assertThat(tasks).anySatisfy {
                assertThat(it.type).isEqualTo(JournalførManueltBrevTask.TASK_STEP_TYPE)
                assertThat(it.metadata["fagsakId"]).isEqualTo(behandling.fagsak.id.toString())
                assertThat(it.metadata["behandlingId"]).isEqualTo(behandling.id.toString())
                assertThat(it.metadata["mottakerType"]).isEqualTo(Bruker::class.simpleName)
            }
            assertThat(tasks).anySatisfy {
                assertThat(it.type).isEqualTo(JournalførManueltBrevTask.TASK_STEP_TYPE)
                assertThat(it.metadata["fagsakId"]).isEqualTo(behandling.fagsak.id.toString())
                assertThat(it.metadata["behandlingId"]).isEqualTo(behandling.id.toString())
                assertThat(it.metadata["mottakerType"]).isEqualTo(FullmektigEllerVerge::class.simpleName)
            }
        }

        @Test
        fun `skal kaste feil dersom manuelle brevmottakere er ugyldige`() {
            // Arrange
            val aktør = randomAktør()
            val fagsak = Fagsak(aktør = aktør)
            val søkersident = aktør.aktivFødselsnummer()
            val brevmottakere =
                listOf(
                    lagBrevmottakerDto(id = 1234, postnummer = "0661", poststed = "Stockholm", landkode = "SE"),
                )
            val manueltBrevDto =
                ManueltBrevDto(
                    mottakerIdent = søkersident,
                    brevmal = Brevmal.SVARTIDSBREV,
                    manuelleBrevmottakere = brevmottakere,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    brevService.sendBrevNy(fagsak, behandlingId = null, manueltBrevDto)
                }
            assertThat(exception.message).isEqualTo("Det finnes ugyldige brevmottakere i utsending av manuelt brev")
        }
    }
}
