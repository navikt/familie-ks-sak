package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.api.dto.Bruker
import no.nav.familie.ks.sak.api.dto.DistribuerBrevDto
import no.nav.familie.ks.sak.api.dto.tilAvsenderMottaker
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagManueltBrevDto
import no.nav.familie.ks.sak.integrasjon.distribuering.DistribuerBrevTask
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService.Companion.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.DbJournalpost
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.DbJournalpostType
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringRepository
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class JournalførManueltBrevTaskTest {
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val genererBrevService = mockk<GenererBrevService>()
    private val utgåendeJournalføringService = mockk<UtgåendeJournalføringService>()
    private val journalføringRepository = mockk<JournalføringRepository>()
    private val taskService = mockk<TaskService>()
    private val journalførManueltBrevTask =
        JournalførManueltBrevTask(
            fagsakService = fagsakService,
            behandlingService = behandlingService,
            genererBrevService = genererBrevService,
            utgåendeJournalføringService = utgåendeJournalføringService,
            journalføringRepository = journalføringRepository,
            taskService = taskService,
        )

    @Nested
    inner class DoTask {
        @Test
        fun `skal journalføre manuelt brev og opprette distribuer brev task uten forside`() {
            // Arrange
            val journalpostId = "1"
            val fagsak = lagFagsak(id = 321L)
            val behandling = lagBehandling(fagsak = fagsak, id = 123L)
            val brev = ByteArray(0)
            val mottakerInfo = Bruker()
            val manueltBrevDto =
                lagManueltBrevDto(
                    enhet = Enhet("1", "Oslo"),
                    brevmal = Brevmal.ENDRING_AV_FRAMTIDIG_OPPHØR,
                )

            val dbJournalpostSlot = slot<DbJournalpost>()
            val taskSlot = slot<Task>()

            every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
            every { behandlingService.hentBehandling(behandling.id) } returns behandling
            every {
                genererBrevService.genererManueltBrev(
                    manueltBrevRequest = manueltBrevDto,
                    saksbehandlerSignaturTilBrev = "Olaf Test",
                )
            } returns brev
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
            } returns journalpostId
            every { journalføringRepository.save(capture(dbJournalpostSlot)) } returnsArgument 0
            every { taskService.save(capture(taskSlot)) } returnsArgument 0

            // Act
            val task =
                JournalførManueltBrevTask.opprettTask(
                    behandlingId = behandling.id,
                    fagsakId = fagsak.id,
                    manueltBrevDto = manueltBrevDto,
                    mottakerInfo = mottakerInfo,
                    saksbehandlerSignaturTilBrev = "Olaf Test",
                )

            // Act
            journalførManueltBrevTask.doTask(task)

            // Assert
            verify(exactly = 1) { fagsakService.hentFagsak(fagsak.id) }
            verify(exactly = 1) {
                genererBrevService.genererManueltBrev(
                    manueltBrevRequest = manueltBrevDto,
                    saksbehandlerSignaturTilBrev = "Olaf Test",
                )
            }
            verify(exactly = 1) {
                utgåendeJournalføringService.journalførDokument(
                    fnr = eq(fagsak.aktør.aktivFødselsnummer()),
                    fagsakId = eq(fagsak.id),
                    journalførendeEnhet = eq(manueltBrevDto.enhet!!.enhetId),
                    brev =
                        match { dokumenter ->
                            dokumenter.size == 1 &&
                                dokumenter[0].dokument == brev &&
                                dokumenter[0].filtype == Filtype.PDFA &&
                                dokumenter[0].dokumenttype == Dokumenttype.KONTANTSTØTTE_ENDRING_AV_FRAMTIDIG_OPPHØR
                        },
                    førsteside = isNull(),
                    avsenderMottaker = isNull(),
                    eksternReferanseId = eq("${fagsak.id}_${behandling.id}_null"),
                )
            }
            verify(exactly = 1) { journalføringRepository.save(any()) }
            val dbJournalpost = dbJournalpostSlot.captured
            assertThat(dbJournalpost.behandling).isEqualTo(behandling)
            assertThat(dbJournalpost.journalpostId).isEqualTo(journalpostId)
            assertThat(dbJournalpost.type).isEqualTo(DbJournalpostType.U)
            val distribuerBrevTask = taskSlot.captured
            verify(exactly = 1) { taskService.save(eq(distribuerBrevTask)) }
            val distribuerBrevDto = objectMapper.readValue(distribuerBrevTask.payload, DistribuerBrevDto::class.java)
            assertThat(distribuerBrevDto.behandlingId).isEqualTo(behandling.id)
            assertThat(distribuerBrevDto.journalpostId).isEqualTo(journalpostId)
            assertThat(distribuerBrevDto.brevmal).isEqualTo(manueltBrevDto.brevmal)
            assertThat(distribuerBrevDto.erManueltSendt).isTrue()
            assertThat(distribuerBrevDto.manuellAdresseInfo).isEqualTo(mottakerInfo.manuellAdresseInfo)
            assertThat(distribuerBrevTask.type).isEqualTo(DistribuerBrevTask.TASK_STEP_TYPE)
            assertThat(distribuerBrevTask.metadata["fagsakIdent"]).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(distribuerBrevTask.metadata["mottakerIdent"]).isEqualTo(manueltBrevDto.mottakerIdent)
            assertThat(distribuerBrevTask.metadata["journalpostId"]).isEqualTo(journalpostId)
            assertThat(distribuerBrevTask.metadata["fagsakId"]).isEqualTo(fagsak.id.toString())
            assertThat(distribuerBrevTask.metadata["behandlingId"]).isEqualTo(behandling.id.toString())
            assertThat(distribuerBrevTask.metadata["mottakerType"]).isEqualTo(task.metadata["mottakerType"])
        }

        @Test
        fun `skal journalføre manuelt brev og opprette distribuer brev task med forside`() {
            // Arrange
            val fagsakId = 321L
            val behandlingId = 123L
            val journalpostId = "1"
            val fagsak = lagFagsak(id = fagsakId)
            val behandling = lagBehandling(fagsak = fagsak, id = behandlingId)
            val brev = ByteArray(0)
            val mottakerInfo = Bruker()
            val manueltBrevDto = lagManueltBrevDto(brevmal = Brevmal.INNHENTE_OPPLYSNINGER)

            val dbJournalpostSlot = slot<DbJournalpost>()
            val taskSlot = slot<Task>()

            every { fagsakService.hentFagsak(fagsakId) } returns fagsak
            every { behandlingService.hentBehandling(behandlingId) } returns behandling
            every {
                genererBrevService.genererManueltBrev(
                    manueltBrevRequest = manueltBrevDto,
                    saksbehandlerSignaturTilBrev = "Olaf Test",
                )
            } returns brev
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
            } returns journalpostId
            every { journalføringRepository.save(capture(dbJournalpostSlot)) } returnsArgument 0
            every { taskService.save(capture(taskSlot)) } returnsArgument 0

            // Act
            val task =
                JournalførManueltBrevTask.opprettTask(
                    behandlingId = behandlingId,
                    fagsakId = fagsakId,
                    manueltBrevDto = manueltBrevDto,
                    mottakerInfo = mottakerInfo,
                    saksbehandlerSignaturTilBrev = "Olaf Test",
                )

            // Act
            journalførManueltBrevTask.doTask(task)

            // Assert
            verify(exactly = 1) { fagsakService.hentFagsak(fagsakId) }
            verify(exactly = 1) {
                genererBrevService.genererManueltBrev(
                    manueltBrevRequest = manueltBrevDto,
                    saksbehandlerSignaturTilBrev = "Olaf Test",
                )
            }
            verify(exactly = 1) {
                utgåendeJournalføringService.journalførDokument(
                    fnr = eq(fagsak.aktør.aktivFødselsnummer()),
                    fagsakId = eq(fagsak.id),
                    journalførendeEnhet = eq(DEFAULT_JOURNALFØRENDE_ENHET),
                    brev =
                        match { dokumenter ->
                            dokumenter.size == 1 &&
                                dokumenter[0].dokument == brev &&
                                dokumenter[0].filtype == Filtype.PDFA &&
                                dokumenter[0].dokumenttype == Dokumenttype.KONTANTSTØTTE_INNHENTE_OPPLYSNINGER
                        },
                    førsteside =
                        match { førsteside ->
                            førsteside.språkkode == Språkkode.NB &&
                                førsteside.navSkjemaId == JournalførManueltBrevTask.FØRSTESIDE_NAV_SKJEMA_ID &&
                                førsteside.overskriftstittel == JournalførManueltBrevTask.FØRSTESIDE_OVERSKRIFTSTITTEL
                        },
                    avsenderMottaker = isNull(),
                    eksternReferanseId = eq("${fagsakId}_${behandlingId}_null"),
                )
            }
            verify(exactly = 1) { journalføringRepository.save(any()) }
            val dbJournalpost = dbJournalpostSlot.captured
            assertThat(dbJournalpost.behandling).isEqualTo(behandling)
            assertThat(dbJournalpost.journalpostId).isEqualTo(journalpostId)
            assertThat(dbJournalpost.type).isEqualTo(DbJournalpostType.U)
            val distribuerBrevTask = taskSlot.captured
            verify(exactly = 1) { taskService.save(eq(distribuerBrevTask)) }
            val distribuerBrevDto = objectMapper.readValue(distribuerBrevTask.payload, DistribuerBrevDto::class.java)
            assertThat(distribuerBrevDto.behandlingId).isEqualTo(behandlingId)
            assertThat(distribuerBrevDto.journalpostId).isEqualTo(journalpostId)
            assertThat(distribuerBrevDto.brevmal).isEqualTo(manueltBrevDto.brevmal)
            assertThat(distribuerBrevDto.erManueltSendt).isTrue()
            assertThat(distribuerBrevDto.manuellAdresseInfo).isEqualTo(mottakerInfo.manuellAdresseInfo)
            assertThat(distribuerBrevTask.type).isEqualTo(DistribuerBrevTask.TASK_STEP_TYPE)
            assertThat(distribuerBrevTask.metadata["fagsakIdent"]).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(distribuerBrevTask.metadata["mottakerIdent"]).isEqualTo(manueltBrevDto.mottakerIdent)
            assertThat(distribuerBrevTask.metadata["journalpostId"]).isEqualTo(journalpostId)
            assertThat(distribuerBrevTask.metadata["fagsakId"]).isEqualTo(fagsak.id.toString())
            assertThat(distribuerBrevTask.metadata["behandlingId"]).isEqualTo(behandlingId.toString())
            assertThat(distribuerBrevTask.metadata["mottakerType"]).isEqualTo(task.metadata["mottakerType"])
        }

        @Test
        fun `skal journalføre manuelt brev og opprette distribuer brev task uten behandling`() {
            // Arrange
            val journalpostId = "1"
            val fagsak = lagFagsak(id = 321L)
            val brev = ByteArray(0)
            val manueltBrevDto = lagManueltBrevDto(brevmal = Brevmal.ENDRING_AV_FRAMTIDIG_OPPHØR)
            val mottakerInfo = Bruker()

            val taskSlot = slot<Task>()
            every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
            every { genererBrevService.genererManueltBrev(manueltBrevDto, false, "Olaf Test") } returns brev
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
            } returns journalpostId
            every { taskService.save(capture(taskSlot)) } returnsArgument 0

            // Act
            val task =
                JournalførManueltBrevTask.opprettTask(
                    behandlingId = null,
                    fagsakId = fagsak.id,
                    manueltBrevDto = manueltBrevDto,
                    mottakerInfo = mottakerInfo,
                    saksbehandlerSignaturTilBrev = "Olaf Test",
                )

            // Act
            journalførManueltBrevTask.doTask(task)

            // Assert
            verify(exactly = 1) { fagsakService.hentFagsak(fagsak.id) }
            verify(exactly = 1) { genererBrevService.genererManueltBrev(manueltBrevDto, false, "Olaf Test") }
            verify(exactly = 1) {
                utgåendeJournalføringService.journalførDokument(
                    fnr = eq(fagsak.aktør.aktivFødselsnummer()),
                    fagsakId = eq(fagsak.id),
                    journalførendeEnhet = eq(DEFAULT_JOURNALFØRENDE_ENHET),
                    brev =
                        match { dokumenter ->
                            dokumenter.size == 1 &&
                                dokumenter[0].dokument == brev &&
                                dokumenter[0].filtype == Filtype.PDFA &&
                                dokumenter[0].dokumenttype == Dokumenttype.KONTANTSTØTTE_ENDRING_AV_FRAMTIDIG_OPPHØR
                        },
                    førsteside = isNull(),
                    avsenderMottaker = isNull(),
                    eksternReferanseId = eq("${fagsak.id}_null_null"),
                )
            }
            verify(exactly = 0) { journalføringRepository.save(any()) }
            val distribuerBrevTask = taskSlot.captured
            verify(exactly = 1) { taskService.save(eq(distribuerBrevTask)) }
            val distribuerBrevDto = objectMapper.readValue(distribuerBrevTask.payload, DistribuerBrevDto::class.java)
            assertThat(distribuerBrevDto.behandlingId).isNull()
            assertThat(distribuerBrevDto.journalpostId).isEqualTo(journalpostId)
            assertThat(distribuerBrevDto.brevmal).isEqualTo(manueltBrevDto.brevmal)
            assertThat(distribuerBrevDto.erManueltSendt).isTrue()
            assertThat(distribuerBrevDto.manuellAdresseInfo).isEqualTo(mottakerInfo.manuellAdresseInfo)
            assertThat(distribuerBrevTask.type).isEqualTo(DistribuerBrevTask.TASK_STEP_TYPE)
            assertThat(distribuerBrevTask.metadata["fagsakIdent"]).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(distribuerBrevTask.metadata["mottakerIdent"]).isEqualTo(manueltBrevDto.mottakerIdent)
            assertThat(distribuerBrevTask.metadata["journalpostId"]).isEqualTo(journalpostId)
            assertThat(distribuerBrevTask.metadata["fagsakId"]).isEqualTo(fagsak.id.toString())
            assertThat(distribuerBrevTask.metadata["behandlingId"]).isEqualTo("null")
            assertThat(distribuerBrevTask.metadata["mottakerType"]).isEqualTo(task.metadata["mottakerType"])
        }
    }

    @Nested
    inner class OpprettTask {
        @Test
        fun `skal opprette task`() {
            // Arrange
            val fagsakId = 321L
            val behandlingId = 123L
            val manueltBrevDto = lagManueltBrevDto()
            val mottakerInfo = Bruker()

            // Act
            val task =
                JournalførManueltBrevTask.opprettTask(
                    behandlingId = behandlingId,
                    fagsakId = fagsakId,
                    manueltBrevDto = manueltBrevDto,
                    mottakerInfo = mottakerInfo,
                    saksbehandlerSignaturTilBrev = "Olaf Test",
                )

            // Assert
            assertThat(task.type).isEqualTo(JournalførManueltBrevTask.TASK_STEP_TYPE)
            assertThat(task.payload).isNotNull()
            val dto = objectMapper.readValue(task.payload, JournalførManueltBrevDto::class.java)
            assertThat(dto.fagsakId).isEqualTo(fagsakId)
            assertThat(dto.behandlingId).isEqualTo(behandlingId)
            assertThat(dto.manueltBrevDto).isEqualTo(manueltBrevDto)
            assertThat(dto.avsenderMottaker).isEqualTo(mottakerInfo.tilAvsenderMottaker())
            assertThat(dto.manuellAdresseInfo).isEqualTo(mottakerInfo.manuellAdresseInfo)
            assertThat(dto.eksternReferanseId).contains("${fagsakId}_${behandlingId}_")
            assertThat(task.metadata["fagsakId"]).isEqualTo(fagsakId.toString())
            assertThat(task.metadata["behandlingId"]).isEqualTo(behandlingId.toString())
            assertThat(task.metadata["brevmal"]).isEqualTo(manueltBrevDto.brevmal.name)
            assertThat(task.metadata["mottakerType"]).isEqualTo(Bruker::class.simpleName)
        }
    }
}
