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
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
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
    private val integrasjonClient: IntegrasjonClient,
    private val fagsakService: FagsakService,
    private val opprettBehandlingService: OpprettBehandlingService,
    private val behandlingService: BehandlingService,
    private val journalføringRepository: JournalføringRepository,
    private val loggService: LoggService,
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,
    private val klageService: KlageService,
) {
    fun hentJournalposterForBruker(brukerId: String): List<TilgangsstyrtJournalpost> =
        integrasjonClient
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

    fun hentJournalpost(journalpostId: String): Journalpost = integrasjonClient.hentJournalpost(journalpostId)

    fun hentDokumentIJournalpost(
        journalpostId: String,
        dokumentId: String,
    ): ByteArray = integrasjonClient.hentDokumentIJournalpost(dokumentId, journalpostId)

    @Transactional
    fun journalfør(
        request: JournalføringRequestDto,
        journalpostId: String,
        oppgaveId: String,
    ): String {
        val tilknyttedeBehandlinger = request.tilknyttedeBehandlinger.toMutableList()
        val journalpost = integrasjonClient.hentJournalpost(journalpostId)

        val fagsakId =
            request.fagsakId
                ?: if (request.opprettOgKnyttTilNyBehandling) {
                    fagsakService.hentEllerOpprettFagsak(FagsakRequestDto(request.bruker.id)).id
                } else {
                    throw Feil("Forventet fagsak ved journalføring for journalpostId=$journalpostId og oppgaveId=$oppgaveId")
                }

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

    @Transactional
    @Deprecated("Erstattes av ny journalføring-funksjon")
    fun journalførGammel(
        request: JournalføringRequestDto,
        journalpostId: String,
        oppgaveId: String,
    ): String {
        val tilknyttedeBehandlingIder = request.tilknyttedeBehandlingIder.toMutableList()
        val journalpost = integrasjonClient.hentJournalpost(journalpostId)

        if (request.opprettOgKnyttTilNyBehandling) {
            fagsakService.hentEllerOpprettFagsak(FagsakRequestDto(request.bruker.id))

            val nyBehandling =
                opprettBehandlingForJournalføring(
                    personIdent = request.bruker.id,
                    saksbehandlerIdent = request.navIdent,
                    type = request.nyBehandlingstype.tilBehandingType(),
                    årsak = request.nyBehandlingsårsak,
                    kategori = request.kategori,
                    søknadMottattDato = request.datoMottatt?.toLocalDate(),
                )

            tilknyttedeBehandlingIder.add(nyBehandling.id.toString())
        }

        val (tilknyttetFagsak, behandlinger) =
            lagreJournalpostOgKnyttFagsakTilJournalpostGammel(
                tilknyttedeBehandlingIder,
                journalpostId,
            )

        oppdaterLogiskeVedlegg(request.dokumenter)

        oppdaterOgFerdigstill(
            oppdaterJournalPostRequest = request.tilOppdaterJournalpostRequestDto(tilknyttetFagsak, journalpost),
            journalpostId = journalpostId,
            behandlendeEnhet = request.journalførendeEnhet,
            oppgaveId = oppgaveId,
            kontantstøtteBehandlinger = behandlinger,
        )

        return tilknyttetFagsak.fagsakId ?: ""
    }

    fun knyttJournalpostTilFagsakOgFerdigstillOppgave(
        request: FerdigstillOppgaveKnyttJournalpostDto,
        oppgaveId: Long,
    ): String {
        val tilknyttedeBehandlinger: MutableList<TilknyttetBehandling> = request.tilknyttedeBehandlinger.toMutableList()
        val journalpost = hentJournalpost(request.journalpostId)

        val fagsakId =
            request.fagsakId
                ?: if (request.opprettOgKnyttTilNyBehandling) {
                    fagsakService.hentEllerOpprettFagsak(FagsakRequestDto(request.bruker!!.id)).id
                } else {
                    throw Feil("Forventet fagsak ved journalføring for journalpostId=${request.journalpostId} og oppgaveId=$oppgaveId")
                }

        if (request.opprettOgKnyttTilNyBehandling) {
            if (request.nyBehandlingstype == JournalføringBehandlingstype.KLAGE) {
                val klageMottattDato = request.datoMottatt?.toLocalDate() ?: throw Feil("Dato mottatt ikke satt ved journalføring av journalpost med id=$request.journalpostId")
                val klageBehandlingId = klageService.opprettKlage(fagsakId, klageMottattDato)
                tilknyttedeBehandlinger.add(TilknyttetBehandling(behandlingstype = JournalføringBehandlingstype.KLAGE, behandlingId = klageBehandlingId.toString()))
            } else {
                if (request.bruker == null || request.navIdent == null || request.nyBehandlingstype == null || request.nyBehandlingsårsak == null || request.kategori == null) {
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

        integrasjonClient.ferdigstillOppgave(oppgaveId = oppgaveId)

        return fagsakId.toString()
    }

    @Deprecated("Erstattet av ny funksjon")
    fun knyttJournalpostTilFagsakOgFerdigstillOppgaveGammel(
        request: FerdigstillOppgaveKnyttJournalpostDto,
        oppgaveId: Long,
    ): String {
        val tilknyttedeBehandlingIder: MutableList<String> = request.tilknyttedeBehandlingIder.toMutableList()

        val journalpost = hentJournalpost(request.journalpostId)
        journalpost.sak?.fagsakId

        if (request.opprettOgKnyttTilNyBehandling) {
            fagsakService.hentEllerOpprettFagsak(FagsakRequestDto(request.bruker!!.id))

            val nyBehandling =
                opprettBehandlingForJournalføring(
                    personIdent = request.bruker.id,
                    saksbehandlerIdent = request.navIdent!!,
                    type = request.nyBehandlingstype!!.tilBehandingType(),
                    årsak = request.nyBehandlingsårsak!!,
                    kategori = request.kategori!!,
                    søknadMottattDato = request.datoMottatt?.toLocalDate(),
                )
            tilknyttedeBehandlingIder.add(nyBehandling.id.toString())
        }

        val (sak) = lagreJournalpostOgKnyttFagsakTilJournalpostGammel(tilknyttedeBehandlingIder, journalpost.journalpostId)

        integrasjonClient.ferdigstillOppgave(oppgaveId = oppgaveId)

        return sak.fagsakId ?: ""
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

    @Deprecated("Erstattet av ny funksjon")
    private fun lagreJournalpostOgKnyttFagsakTilJournalpostGammel(
        tilknyttedeBehandlingIder: List<String>,
        journalpostId: String,
    ): Pair<Sak, List<Behandling>> {
        val behandlinger = tilknyttedeBehandlingIder.map { behandlingService.hentBehandling(it.toLong()) }
        val journalpost = hentJournalpost(journalpostId)
        val erSøknad = journalpost.dokumenter?.any { it.brevkode == SØKNADSKODE_KONTANTSTØTTE } ?: false

        behandlinger.forEach {
            journalføringRepository.save(
                DbJournalpost(
                    behandling = it,
                    journalpostId = journalpostId,
                    type = DbJournalpostType.valueOf(journalpost.journalposttype.name),
                ),
            )
            if (erSøknad) {
                behandlingSøknadsinfoService.lagreNedSøknadsinfo(
                    søknadsinfo =
                        Søknadsinfo(
                            mottattDato = journalpost.datoMottatt ?: LocalDateTime.now(),
                            journalpostId = journalpostId,
                            erDigital = journalpost.kanal == NAV_NO,
                        ),
                    behandling = it,
                )
            }
        }

        val fagsak = behandlinger.map { it.fagsak }.toSet().singleOrNull()

        val tilknyttetFagsak =
            Sak(
                fagsakId = fagsak?.id?.toString(),
                fagsaksystem = fagsak?.let { Fagsystem.KONT.name },
                sakstype = fagsak?.let { Sakstype.FAGSAK.type } ?: Sakstype.GENERELL_SAK.type,
            )

        return Pair(tilknyttetFagsak, behandlinger)
    }

    private fun oppdaterLogiskeVedlegg(dokumenter: List<JournalpostDokumentDto>) {
        dokumenter.forEach { dokument ->
            val eksisterendeLogiskeVedlegg = dokument.eksisterendeLogiskeVedlegg ?: emptyList()
            val logiskeVedlegg = dokument.logiskeVedlegg ?: emptyList()

            val fjernedeVedlegg = eksisterendeLogiskeVedlegg.filter { !logiskeVedlegg.contains(it) }
            val nyeVedlegg = logiskeVedlegg.filter { !eksisterendeLogiskeVedlegg.contains(it) }

            fjernedeVedlegg.forEach {
                integrasjonClient.slettLogiskVedlegg(it.logiskVedleggId, dokument.dokumentInfoId)
            }

            nyeVedlegg.forEach {
                integrasjonClient.leggTilLogiskVedlegg(LogiskVedleggRequest(it.tittel), dokument.dokumentInfoId)
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

            integrasjonClient.oppdaterJournalpost(oppdaterJournalPostRequest, journalpostId)

            opprettLoggPåDokumenter(journalpostId, kontantstøtteBehandlinger)

            secureLogger.info("Ferdigstiller journalpost $journalpostId")

            integrasjonClient.ferdigstillJournalpost(
                journalpostId = journalpostId,
                journalførendeEnhet = behandlendeEnhet,
            )

            integrasjonClient.ferdigstillOppgave(oppgaveId = oppgaveId.toLong())
        }.onFailure {
            hentJournalpost(journalpostId).journalstatus.apply {
                if (this == Journalstatus.FERDIGSTILT) {
                    integrasjonClient.ferdigstillOppgave(oppgaveId = oppgaveId.toLong())
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
