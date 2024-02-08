package no.nav.familie.ks.sak.kjerne.behandling.steg.journalførvedtaksbrev

import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.ks.sak.api.dto.BehandlingStegDto
import no.nav.familie.ks.sak.api.dto.DistribuerBrevDto
import no.nav.familie.ks.sak.api.dto.JournalførVedtaksbrevDTO
import no.nav.familie.ks.sak.api.dto.tilAvsenderMottaker
import no.nav.familie.ks.sak.integrasjon.distribuering.DistribuerBrevTask
import no.nav.familie.ks.sak.integrasjon.distribuering.DistribuerVedtaksbrevTilVergeEllerFullmektigTask
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.tilDokumenttype
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.IBehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.brev.hentBrevmal
import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JournalførVedtaksbrevSteg(
    private val vedtakService: VedtakService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val utgåendeJournalføringService: UtgåendeJournalføringService,
    private val taskService: TaskService,
    private val fagsakService: FagsakService,
    private val brevmottakerService: BrevmottakerService,
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
                søkersIdent = søkersident,
            )

        val journalposterTilDistribusjon =
            mottakere.map { mottaker ->
                journalførVedtaksbrev(
                    fnr = søkersident,
                    fagsakId = fagsak.id,
                    vedtak = vedtak,
                    journalførendeEnhet = behandlendeEnhet,
                    tilVergeEllerFullmektig = mottaker.erVergeEllerFullmektig,
                    avsenderMottaker = mottaker.tilAvsenderMottaker(),
                ) to mottaker
            }

        journalposterTilDistribusjon.forEach { (journalpostId, mottaker) ->
            val distribuerBrevDto =
                DistribuerBrevDto(
                    personIdent = mottaker.brukerId,
                    behandlingId = vedtak.behandling.id,
                    journalpostId = journalpostId,
                    brevmal = hentBrevmal(vedtak.behandling),
                    erManueltSendt = false,
                    manuellAdresseInfo = mottaker.manuellAdresseInfo,
                )
            val distributerBrevTask =
                if (mottaker.erVergeEllerFullmektig) {
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
        val vedleggPdf = hentVedlegg(KONTANTSTØTTE_VEDTAK_VEDLEGG_FILNAVN)

        val brev =
            listOf(
                Dokument(
                    vedtak.stønadBrevPdf!!,
                    filtype = Filtype.PDFA,
                    dokumenttype = vedtak.behandling.resultat.tilDokumenttype(),
                    tittel = hentOverstyrtDokumenttittel(vedtak.behandling),
                ),
            )

        logger.info(
            "Journalfører vedtaksbrev ${
                if (tilVergeEllerFullmektig) "til verge/fullmektig" else ""
            } for behandling ${vedtak.behandling.id} med tittel ${
                hentOverstyrtDokumenttittel(vedtak.behandling)
            }",
        )

        val vedlegg =
            listOf(
                Dokument(
                    vedleggPdf,
                    filtype = Filtype.PDFA,
                    dokumenttype = Dokumenttype.BARNETRYGD_VEDLEGG,
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

    fun hentOverstyrtDokumenttittel(behandling: Behandling): String? {
        return if (behandling.type == BehandlingType.REVURDERING) {
            when (behandling.resultat) {
                Behandlingsresultat.INNVILGET, Behandlingsresultat.DELVIS_INNVILGET,
                Behandlingsresultat.INNVILGET_OG_ENDRET,
                Behandlingsresultat.INNVILGET_OG_OPPHØRT,
                Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT,
                Behandlingsresultat.ENDRET_OG_OPPHØRT,
                -> "Vedtak om endret barnetrygd"

                Behandlingsresultat.FORTSATT_INNVILGET -> "Vedtak om fortsatt barnetrygd"
                else -> null
            }
        } else {
            null
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(JournalførVedtaksbrevSteg::class.java)

        const val KONTANTSTØTTE_VEDTAK_VEDLEGG_FILNAVN = "NAV_34-0005bm08-2018.pdf"
        const val KONTANTSTØTTE_VEDTAK_VEDLEGG_TITTEL = "Stønadsmottakerens rettigheter og plikter (Kontantstøtte)"

        private fun hentVedlegg(vedleggsnavn: String): ByteArray {
            val inputStream =
                this::class.java.classLoader.getResourceAsStream("dokumenter/$vedleggsnavn")
                    ?: error("Klarte ikke hente vedlegg $vedleggsnavn")

            return inputStream.readAllBytes()
        }
    }
}
