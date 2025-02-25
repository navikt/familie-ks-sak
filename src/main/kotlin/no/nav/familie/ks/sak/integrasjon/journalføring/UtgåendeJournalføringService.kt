package no.nav.familie.ks.sak.integrasjon.journalføring

import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class UtgåendeJournalføringService(
    private val integrasjonClient: IntegrasjonClient,
) {
    fun journalførDokument(
        fnr: String,
        fagsakId: Long,
        journalførendeEnhet: String? = null,
        brev: List<Dokument>,
        vedlegg: List<Dokument> = emptyList(),
        førsteside: Førsteside? = null,
        behandlingId: Long? = null,
        tilVergeEllerFullmektig: Boolean = false,
        avsenderMottaker: AvsenderMottaker? = null,
    ): String {
        if (journalførendeEnhet == DEFAULT_JOURNALFØRENDE_ENHET) {
            logger.warn("Informasjon om enhet mangler på bruker og er satt til fallback-verdi, $DEFAULT_JOURNALFØRENDE_ENHET")
        }

        val eksternReferanseId = genererEksternReferanseIdForJournalpost(fagsakId, behandlingId, tilVergeEllerFullmektig)

        val journalpostId =
            try {
                val journalpost =
                    integrasjonClient.journalførDokument(
                        ArkiverDokumentRequest(
                            fnr = fnr,
                            forsøkFerdigstill = true,
                            hoveddokumentvarianter = brev,
                            vedleggsdokumenter = vedlegg,
                            fagsakId = fagsakId.toString(),
                            journalførendeEnhet = journalførendeEnhet,
                            førsteside = førsteside,
                            eksternReferanseId = eksternReferanseId,
                            avsenderMottaker = avsenderMottaker,
                        ),
                    )

                if (!journalpost.ferdigstilt) {
                    throw Feil("Klarte ikke ferdigstille journalpost med id ${journalpost.journalpostId}")
                }

                journalpost.journalpostId
            } catch (ressursException: RessursException) {
                when (ressursException.httpStatus) {
                    HttpStatus.CONFLICT -> {
                        logger.warn(
                            "Klarte ikke journalføre dokument på fagsak=$fagsakId fordi det allerede finnes en journalpost " +
                                "med eksternReferanseId=$eksternReferanseId. Bruker eksisterende journalpost.",
                        )
                        hentEksisterendeJournalpost(eksternReferanseId, fnr)
                    }

                    else -> throw ressursException
                }
            }

        return journalpostId
    }

    private fun hentEksisterendeJournalpost(
        eksternReferanseId: String,
        fnr: String,
    ): String =
        integrasjonClient
            .hentJournalposterForBruker(
                JournalposterForBrukerRequest(
                    brukerId = Bruker(id = fnr, type = BrukerIdType.FNR),
                    antall = 50,
                ),
            ).single { it.eksternReferanseId == eksternReferanseId }
            .journalpostId

    private fun genererEksternReferanseIdForJournalpost(
        fagsakId: Long,
        behandlingId: Long?,
        tilVergeEllerFullmektig: Boolean,
    ) = "${fagsakId}_${behandlingId}${if (tilVergeEllerFullmektig) "_tilleggsmottaker" else ""}_${MDC.get(MDCConstants.MDC_CALL_ID)}"

    companion object {
        const val DEFAULT_JOURNALFØRENDE_ENHET = "9999"
        private val logger = LoggerFactory.getLogger(UtgåendeJournalføringService::class.java)
    }
}
