package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.api.dto.tilBrev
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class BrevService(
    private val brevKlient: BrevKlient,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService
) {

    fun hentForhåndsvisningAvBrev(behandlingId: Long, manueltBrevDto: ManueltBrevDto): ByteArray {
        val mottakerPerson = personopplysningGrunnlagService.hentSøker(behandlingId)
        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId)

        val manueltBrevDtoMedMottakerData = manueltBrevDto.copy(
            enhet = Enhet(
                enhetNavn = arbeidsfordelingPåBehandling.behandlendeEnhetNavn,
                enhetId = arbeidsfordelingPåBehandling.behandlendeEnhetId
            ),
            mottakerMålform = mottakerPerson?.målform ?: manueltBrevDto.mottakerMålform,
            mottakerNavn = mottakerPerson?.navn ?: manueltBrevDto.mottakerNavn
        )
        return genererManueltBrev(manueltBrevDtoMedMottakerData, true)
    }

    private fun genererManueltBrev(
        manueltBrevRequest: ManueltBrevDto,
        erForhåndsvisning: Boolean = false
    ): ByteArray {
        Result.runCatching {
            val brev: Brev = manueltBrevRequest.tilBrev()
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
}
