package no.nav.familie.ks.sak.kjerne.behandling.steg.journalførvedtaksbrev

import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.ks.sak.api.dto.BehandlingStegDto
import no.nav.familie.ks.sak.api.dto.DistribuerBrevDto
import no.nav.familie.ks.sak.api.dto.FullmektigEllerVerge
import no.nav.familie.ks.sak.api.dto.JournalførVedtaksbrevDTO
import no.nav.familie.ks.sak.api.dto.tilAvsenderMottaker
import no.nav.familie.ks.sak.common.util.hentDokument
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.integrasjon.distribuering.DistribuerBrevTask
import no.nav.familie.ks.sak.integrasjon.distribuering.DistribuerVedtaksbrevTilVergeEllerFullmektigTask
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.tilDokumenttype
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.IBehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.brev.BrevmalService
import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JournalførVedtaksbrevSteg(
    private val vedtakService: VedtakService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val utgåendeJournalføringService: UtgåendeJournalføringService,
    private val taskService: TaskRepositoryWrapper,
    private val fagsakService: FagsakService,
    private val brevmottakerService: BrevmottakerService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val brevmalService: BrevmalService,
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.JOURNALFØR_VEDTAKSBREV

    override fun utførSteg(
        behandlingId: Long,
        behandlingStegDto: BehandlingStegDto,
    ) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
        val journalførVedtaksbrevDTO = behandlingStegDto as JournalførVedtaksbrevDTO

        val vedtak = vedtakService.hentVedtak(vedtakId = journalførVedtaksbrevDTO.vedtakId)
        val fagsak = fagsakService.hentFagsak(vedtak.behandling.fagsak.id)

        val behandlendeEnhet = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId).behandlendeEnhetId

        val søkersident = fagsak.aktør.aktivFødselsnummer()
        val manueltRegistrerteMottakere = brevmottakerService.hentBrevmottakere(behandlingId)

        val mottakere =
            brevmottakerService.lagMottakereFraBrevMottakere(
                manueltRegistrerteMottakere = manueltRegistrerteMottakere,
            )

        val journalposterTilDistribusjon =
            mottakere.map { mottaker ->
                journalførVedtaksbrev(
                    fnr = søkersident,
                    fagsakId = fagsak.id,
                    vedtak = vedtak,
                    journalførendeEnhet = behandlendeEnhet,
                    tilVergeEllerFullmektig = mottaker is FullmektigEllerVerge,
                    avsenderMottaker = mottaker.tilAvsenderMottaker(),
                ) to mottaker
            }

        journalposterTilDistribusjon.forEach { (journalpostId, mottaker) ->
            val distribuerBrevDto =
                DistribuerBrevDto(
                    behandlingId = vedtak.behandling.id,
                    journalpostId = journalpostId,
                    brevmal = brevmalService.hentBrevmal(vedtak.behandling),
                    erManueltSendt = false,
                    manuellAdresseInfo = mottaker.manuellAdresseInfo,
                )
            val distributerBrevTask =
                if (mottaker is FullmektigEllerVerge) {
                    DistribuerVedtaksbrevTilVergeEllerFullmektigTask.opprettDistribuerVedtaksbrevTilVergeEllerFullmektigTask(
                        distribuerBrevDTO = distribuerBrevDto,
                        properties = journalførVedtaksbrevDTO.task.metadata,
                    )
                } else {
                    DistribuerBrevTask.opprettDistribuerBrevTask(
                        distribuerBrevDTO = distribuerBrevDto,
                        properties = journalførVedtaksbrevDTO.task.metadata,
                    )
                }
            taskService.save(distributerBrevTask)
        }
    }

    fun journalførVedtaksbrev(
        fnr: String,
        fagsakId: Long,
        vedtak: Vedtak,
        journalførendeEnhet: String,
        tilVergeEllerFullmektig: Boolean,
        avsenderMottaker: AvsenderMottaker?,
    ): String {
        val søkersMålform = personopplysningGrunnlagService.hentSøkersMålform(vedtak.behandling.id)

        val vedleggPdf =
            hentDokument(
                if (søkersMålform == Målform.NB) KONTANTSTØTTE_VEDTAK_BOKMÅL_VEDLEGG_FILNAVN else KONTANTSTØTTE_VEDTAK_NYNORSK_VEDLEGG_FILNAVN,
            )

        val dokumenttype = vedtak.behandling.resultat.tilDokumenttype()

        val brev =
            listOf(
                Dokument(
                    vedtak.stønadBrevPdf!!,
                    filtype = Filtype.PDFA,
                    dokumenttype = dokumenttype,
                ),
            )

        logger.info(
            "Journalfører vedtaksbrev ${
                if (tilVergeEllerFullmektig) "til verge/fullmektig" else ""
            } for behandling ${vedtak.behandling.id} med dokumenttype $dokumenttype",
        )

        val vedlegg =
            listOf(
                Dokument(
                    vedleggPdf,
                    filtype = Filtype.PDFA,
                    dokumenttype = Dokumenttype.KONTANTSTØTTE_VEDLEGG,
                    tittel = KONTANTSTØTTE_VEDTAK_VEDLEGG_TITTEL,
                ),
            )

        return utgåendeJournalføringService.journalførDokument(
            fnr = fnr,
            fagsakId = fagsakId,
            journalførendeEnhet = journalførendeEnhet,
            brev = brev,
            vedlegg = vedlegg,
            behandlingId = vedtak.behandling.id,
            tilVergeEllerFullmektig = tilVergeEllerFullmektig,
            avsenderMottaker = avsenderMottaker,
        )
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(JournalførVedtaksbrevSteg::class.java)

        const val KONTANTSTØTTE_VEDTAK_BOKMÅL_VEDLEGG_FILNAVN = "NAV_34-0005bm08-2024.pdf"
        const val KONTANTSTØTTE_VEDTAK_NYNORSK_VEDLEGG_FILNAVN = "NAV_34-0005nn08-2024.pdf"
        const val KONTANTSTØTTE_VEDTAK_VEDLEGG_TITTEL = "Stønadsmottakerens rettigheter og plikter (Kontantstøtte)"
    }
}
