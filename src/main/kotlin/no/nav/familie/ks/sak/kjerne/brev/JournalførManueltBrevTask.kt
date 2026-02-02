package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.ks.sak.api.dto.DistribuerBrevDto
import no.nav.familie.ks.sak.api.dto.FullmektigEllerVerge
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.api.dto.MottakerInfo
import no.nav.familie.ks.sak.api.dto.tilAvsenderMottaker
import no.nav.familie.ks.sak.integrasjon.distribuering.DistribuerBrevTask
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService.Companion.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService.Companion.genererEksternReferanseIdForJournalpost
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.DbJournalpost
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.DbJournalpostType
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringRepository
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.Properties

@Component
@TaskStepBeskrivelse(
    taskStepType = JournalførManueltBrevTask.TASK_STEP_TYPE,
    beskrivelse = "Journalfører manuelt brev",
    maxAntallFeil = 1,
)
class JournalførManueltBrevTask(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val genererBrevService: GenererBrevService,
    private val utgåendeJournalføringService: UtgåendeJournalføringService,
    private val journalføringRepository: JournalføringRepository,
    private val taskService: TaskService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val dto = jsonMapper.readValue(task.payload, JournalførManueltBrevDto::class.java)
        logger.info("Journalfører manuelt brev for fagsak=${dto.fagsakId} og behandling=${dto.behandlingId}")

        val fagsak = fagsakService.hentFagsak(dto.fagsakId)
        val behandling = dto.behandlingId?.let { behandlingId -> behandlingService.hentBehandling(behandlingId) }
        val saksbehandlerSignaturTilBrev = dto.saksbehandlerSignaturTilBrev

        val generertBrev =
            genererBrevService.genererManueltBrev(
                manueltBrevRequest = dto.manueltBrevDto,
                erForhåndsvisning = false,
                saksbehandlerSignaturTilBrev = saksbehandlerSignaturTilBrev,
            )

        val førsteside =
            if (dto.manueltBrevDto.brevmal.skalGenerereForside()) {
                Førsteside(
                    språkkode = dto.manueltBrevDto.mottakerMålform.tilSpråkkode(),
                    navSkjemaId = FØRSTESIDE_NAV_SKJEMA_ID,
                    overskriftstittel = FØRSTESIDE_OVERSKRIFTSTITTEL,
                )
            } else {
                null
            }

        val journalpostId =
            utgåendeJournalføringService.journalførDokument(
                fnr = fagsak.aktør.aktivFødselsnummer(),
                fagsakId = fagsak.id,
                journalførendeEnhet = dto.manueltBrevDto.enhet?.enhetId ?: DEFAULT_JOURNALFØRENDE_ENHET,
                brev =
                    listOf(
                        Dokument(
                            dokument = generertBrev,
                            filtype = Filtype.PDFA,
                            dokumenttype = dto.manueltBrevDto.brevmal.tilFamilieKontrakterDokumentType(),
                        ),
                    ),
                førsteside = førsteside,
                avsenderMottaker = dto.avsenderMottaker,
                eksternReferanseId = dto.eksternReferanseId,
            )

        if (behandling != null) {
            journalføringRepository.save(
                DbJournalpost(
                    behandling = behandling,
                    journalpostId = journalpostId,
                    type = DbJournalpostType.U,
                ),
            )
        }

        taskService.save(
            DistribuerBrevTask
                .opprettDistribuerBrevTask(
                    distribuerBrevDTO =
                        DistribuerBrevDto(
                            behandlingId = behandling?.id,
                            journalpostId = journalpostId,
                            brevmal = dto.manueltBrevDto.brevmal,
                            erManueltSendt = true,
                            manuellAdresseInfo = dto.manuellAdresseInfo,
                        ),
                    properties =
                        Properties().apply {
                            this["fagsakIdent"] = fagsak.aktør.aktivFødselsnummer()
                            this["mottakerIdent"] = dto.manueltBrevDto.mottakerIdent
                            this["journalpostId"] = journalpostId
                            this["fagsakId"] = fagsak.id.toString()
                            this["behandlingId"] = behandling?.id.toString()
                            this["mottakerType"] = task.metadata["mottakerType"]
                        },
                ),
        )
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(JournalførManueltBrevTask::class.java)
        const val TASK_STEP_TYPE = "journalførManueltBrev"
        const val FØRSTESIDE_NAV_SKJEMA_ID = "NAV 34-00.07"
        const val FØRSTESIDE_OVERSKRIFTSTITTEL =
            "Ettersendelse til søknad om kontantstøtte til småbarnsforeldre NAV 34-00.07"

        fun opprettTask(
            behandlingId: Long?,
            fagsakId: Long,
            manueltBrevDto: ManueltBrevDto,
            mottakerInfo: MottakerInfo,
            saksbehandlerSignaturTilBrev: String,
        ): Task {
            val dto =
                JournalførManueltBrevDto(
                    fagsakId = fagsakId,
                    behandlingId = behandlingId,
                    manueltBrevDto = manueltBrevDto,
                    avsenderMottaker = mottakerInfo.tilAvsenderMottaker(),
                    manuellAdresseInfo = mottakerInfo.manuellAdresseInfo,
                    eksternReferanseId =
                        genererEksternReferanseIdForJournalpost(
                            fagsakId,
                            behandlingId,
                            mottakerInfo is FullmektigEllerVerge,
                        ),
                    saksbehandlerSignaturTilBrev = saksbehandlerSignaturTilBrev,
                )

            val properties =
                Properties().apply {
                    this["fagsakId"] = fagsakId.toString()
                    this["behandlingId"] = behandlingId.toString()
                    this["brevmal"] = manueltBrevDto.brevmal.name
                    this["mottakerType"] = mottakerInfo::class.simpleName
                }

            return Task(
                type = TASK_STEP_TYPE,
                payload = jsonMapper.writeValueAsString(dto),
                properties = properties,
            )
        }
    }
}
