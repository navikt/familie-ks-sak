package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import no.nav.familie.ks.sak.api.dto.DistribuerBrevDto
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.api.dto.tilBrev
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.distributering.DistribuerBrevTask
import no.nav.familie.ks.sak.integrasjon.distributering.DistribuerDødsfallBrevPåFagsakTask
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService.Companion.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.DbJournalpost
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.DbJournalpostType
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringRepository
import no.nav.familie.ks.sak.integrasjon.logger
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.SettBehandlingPåVentService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevVedtaksPeriode
import no.nav.familie.ks.sak.kjerne.brev.domene.FellesdataForVedtaksbrev
import no.nav.familie.ks.sak.kjerne.brev.domene.VedtaksbrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Førstegangsvedtak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Properties

@Service
class BrevService(
    private val brevKlient: BrevKlient,
    private val integrasjonClient: IntegrasjonClient,
    private val loggService: LoggService,
    private val taskService: TaskService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val utgåendeJournalføringService: UtgåendeJournalføringService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val behandlingRepository: BehandlingRepository,
    private val journalføringRepository: JournalføringRepository,
    private val settBehandlingPåVentService: SettBehandlingPåVentService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val brevPeriodeService: BrevPeriodeService,
    private val sanityService: SanityService,
    private val totrinnskontrollService: TotrinnskontrollService
) {

    fun hentForhåndsvisningAvBrev(behandlingId: Long, manueltBrevDto: ManueltBrevDto): ByteArray {
        val manueltBrevDtoMedMottakerData = utvidManueltBrevDtoMedEnhetOgMottaker(behandlingId, manueltBrevDto)
        return genererManueltBrev(manueltBrevDtoMedMottakerData, true)
    }

    fun genererOgSendBrev(behandlingId: Long, manueltBrevDto: ManueltBrevDto) {
        val manueltBrevDtoMedMottakerData = utvidManueltBrevDtoMedEnhetOgMottaker(behandlingId, manueltBrevDto)
        sendBrev(behandlingId, manueltBrevDtoMedMottakerData)
    }

    private fun utvidManueltBrevDtoMedEnhetOgMottaker(
        behandlingId: Long,
        manueltBrevDto: ManueltBrevDto
    ): ManueltBrevDto {
        val mottakerPerson = personopplysningGrunnlagService.hentSøker(behandlingId)
        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId)

        return manueltBrevDto.copy(
            enhet = Enhet(
                enhetNavn = arbeidsfordelingPåBehandling.behandlendeEnhetNavn,
                enhetId = arbeidsfordelingPåBehandling.behandlendeEnhetId
            ),
            mottakerMålform = mottakerPerson?.målform ?: manueltBrevDto.mottakerMålform,
            mottakerNavn = mottakerPerson?.navn ?: manueltBrevDto.mottakerNavn
        )
    }

    private fun genererManueltBrev(
        manueltBrevRequest: ManueltBrevDto,
        erForhåndsvisning: Boolean = false
    ): ByteArray {
        try {
            val brev = manueltBrevRequest.tilBrev()
            return brevKlient.genererBrev(
                målform = manueltBrevRequest.mottakerMålform.tilSanityFormat(),
                brev = brev
            )
        } catch (it: Exception) {
            if (it is Feil || it is FunksjonellFeil) {
                throw it
            } else {
                throw Feil(
                    message = "Klarte ikke generere brev for ${manueltBrevRequest.brevmal}. ${it.message}",
                    frontendFeilmelding = "${if (erForhåndsvisning) "Det har skjedd en feil" else "Det har skjedd en feil, og brevet er ikke sendt"}. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                    throwable = it
                )
            }
        }
    }

    fun genererBrevForVedtak(vedtak: Vedtak): ByteArray {
        try {
            if (vedtak.behandling.steg > BehandlingSteg.BESLUTTE_VEDTAK) {
                throw FunksjonellFeil("Ikke tillatt å generere brev etter at behandlingen er sendt fra beslutter")
            }

            val målform = personopplysningGrunnlagService.hentSøkersMålform(vedtak.behandling.id)
            val vedtaksbrev =
                when (vedtak.behandling.opprettetÅrsak) {
                    BehandlingÅrsak.DØDSFALL -> TODO() // brevService.hentDødsfallbrevData(vedtak)
                    BehandlingÅrsak.KORREKSJON_VEDTAKSBREV -> TODO() // brevService.hentKorreksjonbrevData(vedtak)
                    else -> hentVedtaksbrevData(vedtak)
                }
            return brevKlient.genererBrev(målform.tilSanityFormat(), vedtaksbrev)
        } catch (feil: Exception) {
            if (feil is FunksjonellFeil) throw feil

            throw Feil(
                message = "Klarte ikke generere vedtaksbrev på behandling ${vedtak.behandling}: ${feil.message}",
                frontendFeilmelding = "Det har skjedd en feil, og brevet er ikke sendt. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = feil
            )
        }
    }

    @Transactional
    fun sendBrev(behandlingId: Long, manueltBrevDto: ManueltBrevDto) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)

        val generertBrev = genererManueltBrev(manueltBrevDto, false)

        val førsteside = if (manueltBrevDto.brevmal.skalGenerereForside()) {
            Førsteside(
                språkkode = manueltBrevDto.mottakerMålform.tilSpråkkode(),
                navSkjemaId = "NAV 34-00.07",
                overskriftstittel = "Ettersendelse til søknad om kontantstøtte til småbarnsforeldre NAV 34-00.07"
            )
        } else {
            null
        }

        val journalpostId = utgåendeJournalføringService.journalførDokument(
            fnr = behandling.fagsak.aktør.aktivFødselsnummer(),
            fagsakId = behandling.fagsak.id,
            behandlingId = behandlingId,
            journalførendeEnhet = manueltBrevDto.enhet?.enhetId
                ?: DEFAULT_JOURNALFØRENDE_ENHET,
            brev = listOf(
                Dokument(
                    dokument = generertBrev,
                    filtype = Filtype.PDFA,
                    dokumenttype = manueltBrevDto.brevmal.tilFamilieKontrakterDokumentType()
                )
            ),
            førsteside = førsteside
        )

        journalføringRepository.save(
            DbJournalpost(
                behandling = behandling,
                journalpostId = journalpostId,
                type = DbJournalpostType.U
            )
        )

        if ((
            manueltBrevDto.brevmal == Brevmal.INNHENTE_OPPLYSNINGER ||
                manueltBrevDto.brevmal == Brevmal.VARSEL_OM_REVURDERING
            )
        ) {
            leggTilOpplysningspliktIVilkårsvurdering(behandling)
        }

        DistribuerBrevTask.opprettDistribuerBrevTask(
            distribuerBrevDTO = DistribuerBrevDto(
                personIdent = manueltBrevDto.mottakerIdent,
                behandlingId = behandling.id,
                journalpostId = journalpostId,
                brevmal = manueltBrevDto.brevmal,
                erManueltSendt = true
            ),
            properties = Properties().apply {
                this["fagsakIdent"] = behandling.fagsak.aktør.aktivFødselsnummer()
                this["mottakerIdent"] = manueltBrevDto.mottakerIdent
                this["journalpostId"] = journalpostId
                this["behandlingId"] = behandling.id.toString()
                this["fagsakId"] = behandling.fagsak.id.toString()
            }
        ).also {
            taskService.save(it)
        }
        if (
            manueltBrevDto.brevmal.setterBehandlingPåVent()
        ) {
            settBehandlingPåVentService.settBehandlingPåVent(
                behandlingId = behandlingId,
                frist = LocalDate.now()
                    .plusDays(
                        manueltBrevDto.brevmal.ventefristDager(
                            manuellFrist = manueltBrevDto.antallUkerSvarfrist?.toLong(),
                            behandlingKategori = behandling.kategori
                        )
                    )
            )
        }
    }

    fun prøvDistribuerBrevOgLoggHendelse(
        journalpostId: String,
        behandlingId: Long?,
        loggBehandlerRolle: BehandlerRolle,
        brevmal: Brevmal
    ) = try {
        distribuerBrevOgLoggHendelse(journalpostId, behandlingId, brevmal, loggBehandlerRolle)
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
                        if (behandlingId != null) " BehandlingId=$behandlingId." else ""
                )

            else -> throw ressursException
        }
    }

    private fun distribuerBrevOgLoggHendelse(
        journalpostId: String,
        behandlingId: Long?,
        brevMal: Brevmal,
        loggBehandlerRolle: BehandlerRolle
    ) {
        integrasjonClient.distribuerBrev(journalpostId = journalpostId, distribusjonstype = brevMal.distribusjonstype)

        if (behandlingId != null) {
            loggService.opprettDistribuertBrevLogg(
                behandlingId = behandlingId,
                tekst = brevMal.visningsTekst,
                rolle = loggBehandlerRolle
            )
        }
    }

    internal fun håndterMottakerDødIngenAdressePåBehandling(
        journalpostId: String,
        brevmal: Brevmal,
        behandlingId: Long
    ) {
        val task = DistribuerDødsfallBrevPåFagsakTask.opprettTask(journalpostId = journalpostId, brevmal = brevmal)

        taskService.save(task)

        logger.info("Klarte ikke å distribuere brev for journalpostId $journalpostId på behandling $behandlingId. Bruker har ukjent dødsboadresse.")
        loggService.opprettBrevIkkeDistribuertUkjentDødsboadresseLogg(
            behandlingId = behandlingId,
            brevnavn = brevmal.visningsTekst
        )
    }

    internal fun loggBrevIkkeDistribuertUkjentAdresse(
        journalpostId: String,
        behandlingId: Long,
        brevMal: Brevmal
    ) {
        logger.info("Klarte ikke å distribuere brev for journalpostId $journalpostId på behandling $behandlingId. Bruker har ukjent adresse.")
        loggService.opprettBrevIkkeDistribuertUkjentAdresseLogg(
            behandlingId = behandlingId,
            brevnavn = brevMal.visningsTekst
        )
    }

    private fun leggTilOpplysningspliktIVilkårsvurdering(behandling: Behandling) {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)
        vilkårsvurdering.personResultater.single { it.erSøkersResultater() }
            .leggTilBlankAnnenVurdering(AnnenVurderingType.OPPLYSNINGSPLIKT)
    }

    fun hentVedtaksbrevData(vedtak: Vedtak): VedtaksbrevDto {
        val brevtype = hentVedtaksbrevmal(vedtak.behandling)
        val fellestdataForVedtaksbrev = lagDataForVedtaksbrev(vedtak)

        return when (brevtype) {
            Brevmal.VEDTAK_FØRSTEGANGSVEDTAK -> Førstegangsvedtak(
                fellesdataForVedtaksbrev = fellestdataForVedtaksbrev,
                etterbetaling = null // TODO når simulering er inne
            )

            else -> throw Feil("Forsøker å hente vedtaksbrevdata for brevmal ${brevtype.visningsTekst}")
        }
    }

    fun lagDataForVedtaksbrev(vedtak: Vedtak): FellesdataForVedtaksbrev {
        val utvidetVedtaksperioderMedBegrunnelser =
            vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelser(vedtak).filter {
                !(it.begrunnelser.isEmpty() && it.fritekster.isEmpty() && it.eøsBegrunnelser.isEmpty())
            }.sortedBy { it.fom }

        if (utvidetVedtaksperioderMedBegrunnelser.isEmpty()) {
            throw FunksjonellFeil(
                "Vedtaket mangler begrunnelser. Du må legge til begrunnelser for å generere vedtaksbrevet."
            )
        }

        val personopplysningsgrunnlagOgSignaturData = hentGrunnlagOgSignaturData(vedtak)
        val grunnlagForBrevperioder = brevPeriodeService
            .hentGrunnlagForBrevperioder(utvidetVedtaksperioderMedBegrunnelser.map { it.id }, vedtak.id)
        val brevPeriodeDtoer = grunnlagForBrevperioder.sorted().mapNotNull {
            BrevPeriodeGenerator(it).genererBrevPeriode()
        }
        val hjemler = hentHjemler(
            behandlingId = vedtak.behandling.id,
            brevVedtaksPerioder = grunnlagForBrevperioder.map { it.brevVedtaksPeriode },
            målform = personopplysningsgrunnlagOgSignaturData.grunnlag.søker.målform
        )

        return FellesdataForVedtaksbrev(
            enhet = personopplysningsgrunnlagOgSignaturData.enhet,
            saksbehandler = personopplysningsgrunnlagOgSignaturData.saksbehandler,
            beslutter = personopplysningsgrunnlagOgSignaturData.beslutter,
            hjemmeltekst = Hjemmeltekst(hjemler),
            søkerNavn = personopplysningsgrunnlagOgSignaturData.grunnlag.søker.navn,
            søkerFødselsnummer = personopplysningsgrunnlagOgSignaturData.grunnlag.søker.aktør.aktivFødselsnummer(),
            perioder = brevPeriodeDtoer,
            gjelder = personopplysningsgrunnlagOgSignaturData.grunnlag.søker.navn
        )
    }

    private fun hentGrunnlagOgSignaturData(vedtak: Vedtak): GrunnlagOgSignaturData {
        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(vedtak.behandling.id)
        val totrinnskontroll = totrinnskontrollService.finnAktivForBehandling(vedtak.behandling.id)
        val enhetNavn = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(vedtak.behandling.id).behandlendeEnhetNavn

        return GrunnlagOgSignaturData(
            grunnlag = personopplysningGrunnlag,
            saksbehandler = totrinnskontroll?.saksbehandler ?: SikkerhetContext.hentSaksbehandlerNavn(),
            beslutter = totrinnskontroll?.beslutter ?: "Beslutter",
            enhet = enhetNavn
        )
    }

    private fun hentHjemler(
        behandlingId: Long,
        brevVedtaksPerioder: List<BrevVedtaksPeriode>,
        målform: Målform
    ): String {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandlingId)

        val opplysningspliktHjemlerSkalMedIBrev =
            vilkårsvurdering.finnOpplysningspliktVilkår()?.resultat == Resultat.IKKE_OPPFYLT

        return hentHjemmeltekst(
            brevVedtaksperioder = brevVedtaksPerioder,
            sanityBegrunnelser = sanityService.hentSanityBegrunnelser(),
            opplysningspliktHjemlerSkalMedIBrev = opplysningspliktHjemlerSkalMedIBrev,
            målform = målform
        )
    }

    private data class GrunnlagOgSignaturData(
        val grunnlag: PersonopplysningGrunnlag,
        val saksbehandler: String,
        val beslutter: String,
        val enhet: String
    )
}
