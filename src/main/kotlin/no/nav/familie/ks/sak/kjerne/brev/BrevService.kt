package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.ks.sak.api.dto.ManuellAdresseInfo
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
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
    } catch (ressursException: RessursException) {
        logger.info("Klarte ikke å distribuere brev til journalpost $journalpostId. Httpstatus ${ressursException.httpStatus}")

        when {
            mottakerErIkkeDigitalOgHarUkjentAdresse(ressursException) && behandlingId != null -> {
                loggBrevIkkeDistribuertUkjentAdresse(journalpostId, behandlingId, brevmal)
            }

            mottakerErDødUtenDødsboadresse(ressursException) && behandlingId != null -> {
                håndterMottakerDødIngenAdressePåBehandling(journalpostId, brevmal, behandlingId)
            }

            dokumentetErAlleredeDistribuert(ressursException) -> {
                logger.warn(
                    "Journalpost med Id=$journalpostId er allerede distribuert. Hopper over distribuering." +
                        if (behandlingId != null) " BehandlingId=$behandlingId." else "",
                )
            }

            else -> {
                throw ressursException
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
