package no.nav.familie.ks.sak.kjerne.personident

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.integrasjon.pdl.PdlClient
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlIdent
import no.nav.familie.ks.sak.integrasjon.pdl.domene.hentAktivAktørId
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PersonidentService(
    private val personidentRepository: PersonidentRepository,
    private val aktørRepository: AktørRepository,
    private val pdlClient: PdlClient,
    private val taskService: TaskRepositoryWrapper,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentOgLagreAktør(
        personIdentEllerAktørId: String,
        skalLagre: Boolean,
    ): Aktør {
        // hent Aktør hvis det allerede er lagret
        val aktør =
            personidentRepository.findByFødselsnummerOrNull(personIdentEllerAktørId)?.aktør
                ?: aktørRepository.findByAktørId(personIdentEllerAktørId)

        aktør?.let { return it }

        val pdlIdenter = hentIdenter(personIdentEllerAktørId, false)
        val aktivtFødselsnummer = filtrerAktivtFødselsnummer(pdlIdenter)

        // Hent aktør fra aktivFødselsnummer når aktivFødselsnummer er annerledes enn søk param og er lagret i system
        val personident = personidentRepository.findByFødselsnummerOrNull(aktivtFødselsnummer)

        personident?.let { return it.aktør }

        val aktørId = filtrerAktørId(pdlIdenter)
        return aktørRepository.findByAktørId(aktørId)?.let { opprettPersonIdent(it, aktivtFødselsnummer, skalLagre) }
            ?: opprettAktørIdOgPersonident(aktørId, aktivtFødselsnummer, skalLagre)
    }

    fun hentAktør(identEllerAktørId: String): Aktør {
        val aktør = hentOgLagreAktør(personIdentEllerAktørId = identEllerAktørId, skalLagre = false)

        if (aktør.personidenter.none { it.aktiv }) {
            secureLogger.warn("Fant ikke aktiv ident for aktør med id ${aktør.aktørId} for ident $identEllerAktørId")
            throw Feil("Fant ikke aktiv ident for aktør")
        }
        return aktør
    }

    fun opprettPersonIdent(
        aktør: Aktør,
        fødselsnummer: String,
        skalLagre: Boolean = true,
    ): Aktør {
        secureLogger.info("Oppretter personIdent. aktørIdStr=${aktør.aktørId} fødselsnummer=$fødselsnummer skal lagre=$skalLagre")

        if (aktør.personidenter.none { it.fødselsnummer == fødselsnummer && it.aktiv }) {
            aktør.personidenter.filter { it.aktiv }.map {
                it.aktiv = false
                it.gjelderTil = LocalDateTime.now()
            }
            // Må lagre her fordi unik index er en blanding av aktørid og gjelderTil,
            // og hvis man ikke lagerer før man legger til ny, så feiler det pga indexen.
            if (skalLagre) aktørRepository.saveAndFlush(aktør)

            aktør.personidenter.add(Personident(fødselsnummer = fødselsnummer, aktør = aktør))
            if (skalLagre) aktørRepository.saveAndFlush(aktør)
        }
        return aktør
    }

    private fun opprettAktørIdOgPersonident(
        aktørIdStr: String,
        fødselsnummer: String,
        kanLagre: Boolean,
    ): Aktør {
        secureLogger.info("Oppretter aktør og personIdent. aktørIdStr=$aktørIdStr fødselsnummer=$fødselsnummer skal lagre=$kanLagre")

        val aktør =
            Aktør(aktørId = aktørIdStr).apply {
                personidenter.add(Personident(fødselsnummer = fødselsnummer, aktør = this))
            }

        return if (kanLagre) aktørRepository.saveAndFlush(aktør) else aktør
    }

    fun hentIdenter(
        personIdent: String,
        historikk: Boolean,
    ): List<PdlIdent> = pdlClient.hentIdenter(personIdent, historikk)

    fun opprettTaskForIdentHendelse(nyIdent: PersonIdent) {
        if (identSkalLeggesTil(nyIdent)) {
            logger.info("Oppretter task for senere håndterering av ny ident")
            secureLogger.info("Oppretter task for senere håndterering av ny ident ${nyIdent.ident}")
            taskService.save(IdentHendelseTask.opprettTask(nyIdent))
        } else {
            logger.info("Ident er ikke knyttet til noen av aktørene våre, ignorerer hendelse.")
        }
    }

    fun identSkalLeggesTil(nyIdent: PersonIdent): Boolean {
        val identerFraPdl = hentIdenter(nyIdent.ident, true)
        val aktører =
            identerFraPdl
                .filter { it.gruppe == "AKTORID" }
                .mapNotNull { aktørRepository.findByAktørId(it.ident) }

        if (aktører.isNotEmpty()) {
            return aktører.none { it.harIdent(nyIdent.ident) }
        }
        return false
    }

    private fun filtrerAktivtFødselsnummer(pdlIdenter: List<PdlIdent>) =
        pdlIdenter.singleOrNull { it.gruppe == "FOLKEREGISTERIDENT" }?.ident
            ?: throw PdlPersonKanIkkeBehandlesIFagsystem("Finner ikke aktiv ident i Pdl for personen")

    private fun filtrerAktørId(pdlIdenter: List<PdlIdent>): String =
        pdlIdenter.singleOrNull { it.gruppe == "AKTORID" }?.ident
            ?: throw Feil("Finner ikke aktørId i Pdl")

    private fun validerOmAktørIdErMerget(alleHistoriskeIdenterFraPdl: List<PdlIdent>) {
        val alleHistoriskeAktørIder = alleHistoriskeIdenterFraPdl.filter { it.gruppe == "AKTORID" && it.historisk }.map { it.ident }

        val aktiveAktørerForHistoriskAktørIder =
            alleHistoriskeAktørIder
                .mapNotNull { aktørId -> aktørRepository.findByAktørId(aktørId) }
                .filter { aktør -> aktør.personidenter.any { personident -> personident.aktiv } }

        if (aktiveAktørerForHistoriskAktørIder.isNotEmpty()) {
            secureLogger.warn("Potensielt merget ident for $alleHistoriskeIdenterFraPdl")
            throw Feil(
                message = "Mottok potensielt en hendelse på en merget ident for aktørId=${alleHistoriskeIdenterFraPdl.hentAktivAktørId()}. Sjekk securelogger for liste med identer. Sjekk om identen har flere saker. Disse må løses manuelt.",
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(PersonidentService::class.java)
    }
}
