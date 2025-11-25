package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import no.nav.familie.ks.sak.api.dto.DistribuerBrevDto
import no.nav.familie.ks.sak.api.dto.FullmektigEllerVerge
import no.nav.familie.ks.sak.api.dto.ManuellAdresseInfo
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.api.dto.tilAvsenderMottaker
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.integrasjon.distribuering.DistribuerBrevTask
import no.nav.familie.ks.sak.integrasjon.distribuering.DistribuerDødsfallBrevPåFagsakTask
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService.Companion.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService.Companion.genererEksternReferanseIdForJournalpost
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.DbJournalpost
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.DbJournalpostType
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringRepository
import no.nav.familie.ks.sak.integrasjon.logger
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.SettBehandlingPåVentService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Properties

@Service
class BrevService(
    private val integrasjonKlient: IntegrasjonKlient,
    private val loggService: LoggService,
    private val taskService: TaskRepositoryWrapper,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val utgåendeJournalføringService: UtgåendeJournalføringService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val behandlingRepository: BehandlingRepository,
    private val journalføringRepository: JournalføringRepository,
    private val settBehandlingPåVentService: SettBehandlingPåVentService,
    private val genererBrevService: GenererBrevService,
    private val brevmottakerService: BrevmottakerService,
    private val validerBrevmottakerService: ValiderBrevmottakerService,
    private val featureToggleService: FeatureToggleService,
    private val saksbehandlerContext: SaksbehandlerContext,
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

        if (featureToggleService.isEnabled(FeatureToggle.JOURNALFOER_MANUELT_BREV_I_TASK)) {
            sendBrevNy(behandling.fagsak, behandlingId, manueltBrevDtoMedMottakerData)
        } else {
            sendBrev(behandling.fagsak, behandlingId, manueltBrevDtoMedMottakerData)
        }
    }

    private fun utvidManueltBrevDtoMedEnhetOgMottaker(
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

    @Transactional
    fun sendBrev(
        fagsak: Fagsak,
        behandlingId: Long? = null,
        manueltBrevDto: ManueltBrevDto,
    ) {
        validerManuelleBrevmottakere(behandlingId, fagsak, manueltBrevDto)

        val behandling = behandlingId?.let { behandlingRepository.hentBehandling(behandlingId) }
        val generertBrev = genererBrevService.genererManueltBrev(manueltBrevDto)

        val førsteside =
            if (manueltBrevDto.brevmal.skalGenerereForside()) {
                Førsteside(
                    språkkode = manueltBrevDto.mottakerMålform.tilSpråkkode(),
                    navSkjemaId = "NAV 34-00.07",
                    overskriftstittel = "Ettersendelse til søknad om kontantstøtte til småbarnsforeldre NAV 34-00.07",
                )
            } else {
                null
            }

        val brevmottakereFraBehandling = behandling?.let { brevmottakerService.hentBrevmottakere(it.id) } ?: emptyList()
        val brevmottakerDtoListe = manueltBrevDto.manuelleBrevmottakere + brevmottakereFraBehandling
        val mottakere =
            brevmottakerService.lagMottakereFraBrevMottakere(
                manueltRegistrerteMottakere = brevmottakerDtoListe,
            )

        if (!BrevmottakerAdresseValidering.harBrevmottakereGyldigAddresse(brevmottakerDtoListe)) {
            throw FunksjonellFeil(
                melding = "Det finnes ugyldige brevmottakere i utsending av manuelt brev",
                frontendFeilmelding = "Adressen som er lagt til manuelt har ugyldig format, og brevet kan ikke sendes. Du må legge til manuell adresse på nytt.",
            )
        }

        val journalposterTilDistribusjon =
            mottakere.map { mottaker ->
                val journalpostId =
                    utgåendeJournalføringService.journalførDokument(
                        fnr = fagsak.aktør.aktivFødselsnummer(),
                        fagsakId = fagsak.id,
                        journalførendeEnhet =
                            manueltBrevDto.enhet?.enhetId
                                ?: DEFAULT_JOURNALFØRENDE_ENHET,
                        brev =
                            listOf(
                                Dokument(
                                    dokument = generertBrev,
                                    filtype = Filtype.PDFA,
                                    dokumenttype = manueltBrevDto.brevmal.tilFamilieKontrakterDokumentType(),
                                ),
                            ),
                        førsteside = førsteside,
                        avsenderMottaker = mottaker.tilAvsenderMottaker(),
                        eksternReferanseId = genererEksternReferanseIdForJournalpost(fagsak.id, behandlingId, mottaker is FullmektigEllerVerge),
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

                journalpostId to mottaker
            }

        behandling?.let {
            val skalLeggeTilOpplysningspliktPåVilkårsvurdering =
                manueltBrevDto.brevmal == Brevmal.INNHENTE_OPPLYSNINGER || manueltBrevDto.brevmal == Brevmal.VARSEL_OM_REVURDERING

            if (skalLeggeTilOpplysningspliktPåVilkårsvurdering) {
                leggTilOpplysningspliktIVilkårsvurdering(behandling)
            }

            if (
                manueltBrevDto.brevmal.setterBehandlingPåVent()
            ) {
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

        journalposterTilDistribusjon.forEach { (journalpostId, mottaker) ->
            DistribuerBrevTask
                .opprettDistribuerBrevTask(
                    distribuerBrevDTO =
                        DistribuerBrevDto(
                            behandlingId = behandling?.id,
                            journalpostId = journalpostId,
                            brevmal = manueltBrevDto.brevmal,
                            erManueltSendt = true,
                            manuellAdresseInfo = mottaker.manuellAdresseInfo,
                        ),
                    properties =
                        Properties().apply {
                            this["fagsakIdent"] = fagsak.aktør.aktivFødselsnummer()
                            this["mottakerIdent"] = manueltBrevDto.mottakerIdent
                            this["journalpostId"] = journalpostId
                            this["behandlingId"] = behandling?.id.toString()
                            this["fagsakId"] = fagsak.id.toString()
                            this["mottakerType"] = mottaker.javaClass.simpleName
                        },
                ).also {
                    taskService.save(it)
                }
        }
    }

    @Transactional
    fun sendBrevNy(
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
    } catch (ressursException: RessursException) {
        logger.info("Klarte ikke å distribuere brev til journalpost $journalpostId. Httpstatus ${ressursException.httpStatus}")

        when {
            mottakerErIkkeDigitalOgHarUkjentAdresse(ressursException) && behandlingId != null ->
                loggBrevIkkeDistribuertUkjentAdresse(journalpostId, behandlingId, brevmal)

            mottakerErDødUtenDødsboadresse(ressursException) && behandlingId != null ->
                håndterMottakerDødIngenAdressePåBehandling(journalpostId, brevmal, behandlingId)

            dokumentetErAlleredeDistribuert(ressursException) ->
                logger.warn(
                    "Journalpost med Id=$journalpostId er allerede distribuert. Hopper over distribuering." +
                        if (behandlingId != null) " BehandlingId=$behandlingId." else "",
                )

            else -> throw ressursException
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
}
