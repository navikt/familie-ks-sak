package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.ks.sak.api.dto.BehandlingStegDto
import no.nav.familie.ks.sak.api.dto.JournalførVedtaksbrevDTO
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.tilDokumenttype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JournalførVedtaksbrevSteg(
    private val vedtakService: VedtakService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val utgåendeJournalføringService: UtgåendeJournalføringService,
    private val fagsakService: FagsakService
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.JOURNALFØR_VEDTAKSBREV

    override fun utførSteg(behandlingId: Long, behandlingStegDto: BehandlingStegDto) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
        val journalførVedtaksbrevDTO = behandlingStegDto as JournalførVedtaksbrevDTO

        val vedtak = vedtakService.hentVedtak(vedtakId = journalførVedtaksbrevDTO.vedtakId)
        val fagsak = fagsakService.hentFagsak(vedtak.behandling.fagsak.id)

        val behandlendeEnhet = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId).behandlendeEnhetId

        journalførVedtaksbrev(
            fnr = fagsak.aktør.aktivFødselsnummer(),
            fagsakId = fagsak.id,
            vedtak = vedtak,
            journalførendeEnhet = behandlendeEnhet
        )

        // TODO: Legg inn oppretting av brev distribusjon task når man starter på distribusjon av brev
    }

    fun journalførVedtaksbrev(
        fnr: String,
        fagsakId: Long,
        vedtak: Vedtak,
        journalførendeEnhet: String
    ): String {
        val vedleggPdf = hentVedlegg(KONTANTSTØTTE_VEDTAK_VEDLEGG_FILNAVN)

        val brev = listOf(
            Dokument(
                vedtak.stønadBrevPdf!!,
                filtype = Filtype.PDFA,
                dokumenttype = vedtak.behandling.resultat.tilDokumenttype(),
                tittel = hentOverstyrtDokumenttittel(vedtak.behandling)
            )
        )

        logger.info(
            "Journalfører vedtaksbrev for behandling ${vedtak.behandling.id} med tittel ${
            hentOverstyrtDokumenttittel(vedtak.behandling)
            }"
        )

        val vedlegg = listOf(
            Dokument(
                vedleggPdf,
                filtype = Filtype.PDFA,
                dokumenttype = Dokumenttype.BARNETRYGD_VEDLEGG,
                tittel = KONTANTSTØTTE_VEDTAK_VEDLEGG_TITTEL
            )
        )

        return utgåendeJournalføringService.journalførDokument(
            fnr = fnr,
            fagsakId = fagsakId,
            journalførendeEnhet = journalførendeEnhet,
            brev = brev,
            vedlegg = vedlegg,
            behandlingId = vedtak.behandling.id
        )
    }

    fun hentOverstyrtDokumenttittel(behandling: Behandling): String? {
        return if (behandling.type == BehandlingType.REVURDERING) {
            when (behandling.resultat) {
                Behandlingsresultat.INNVILGET, Behandlingsresultat.DELVIS_INNVILGET,
                Behandlingsresultat.INNVILGET_OG_ENDRET,
                Behandlingsresultat.INNVILGET_OG_OPPHØRT,
                Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT,
                Behandlingsresultat.ENDRET_OG_OPPHØRT -> "Vedtak om endret barnetrygd"
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
            val inputStream = this::class.java.classLoader.getResourceAsStream("dokumenter/$vedleggsnavn")
                ?: error("Klarte ikke hente vedlegg $vedleggsnavn")

            return inputStream.readAllBytes()
        }
    }
}
