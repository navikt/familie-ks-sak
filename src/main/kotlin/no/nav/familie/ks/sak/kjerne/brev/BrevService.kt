package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.ks.sak.api.dto.ManuellAdresseInfo
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.integrasjon.distribuering.DistribuerDødsfallBrevPåFagsakTask
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.Arbeidsfordelingsenhet
import no.nav.familie.ks.sak.integrasjon.logger
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.SettBehandlingPåVentService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerAdresseValidering
import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ks.sak.kjerne.brev.mottaker.ValiderBrevmottakerService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.sikkerhet.SaksbehandlerContext
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClientResponseException
import java.time.LocalDate

@Service
class BrevService(
    private val integrasjonKlient: IntegrasjonKlient,
    private val loggService: LoggService,
    private val taskService: TaskRepositoryWrapper,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val `vilkårsvurderingService`: VilkårsvurderingService,
    private val behandlingRepository: BehandlingRepository,
    private val `settBehandlingPåVentService`: SettBehandlingPåVentService,
    private val genererBrevService: GenererBrevService,
    private val brevmottakerService: BrevmottakerService,
    private val validerBrevmottakerService: ValiderBrevmottakerService,
    private val saksbehandlerContext: SaksbehandlerContext,
    private val featureToggleService: FeatureToggleService,
    private val behandlingService: BehandlingService,
) {
    fun hentForhåndsvisningAvBrev(
        manueltBrevDto: ManueltBrevDto,
    ): ByteArray = genererBrevService.genererManueltBrev(manueltBrevDto, true)

    @Transactional
    fun genererOgSendBrev(
        behandlingId: Long,
        manueltBrevDto: ManueltBrevDto,
    ) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)

        val manueltBrevDtoMedMottakerData = utvidManueltBrevDtoMedEnhetOgMottaker(behandlingId, manueltBrevDto)

        sendBrev(behandling.fagsak, behandlingId, manueltBrevDtoMedMottakerData)
    }

    fun utvidManueltBrevDtoMedEnhetOgMottaker(
        behandlingId: Long,
        manueltBrevDto: ManueltBrevDto,
    ): ManueltBrevDto {
        val mottakerPerson = personopplysningGrunnlagService.hentSøker(behandlingId)
        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId)

        return manueltBrevDto.copy(
            enhet =
                Enhet(
                    enhetNavn = arbeidsfordelingPåBehandling.behandlendeEnhetNavn,
                    enhetId = arbeidsfordelingPåBehandling.behandlendeEnhetId,
                ),
            mottakerMålform = mottakerPerson?.målform ?: manueltBrevDto.mottakerMålform,
            mottakerNavn = mottakerPerson?.navn ?: manueltBrevDto.mottakerNavn,
        )
    }

    fun leggTilEnhet(
        fagsakId: Long,
        manueltBrevDto: ManueltBrevDto,
    ): ManueltBrevDto {
        val arbeidsfordelingsenhet =
            if (featureToggleService.isEnabled(FeatureToggle.HENT_ARBEIDSFORDELING_MED_BEHANDLINGSTYPE)) {
                val enheterSomNavIdentHarTilgangTil = integrasjonKlient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(NavIdent(SikkerhetContext.hentSaksbehandler()))
                if (enheterSomNavIdentHarTilgangTil.size == 1) {
                    Arbeidsfordelingsenhet(
                        enhetId = enheterSomNavIdentHarTilgangTil.first().enhetsnummer,
                        enhetNavn = enheterSomNavIdentHarTilgangTil.first().enhetsnavn,
                    )
                } else {
                    val sisteVedtatteBehandling = behandlingService.hentSisteBehandlingSomErVedtatt(fagsakId)

                    arbeidsfordelingService
                        .hentArbeidsfordelingsenhetPåIdenter(
                            søkerIdent = manueltBrevDto.mottakerIdent,
                            barnIdenter = manueltBrevDto.barnIBrev,
                            behandlingstype = sisteVedtatteBehandling?.kategori?.tilOppgavebehandlingType(),
                        )
                }
            } else {
                arbeidsfordelingService
                    .hentArbeidsfordelingsenhetPåIdenter(
                        søkerIdent = manueltBrevDto.mottakerIdent,
                        barnIdenter = manueltBrevDto.barnIBrev,
                        behandlingstype = null,
                    )
            }
        return manueltBrevDto.copy(
            enhet =
                Enhet(
                    enhetNavn = arbeidsfordelingsenhet.enhetNavn,
                    enhetId = arbeidsfordelingsenhet.enhetId,
                ),
        )
    }

    @Transactional
    fun sendBrev(
        fagsak: Fagsak,
        behandlingId: Long? = null,
        manueltBrevDto: ManueltBrevDto,
    ) {
        validerManuelleBrevmottakere(behandlingId, fagsak, manueltBrevDto)

        val behandling = behandlingId?.let { behandlingRepository.hentBehandling(behandlingId) }

        val brevmottakereFraBehandling = behandling?.let { brevmottakerService.hentBrevmottakere(it.id) } ?: emptyList()
        val brevmottakerDtoListe = manueltBrevDto.manuelleBrevmottakere + brevmottakereFraBehandling
        val mottakere = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakerDtoListe)

        if (!BrevmottakerAdresseValidering.harBrevmottakereGyldigAddresse(brevmottakerDtoListe)) {
            throw FunksjonellFeil(
                melding = "Det finnes ugyldige brevmottakere i utsending av manuelt brev",
                frontendFeilmelding = "Adressen som er lagt til manuelt har ugyldig format, og brevet kan ikke sendes. Du må legge til manuell adresse på nytt.",
            )
        }

        mottakere.forEach { mottaker ->
            taskService.save(
                JournalførManueltBrevTask.opprettTask(
                    behandlingId = behandlingId,
                    fagsakId = fagsak.id,
                    manueltBrevDto = manueltBrevDto,
                    mottakerInfo = mottaker,
                    saksbehandlerSignaturTilBrev = saksbehandlerContext.hentSaksbehandlerSignaturTilBrev(),
                ),
            )
        }

        if (behandling != null) {
            val skalLeggeTilOpplysningspliktPåVilkårsvurdering =
                manueltBrevDto.brevmal == Brevmal.INNHENTE_OPPLYSNINGER || manueltBrevDto.brevmal == Brevmal.VARSEL_OM_REVURDERING

            if (skalLeggeTilOpplysningspliktPåVilkårsvurdering) {
                leggTilOpplysningspliktIVilkårsvurdering(behandling)
            }

            if (manueltBrevDto.brevmal.setterBehandlingPåVent()) {
                settBehandlingPåVentService.settBehandlingPåVent(
                    behandlingId = behandlingId,
                    frist =
                        LocalDate
                            .now()
                            .plusDays(
                                manueltBrevDto.brevmal.ventefristDager(
                                    manuellFrist = manueltBrevDto.antallUkerSvarfrist?.toLong(),
                                    behandlingKategori = behandling.kategori,
                                ),
                            ),
                    årsak = manueltBrevDto.brevmal.hentVenteÅrsak(),
                )
            }
        }
    }

    fun prøvDistribuerBrevOgLoggHendelse(
        journalpostId: String,
        behandlingId: Long?,
        loggBehandlerRolle: BehandlerRolle,
        brevmal: Brevmal,
        manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) = try {
        distribuerBrevOgLoggHendelse(journalpostId, behandlingId, brevmal, loggBehandlerRolle, manuellAdresseInfo)
    } catch (exception: RestClientResponseException) {
        logger.info("Klarte ikke å distribuere brev til journalpost $journalpostId. Httpstatus ${exception.statusCode}")

        when {
            mottakerErIkkeDigitalOgHarUkjentAdresse(exception) && behandlingId != null -> {
                loggBrevIkkeDistribuertUkjentAdresse(journalpostId, behandlingId, brevmal)
            }

            mottakerErDødUtenDødsboadresse(exception) && behandlingId != null -> {
                håndterMottakerDødIngenAdressePåBehandling(journalpostId, brevmal, behandlingId)
            }

            dokumentetErAlleredeDistribuert(exception) -> {
                logger.warn(
                    "Journalpost med Id=$journalpostId er allerede distribuert. Hopper over distribuering." +
                        if (behandlingId != null) " BehandlingId=$behandlingId." else "",
                )
            }

            else -> {
                throw exception
            }
        }
    }

    private fun distribuerBrevOgLoggHendelse(
        journalpostId: String,
        behandlingId: Long?,
        brevMal: Brevmal,
        loggBehandlerRolle: BehandlerRolle,
        manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) {
        integrasjonKlient.distribuerBrev(
            journalpostId = journalpostId,
            distribusjonstype = brevMal.distribusjonstype,
            manuellAdresseInfo = manuellAdresseInfo,
        )

        if (behandlingId != null) {
            loggService.opprettDistribuertBrevLogg(
                behandlingId = behandlingId,
                tekst = brevMal.visningsTekst,
                rolle = loggBehandlerRolle,
            )
        }
    }

    internal fun håndterMottakerDødIngenAdressePåBehandling(
        journalpostId: String,
        brevmal: Brevmal,
        behandlingId: Long,
    ) {
        val task = DistribuerDødsfallBrevPåFagsakTask.opprettTask(journalpostId = journalpostId, brevmal = brevmal)

        taskService.save(task)

        logger.info(
            "Klarte ikke å distribuere brev for journalpostId $journalpostId på behandling $behandlingId. Bruker har ukjent dødsboadresse.",
        )
        loggService.opprettBrevIkkeDistribuertUkjentDødsboadresseLogg(
            behandlingId = behandlingId,
            brevnavn = brevmal.visningsTekst,
        )
    }

    internal fun loggBrevIkkeDistribuertUkjentAdresse(
        journalpostId: String,
        behandlingId: Long,
        brevMal: Brevmal,
    ) {
        logger.info(
            "Klarte ikke å distribuere brev for journalpostId $journalpostId på behandling $behandlingId. Bruker har ukjent adresse.",
        )
        loggService.opprettBrevIkkeDistribuertUkjentAdresseLogg(
            behandlingId = behandlingId,
            brevnavn = brevMal.visningsTekst,
        )
    }

    private fun leggTilOpplysningspliktIVilkårsvurdering(behandling: Behandling) {
        val sisteVedtattBehandling by lazy { hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) }

        val vilkårsvurdering =
            vilkårsvurderingService.finnAktivVilkårsvurdering(behandling.id)
                ?: vilkårsvurderingService.opprettVilkårsvurdering(behandling, sisteVedtattBehandling)

        vilkårsvurdering.personResultater
            .single { it.erSøkersResultater() }
            .leggTilBlankAnnenVurdering(AnnenVurderingType.OPPLYSNINGSPLIKT)
    }

    private fun hentSisteBehandlingSomErVedtatt(fagsakId: Long): Behandling? =
        behandlingRepository
            .finnBehandlinger(fagsakId)
            .filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.aktivertTidspunkt }

    fun hentVedtaksbrevPdf(vedtak: Vedtak): ByteArray {
        val behandling = vedtak.behandling

        val skalHenteVedtaksbrevFraJoark =
            featureToggleService.isEnabled(FeatureToggle.HENT_VEDTAKSBREV_FRA_JOARK) &&
                behandling.erAvsluttet() &&
                !behandling.erHenlagt() &&
                behandling.skalSendeVedtaksbrev(behandlingService.erLovendringOgFremtidigOpphørOgHarFlereAndeler(behandling))

        return if (skalHenteVedtaksbrevFraJoark) {
            hentVedtaksbrevFraJoark(behandling)
                ?: throw FunksjonellFeil(
                    melding = "Fant ikke vedtaksbrev for behandling med id ${behandling.id} i Joark.",
                    frontendFeilmelding = "Fant ikke vedtaksbrevet i arkivet. Du kan finne brevet i dokumentoversikten.",
                )
        } else {
            vedtak.stønadBrevPdf ?: throw Feil("Klarte ikke finne vedtaksbrev for behandling med id ${behandling.id}")
        }
    }

    private fun hentVedtaksbrevFraJoark(behandling: Behandling): ByteArray? {
        val eksternReferanseIdPrefiks = "${behandling.fagsak.id}_${behandling.id}_"

        val journalposterForVedtaksbrev =
            integrasjonKlient
                .hentJournalposterForBruker(
                    JournalposterForBrukerRequest(
                        brukerId =
                            Bruker(
                                id = behandling.fagsak.aktør.aktivFødselsnummer(),
                                type = BrukerIdType.FNR,
                            ),
                        antall = 1000,
                        tema = listOf(Tema.KON),
                        journalposttype = listOf(Journalposttype.U),
                    ),
                ).filter { it.eksternReferanseId?.startsWith(eksternReferanseIdPrefiks) == true }
                .filter { it.journalstatus == Journalstatus.FERDIGSTILT || it.journalstatus == Journalstatus.EKSPEDERT }
                .filter { it.hoveddokumentErVedtaksbrev() }

        if (journalposterForVedtaksbrev.isEmpty()) {
            logger.warn("Fant ingen journalpost med vedtaksbrev i Joark for behandling ${behandling.id}")
            return null
        }

        if (journalposterForVedtaksbrev.size > 1) {
            logger.info(
                "Fant flere journalposter med vedtaksbrev i Joark for behandling ${behandling.id}: " +
                    "${journalposterForVedtaksbrev.map { it.journalpostId }}. Brevet er likt for alle mottakere, bruker den første.",
            )
        }

        val journalpost = journalposterForVedtaksbrev.first()
        val hoveddokument = checkNotNull(journalpost.hentHoveddokument())

        return integrasjonKlient.hentDokumentIJournalpost(dokumentId = hoveddokument.dokumentInfoId, journalpostId = journalpost.journalpostId)
    }

    // Hoveddokumentet er det eneste dokumentet i journalposten med brevkode, vedlegg journalføres uten
    private fun Journalpost.hentHoveddokument(): DokumentInfo? = dokumenter?.firstOrNull { it.brevkode != null }

    private fun Journalpost.hoveddokumentErVedtaksbrev(): Boolean {
        val brevkode = hentHoveddokument()?.brevkode ?: return false
        return brevkode in BREVKODER_FOR_VEDTAKSBREV
    }

    private fun validerManuelleBrevmottakere(
        behandlingId: Long?,
        fagsak: Fagsak,
        manueltBrevDto: ManueltBrevDto,
    ) {
        if (behandlingId == null) {
            validerBrevmottakerService.validerAtFagsakIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
                fagsakId = fagsak.id,
                manuelleBrevmottakere = manueltBrevDto.manuelleBrevmottakere,
                barnLagtTilIBrev = manueltBrevDto.barnIBrev,
            )
        } else {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
                behandlingId = behandlingId,
                ekstraBarnLagtTilIBrev = manueltBrevDto.barnIBrev,
            )
        }
    }

    companion object {
        // Brevkodene vedtaksbrev journalføres med i familie-integrasjoner
        private val BREVKODER_FOR_VEDTAKSBREV =
            setOf(
                "vedtak-om-kontantstøtte",
                "vedtak-om-innvilgelse-kontantstøtte",
                "vedtak-om-endret-kontantstøtte",
                "vedtak-om-avslag-kontantstøtte",
                "opphor",
            )
    }
}
