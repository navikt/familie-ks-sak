
package no.nav.familie.ks.sak.kjerne.personident

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.config.PersonInfoQuery
import no.nav.familie.ks.sak.integrasjon.pdl.PdlClient
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlIdent
import no.nav.familie.ks.sak.integrasjon.pdl.domene.hentAktivAktørId
import no.nav.familie.ks.sak.integrasjon.pdl.domene.hentAktivFødselsnummer
import no.nav.familie.ks.sak.integrasjon.pdl.domene.hentAktørIder
import no.nav.familie.ks.sak.integrasjon.pdl.domene.hentFødselsnumre
import no.nav.familie.ks.sak.integrasjon.pdl.tilPersonInfo
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HåndterNyIdentService(
    private val aktørIdRepository: AktørRepository,
    private val fagsakService: FagsakService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val behandlingRepository: BehandlingRepository,
    private val pdlClient: PdlClient,
    private val personIdentService: PersonidentService,
    private val behandlingService: BehandlingService,
) {
    @Transactional
    fun håndterNyIdent(nyIdent: PersonIdent): Aktør? {
        logger.info("Håndterer ny ident")
        secureLogger.info("Håndterer ny ident ${nyIdent.ident}")
        val identerFraPdl = personIdentService.hentIdenter(nyIdent.ident, true)

        val aktørId = identerFraPdl.hentAktivAktørId()
        val aktør = aktørIdRepository.findByAktørId(aktørId)
        val aktuelleFagsakerForIdenter = hentAktuelleFagsaker(identerFraPdl)

        return when {
            // Personen er ikke i noen fagsaker
            aktuelleFagsakerForIdenter.isNullOrEmpty() -> aktør

            // Ny aktørId, nytt fødselsnummer -> begge håndteres i PatchMergetIdentTask
            aktør == null -> {
                aktuelleFagsakerForIdenter.forEach { fagsak ->
                    validerUendretFødselsdatoFraForrigeBehandling(identerFraPdl, fagsak)
                }
                // patcheendepunktet trenger en fagsak, men samme hvilken fagsak
                opprettMergeIdentTask(aktuelleFagsakerForIdenter.first().id, identerFraPdl)
                null
            }

            // Samme aktørId, nytt fødselsnummer -> legg til fødselsnummer på aktør
            !aktør.harIdent(fødselsnummer = nyIdent.ident) -> {
                aktuelleFagsakerForIdenter.forEach { fagsak ->
                    validerUendretFødselsdatoFraForrigeBehandling(identerFraPdl, fagsak)
                }
                logger.info("Legger til ny ident")
                secureLogger.info("Legger til ny ident ${nyIdent.ident} på aktør ${aktør.aktørId}")
                personIdentService.opprettPersonIdent(aktør, nyIdent.ident)
            }

            // Samme aktørId, samme fødselsnummer -> ignorer hendelse
            else -> aktør
        }
    }

    private fun opprettMergeIdentTask(
        fagsakId: Long,
        identerFraPdl: List<PdlIdent>,
    ): Task {
        val fødeslsnumre = identerFraPdl.hentFødselsnumre()

        val behandlingerPåFagsak = behandlingRepository.finnBehandlinger(fagsakId)
        val aktiveFødselsnummerForFagsak = behandlingRepository.finnAktivtFødselsnummerForBehandlinger(behandlingerPåFagsak.map { it.id }).map { it.second }.toSet()
        val gammelIdent =
            aktiveFødselsnummerForFagsak.singleOrNull {
                it in fødeslsnumre
            } ?: throw Feil("Fant ikke gammel ident for aktør ${identerFraPdl.hentAktivAktørId()} på fagsak $fagsakId")

        val task =
            PatchMergetIdentTask.opprettTask(
                PatchMergetIdentDto(
                    fagsakId = fagsakId,
                    nyIdent = PersonIdent(identerFraPdl.hentAktivFødselsnummer()),
                    gammelIdent = PersonIdent(gammelIdent),
                ),
            )
        secureLogger.info("Potensielt merget ident for $identerFraPdl")
        return task
    }

    private fun hentAktuelleFagsaker(alleIdenterFraPdl: List<PdlIdent>): List<Fagsak> {
        val aktørerMedAktivPersonident =
            alleIdenterFraPdl
                .hentAktørIder()
                .mapNotNull { aktørIdRepository.findByAktørId(it) }
                .filter { aktør -> aktør.personidenter.any { personident -> personident.aktiv } }

        return aktørerMedAktivPersonident
            .flatMap { aktør -> fagsakService.hentFagsakerPåPerson(aktør) }
    }

    private fun validerUendretFødselsdatoFraForrigeBehandling(
        alleIdenterFraPdl: List<PdlIdent>,
        fagsak: Fagsak,
    ) {
        // Hvis søkers fødselsdato endrer seg kan vi alltid patche siden det ikke påvirker andeler.
        val søkersAktørId = fagsak.aktør.aktørId
        if (søkersAktørId in alleIdenterFraPdl.hentAktørIder()) return

        val aktivFødselsnummer = alleIdenterFraPdl.hentAktivFødselsnummer()
        val fødselsdatoFraPdl = tilPersonInfo(pdlClient.hentPerson(aktivFødselsnummer, PersonInfoQuery.ENKEL)).fødselsdato

        val forrigeBehandling =
            behandlingService.hentSisteBehandlingSomErVedtatt(fagsak.id)
                ?: return // Hvis det ikke er noen tidligere behandling kan vi patche uansett

        val aktørIder = alleIdenterFraPdl.hentAktørIder()
        val personGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(forrigeBehandling.id) ?: throw Feil("Fant ikke persongrunnlag for behandling med id ${forrigeBehandling.id}")
        val fødselsdatoForrigeBehandling =
            personGrunnlag.personer.singleOrNull { it.aktør.aktørId in aktørIder }?.fødselsdato
                ?: return // Hvis aktør ikke er med i forrige behandling kan vi patche selv om fødselsdato er ulik

        if (fødselsdatoFraPdl.toYearMonth() != fødselsdatoForrigeBehandling.toYearMonth()) {
            throw Feil("Fødselsdato er forskjellig fra forrige behandling. Må patche ny ident manuelt. $LENKE_INFO_OM_MERGING")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        const val LENKE_INFO_OM_MERGING: String =
            "Se https://github.com/navikt/familie/blob/main/doc/ba-sak/manuellt-patche-akt%C3%B8r-sak.md#manuell-patching-av-akt%C3%B8r-for-en-behandling for mer info."
    }
}
