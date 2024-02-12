package no.nav.familie.ks.sak.kjerne.brev.mottaker

import no.nav.familie.ks.sak.api.dto.BrevmottakerDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.springframework.stereotype.Service

@Service
class ValiderBrevmottakerService(
    private val brevmottakerRepository: BrevmottakerRepository,
    private val persongrunnlagService: PersonopplysningGrunnlagService,
    private val personOpplysningerService: PersonOpplysningerService,
    private val fagsakRepository: FagsakRepository,
) {
    fun validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
        behandlingId: Long,
        nyBrevmottaker: BrevmottakerDb? = null,
        ekstraBarnLagtTilIBrev: List<String>,
    ) {
        var brevmottakere = brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId)
        nyBrevmottaker?.let {
            brevmottakere += it
        }
        if (brevmottakere.isEmpty()) return

        val personopplysningGrunnlag =
            persongrunnlagService.finnAktivPersonopplysningGrunnlag(behandlingId = behandlingId) ?: return
        val personIdenter =
            personopplysningGrunnlag.personer
                .takeIf { it.isNotEmpty() }
                ?.map { it.aktør.aktivFødselsnummer() }
                ?: return
        val strengtFortroligePersonIdenter =
            personOpplysningerService.hentIdenterMedStrengtFortroligAdressebeskyttelse(
                (personIdenter + ekstraBarnLagtTilIBrev).toSet().toList(),
            )
        if (strengtFortroligePersonIdenter.isNotEmpty()) {
            val melding =
                "Behandlingen (id: $behandlingId) inneholder ${strengtFortroligePersonIdenter.size} person(er) med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere (${brevmottakere.size} stk)."
            val frontendFeilmelding =
                "Behandlingen inneholder personer med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere."
            throw FunksjonellFeil(melding, frontendFeilmelding)
        }
    }

    fun validerAtFagsakIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
        fagsakId: Long,
        manuelleBrevmottakere: List<BrevmottakerDto>,
        barnLagtTilIBrev: List<String>,
    ) {
        val erManuellBrevmottaker = manuelleBrevmottakere.isNotEmpty()
        if (!erManuellBrevmottaker) return

        val fagsak = fagsakRepository.finnFagsak(fagsakId) ?: throw Feil("Fant ikke fagsak $fagsakId")
        val strengtFortroligePersonIdenter =
            personOpplysningerService.hentIdenterMedStrengtFortroligAdressebeskyttelse(listOf(fagsak.aktør.aktivFødselsnummer()) + barnLagtTilIBrev)

        if (strengtFortroligePersonIdenter.isNotEmpty()) {
            val melding =
                "Brev på fagsak $fagsakId inneholder person med strengt fortrolig adressebeskyttelse og kan ikke sendes til manuelle brevmottakere (${manuelleBrevmottakere.size} stk)."
            val frontendFeilmelding =
                "Brevet inneholder personer med strengt fortrolig adressebeskyttelse og kan ikke sendes til manuelle brevmottakere."
            throw FunksjonellFeil(melding, frontendFeilmelding)
        }
    }
}
