package no.nav.familie.ks.sak.integrasjon.journalføring

import FerdigstillOppgaveKnyttJournalpostDto
import jakarta.transaction.Transactional
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.LogiskVedleggRequest
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.Sak
import no.nav.familie.kontrakter.felles.journalpost.TilgangsstyrtJournalpost
import no.nav.familie.ks.sak.api.dto.FagsakRequestDto
import no.nav.familie.ks.sak.api.dto.JournalføringRequestDto
import no.nav.familie.ks.sak.api.dto.JournalpostDokumentDto
import no.nav.familie.ks.sak.api.dto.OppdaterJournalpostRequestDto
import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.api.dto.Sakstype
import no.nav.familie.ks.sak.api.dto.TilknyttetBehandling
import no.nav.familie.ks.sak.api.dto.tilOppdaterJournalpostRequestDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.DbJournalpost
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.DbJournalpostType
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringBehandlingstype
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringRepository
import no.nav.familie.ks.sak.integrasjon.secureLogger
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.OpprettBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.Søknadsinfo
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.klage.KlageService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class InnkommendeJournalføringService(
    private val integrasjonKlient: IntegrasjonKlient,
    private val fagsakService: FagsakService,
    private val opprettBehandlingService: OpprettBehandlingService,
    private val behandlingService: BehandlingService,
    private val journalføringRepository: JournalføringRepository,
    private val loggService: LoggService,
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,
    private val klageService: KlageService,
) {
    fun hentJournalposterForBruker(brukerId: String): List<TilgangsstyrtJournalpost> =
        integrasjonKlient
            .hentTilgangsstyrteJournalposterForBruker(
                JournalposterForBrukerRequest(
                    antall = 1000,
                    brukerId =
                        Bruker(
                            id = brukerId,
                            type = BrukerIdType.FNR,
                        ),
                    tema = listOf(Tema.KON),
                ),
            )

    fun hentJournalpost(journalpostId: String): Journalpost = integrasjonKlient.hentJournalpost(journalpostId)

    fun hentDokumentIJournalpost(
        journalpostId: String,
        dokumentId: String,
    ): ByteArray = integrasjonKlient.hentDokumentIJournalpost(dokumentId, journalpostId)

    @Transactional
    fun journalfør(
        request: JournalføringRequestDto,
        journalpostId: String,
        oppgaveId: String,
    ): String {
        val fagsakId = fagsakService.hentEllerOpprettFagsak(FagsakRequestDto(request.bruker.id)).id
        val tilknyttedeBehandlinger = request.tilknyttedeBehandlinger.toMutableList()
        val journalpost = integrasjonKlient.hentJournalpost(journalpostId)

        if (request.opprettOgKnyttTilNyBehandling) {
            if (request.nyBehandlingstype == JournalføringBehandlingstype.KLAGE) {
                val klageMottattDato = request.datoMottatt?.toLocalDate() ?: throw Feil("Dato mottatt ikke satt ved journalføring av journalpost med id=$journalpostId")
                val klageBehandlingId = klageService.opprettKlage(fagsakId, klageMottattDato)
                tilknyttedeBehandlinger.add(TilknyttetBehandling(behandlingstype = JournalføringBehandlingstype.KLAGE, behandlingId = klageBehandlingId.toString()))
            } else {
                val nyBehandling =
                    opprettBehandlingForJournalføring(
                        personIdent = request.bruker.id,
                        saksbehandlerIdent = request.navIdent,
                        type = request.nyBehandlingstype.tilBehandingType(),
                        årsak = request.nyBehandlingsårsak,
                        kategori = request.kategori,
                        søknadMottattDato = request.datoMottatt?.toLocalDate(),
                    )

                tilknyttedeBehandlinger.add(
                    TilknyttetBehandling(
                        behandlingstype = request.nyBehandlingstype,
                        behandlingId = nyBehandling.id.toString(),
                    ),
                )
            }
        }

        val kontantstøtteBehandlinger =
            tilknyttedeBehandlinger
                .filter { !it.behandlingstype.skalBehandlesIEksternApplikasjon() }
                .map { behandlingService.hentBehandling(it.behandlingId.toLong()) }

        knyttBehandlingerTilJournalpostOgLagreSøknadsinfo(
            kontantstøtteBehandlinger = kontantstøtteBehandlinger,
            journalpost = journalpost,
        )

        oppdaterLogiskeVedlegg(request.dokumenter)

        oppdaterOgFerdigstill(
            oppdaterJournalPostRequest =
                request.tilOppdaterJournalpostRequestDto(
                    sak =
                        Sak(
                            fagsakId = fagsakId.toString(),
                            fagsaksystem = Fagsystem.KONT.name,
                            sakstype = Sakstype.FAGSAK.type,
                        ),
                    journalpost = journalpost,
                ),
            journalpostId = journalpostId,
            behandlendeEnhet = request.journalførendeEnhet,
            oppgaveId = oppgaveId,
            kontantstøtteBehandlinger = kontantstøtteBehandlinger,
        )

        return fagsakId.toString()
    }

    fun knyttJournalpostTilFagsakOgFerdigstillOppgave(
        request: FerdigstillOppgaveKnyttJournalpostDto,
        oppgaveId: Long,
    ): String {
        val fagsakId = fagsakService.hentEllerOpprettFagsak(FagsakRequestDto(request.bruker.id)).id
        val tilknyttedeBehandlinger: MutableList<TilknyttetBehandling> = request.tilknyttedeBehandlinger.toMutableList()
        val journalpost = hentJournalpost(request.journalpostId)

        if (request.opprettOgKnyttTilNyBehandling) {
            if (request.nyBehandlingstype == JournalføringBehandlingstype.KLAGE) {
                val klageMottattDato = request.datoMottatt?.toLocalDate() ?: throw Feil("Dato mottatt ikke satt ved journalføring av journalpost med id=$request.journalpostId")
                val klageBehandlingId = klageService.opprettKlage(fagsakId, klageMottattDato)
                tilknyttedeBehandlinger.add(TilknyttetBehandling(behandlingstype = JournalføringBehandlingstype.KLAGE, behandlingId = klageBehandlingId.toString()))
            } else {
                if (request.navIdent == null || request.nyBehandlingstype == null || request.nyBehandlingsårsak == null || request.kategori == null) {
                    secureLogger.info("Obligatoriske felter er ikke sendt med ved oppretting av ny behandling: $request")
                    throw Feil("Obligatoriske felter er ikke sendt med for oppretting av ny behandling for journalpostId=${request.journalpostId}. Se secure logs for detaljer.")
                }
                val nyBehandling =
                    opprettBehandlingForJournalføring(
                        personIdent = request.bruker.id,
                        saksbehandlerIdent = request.navIdent,
                        type = request.nyBehandlingstype.tilBehandingType(),
                        årsak = request.nyBehandlingsårsak,
                        kategori = request.kategori,
                        søknadMottattDato = request.datoMottatt?.toLocalDate(),
                    )
                tilknyttedeBehandlinger.add(
                    TilknyttetBehandling(
                        behandlingstype = request.nyBehandlingstype,
                        behandlingId = nyBehandling.id.toString(),
                    ),
                )
            }
        }

        val kontantstøtteBehandlinger =
            tilknyttedeBehandlinger
                .filter { !it.behandlingstype.skalBehandlesIEksternApplikasjon() }
                .map { behandlingService.hentBehandling(it.behandlingId.toLong()) }

        knyttBehandlingerTilJournalpostOgLagreSøknadsinfo(
            kontantstøtteBehandlinger = kontantstøtteBehandlinger,
            journalpost = journalpost,
        )

        integrasjonKlient.ferdigstillOppgave(oppgaveId = oppgaveId)

        return fagsakId.toString()
    }

    private fun opprettBehandlingForJournalføring(
        personIdent: String,
        saksbehandlerIdent: String,
        type: BehandlingType,
        årsak: BehandlingÅrsak,
        kategori: BehandlingKategori? = null,
        søknadMottattDato: LocalDate? = null,
    ): Behandling {
        val nyBehandlingDto =
            OpprettBehandlingDto(
                kategori = kategori,
                søkersIdent = personIdent,
                behandlingType = type,
                behandlingÅrsak = årsak,
                saksbehandlerIdent = saksbehandlerIdent,
                søknadMottattDato = søknadMottattDato,
            )

        return opprettBehandlingService.opprettBehandling(nyBehandlingDto)
    }

    private fun knyttBehandlingerTilJournalpostOgLagreSøknadsinfo(
        kontantstøtteBehandlinger: List<Behandling>,
        journalpost: Journalpost,
    ) {
        val erSøknad = journalpost.dokumenter?.any { it.brevkode == SØKNADSKODE_KONTANTSTØTTE } ?: false

        // TODO: Finne ut hvordan man kan knytte journalpost til ekstern behandling, nå funker det kun med interne behandlinger
        kontantstøtteBehandlinger.forEach {
            journalføringRepository.save(
                DbJournalpost(
                    behandling = it,
                    journalpostId = journalpost.journalpostId,
                    type = DbJournalpostType.valueOf(journalpost.journalposttype.name),
                ),
            )
            if (erSøknad) {
                behandlingSøknadsinfoService.lagreNedSøknadsinfo(
                    søknadsinfo =
                        Søknadsinfo(
                            mottattDato = journalpost.datoMottatt ?: LocalDateTime.now(),
                            journalpostId = journalpost.journalpostId,
                            erDigital = journalpost.kanal == NAV_NO,
                        ),
                    behandling = it,
                )
            }
        }
    }

    private fun oppdaterLogiskeVedlegg(dokumenter: List<JournalpostDokumentDto>) {
        dokumenter.forEach { dokument ->
            val eksisterendeLogiskeVedlegg = dokument.eksisterendeLogiskeVedlegg ?: emptyList()
            val logiskeVedlegg = dokument.logiskeVedlegg ?: emptyList()

            val fjernedeVedlegg = eksisterendeLogiskeVedlegg.filter { !logiskeVedlegg.contains(it) }
            val nyeVedlegg = logiskeVedlegg.filter { !eksisterendeLogiskeVedlegg.contains(it) }

            fjernedeVedlegg.forEach {
                integrasjonKlient.slettLogiskVedlegg(it.logiskVedleggId, dokument.dokumentInfoId)
            }

            nyeVedlegg.forEach {
                integrasjonKlient.leggTilLogiskVedlegg(LogiskVedleggRequest(it.tittel), dokument.dokumentInfoId)
            }
        }
    }

    private fun oppdaterOgFerdigstill(
        oppdaterJournalPostRequest: OppdaterJournalpostRequestDto,
        journalpostId: String,
        behandlendeEnhet: String,
        oppgaveId: String,
        kontantstøtteBehandlinger: List<Behandling>,
    ) {
        runCatching {
            secureLogger.info("Oppdaterer journalpost $journalpostId med $oppdaterJournalPostRequest")

            integrasjonKlient.oppdaterJournalpost(oppdaterJournalPostRequest, journalpostId)

            opprettLoggPåDokumenter(journalpostId, kontantstøtteBehandlinger)

            secureLogger.info("Ferdigstiller journalpost $journalpostId")

            integrasjonKlient.ferdigstillJournalpost(
                journalpostId = journalpostId,
                journalførendeEnhet = behandlendeEnhet,
            )

            integrasjonKlient.ferdigstillOppgave(oppgaveId = oppgaveId.toLong())
        }.onFailure {
            hentJournalpost(journalpostId).journalstatus.apply {
                if (this == Journalstatus.FERDIGSTILT) {
                    integrasjonKlient.ferdigstillOppgave(oppgaveId = oppgaveId.toLong())
                } else {
                    throw it
                }
            }
        }
    }

    private fun opprettLoggPåDokumenter(
        journalpostId: String,
        behandlinger: List<Behandling>,
    ) {
        val journalpost = hentJournalpost(journalpostId)

        val loggTekst =
            journalpost.dokumenter?.fold("") { loggTekst, dokumentInfo ->
                loggTekst + "${dokumentInfo.tittel}" + dokumentInfo.logiskeVedlegg?.fold("") { logiskeVedleggTekst, logiskVedlegg -> logiskeVedleggTekst + "\n\u2002\u2002${logiskVedlegg.tittel}" } + "\n"
            } ?: ""

        behandlinger.forEach {
            loggService.opprettMottattDokumentLogg(
                behandling = it,
                tekst = loggTekst,
                mottattDato = journalpost.datoMottatt!!,
            )
        }
    }

    companion object {
        const val NAV_NO = "NAV_NO"
        const val SØKNADSKODE_KONTANTSTØTTE = "NAV 34-00.08"
    }
}
