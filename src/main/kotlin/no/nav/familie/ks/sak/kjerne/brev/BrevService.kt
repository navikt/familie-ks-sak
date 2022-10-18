package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.api.dto.tilBrev
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService.Companion.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.DbJournalpost
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.DbJournalpostType
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringRepository
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.settpåvent.SettPåVentService
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class BrevService(
    private val brevKlient: BrevKlient,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val utgåendeJournalføringService: UtgåendeJournalføringService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val settPåVentService: SettPåVentService,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val journalføringRepository: JournalføringRepository
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
        Result.runCatching {
            val brev = manueltBrevRequest.tilBrev()
            return brevKlient.genererBrev(
                målform = manueltBrevRequest.mottakerMålform.tilSanityFormat(),
                brev = brev
            )
        }.fold(
            onSuccess = { it },
            onFailure = {
                if (it is Feil) {
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
        )
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

        val fagsak = fagsakRepository.finnFagsak(behandling.fagsak.id)

        val journalpostId = utgåendeJournalføringService.journalførDokument(
            fnr = fagsak!!.aktør.aktivFødselsnummer(),
            fagsakId = fagsak.id,
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

        if (behandling != null) {
            journalføringRepository.save(
                DbJournalpost(
                    behandling = behandling,
                    journalpostId = journalpostId,
                    type = DbJournalpostType.U
                )
            )
        }

        if ((
            manueltBrevDto.brevmal == Brevmal.INNHENTE_OPPLYSNINGER ||
                manueltBrevDto.brevmal == Brevmal.VARSEL_OM_REVURDERING
            ) && behandling != null
        ) {
            leggTilOpplysningspliktIVilkårsvurdering(behandling)
        }

        // TODO: Legg inn kode for å opprette DistribuerDokumentTask:

        //        DistribuerDokumentTask.opprettDistribuerDokumentTask(
        //            distribuerDokumentDTO = DistribuerDokumentDTO(
        //                personEllerInstitusjonIdent = manueltBrevRequest.mottakerIdent,
        //                behandlingId = behandling?.id,
        //                journalpostId = journalpostId,
        //                brevmal = manueltBrevRequest.brevmal,
        //                erManueltSendt = true
        //            ),
        //            properties = Properties().apply {
        //                this["fagsakIdent"] = behandling?.fagsak?.aktør?.aktivFødselsnummer() ?: ""
        //                this["mottakerIdent"] = manueltBrevRequest.mottakerIdent
        //                this["journalpostId"] = journalpostId
        //                this["behandlingId"] = behandling?.id.toString()
        //                this["fagsakId"] = fagsakId.toString()
        //            }
        //        ).also {
        //            taskRepository.save(it)
        //        }

        if (
            behandling != null &&
            manueltBrevDto.brevmal.setterBehandlingPåVent()
        ) {
            settPåVentService.settBehandlingPåVent(
                behandlingId = behandling.id,
                frist = LocalDate.now()
                    .plusDays(
                        manueltBrevDto.brevmal.ventefristDager(
                            manuellFrist = manueltBrevDto.antallUkerSvarfrist?.toLong(),
                            behandlingKategori = behandling.kategori
                        )
                    ),
                årsak = manueltBrevDto.brevmal.venteårsak()
            )
        }
    }

    private fun leggTilOpplysningspliktIVilkårsvurdering(behandling: Behandling) {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)
        vilkårsvurdering.personResultater.single { it.erSøkersResultater() }
            .leggTilBlankAnnenVurdering(AnnenVurderingType.OPPLYSNINGSPLIKT)
    }
}
