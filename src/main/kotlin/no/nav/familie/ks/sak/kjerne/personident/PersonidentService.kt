package no.nav.familie.ks.sak.kjerne.personident

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.pdl.PdlClient
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlIdent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PersonidentService(
    private val personidentRepository: PersonidentRepository,
    private val aktørRepository: AktørRepository,
    private val pdlClient: PdlClient
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentOgLagreAktør(personIdentEllerAktørId: String, skalLagre: Boolean): Aktør {
        // hent Aktør hvis det allerede er lagret
        val aktør = personidentRepository.findByFødselsnummerOrNull(personIdentEllerAktørId)?.aktør
            ?: aktørRepository.findByAktørId(personIdentEllerAktørId)
        if (aktør != null) return aktør

        val pdlIdenter = hentIdenter(personIdentEllerAktørId, false)
        val aktivFødselsnummer = filtrerAktivtFødselsnummer(pdlIdenter)

        // Hent aktør fra aktivFødselsnummer når aktivFødselsnummer er annerledes enn søk param og er lagret i system
        val personident = personidentRepository.findByFødselsnummerOrNull(aktivFødselsnummer)
        if (personident != null) return personident.aktør

        val aktørId: String = filtrerAktørId(pdlIdenter)
        return aktørRepository.findByAktørId(aktørId)?.let { opprettPersonIdent(it, aktivFødselsnummer, skalLagre) }
            ?: opprettAktørIdOgPersonident(aktørId, aktivFødselsnummer, skalLagre)
    }

    fun hentAktør(identEllerAktørId: String): Aktør {
        val aktør = hentOgLagreAktør(personIdentEllerAktørId = identEllerAktørId, skalLagre = false)

        if (aktør.personidenter.find { it.aktiv } == null) {
            secureLogger.warn("Fant ikke aktiv ident for aktør med id ${aktør.aktørId} for ident $identEllerAktørId")
            throw Feil("Fant ikke aktiv ident for aktør")
        }
        return aktør
    }

    private fun opprettPersonIdent(aktør: Aktør, fødselsnummer: String, kanLagre: Boolean = true): Aktør {
        secureLogger.info("Oppretter personIdent. aktørIdStr=${aktør.aktørId} fødselsnummer=$fødselsnummer skal lagre=$kanLagre")
        val eksisterendePersonIdent = aktør.personidenter.find { it.fødselsnummer == fødselsnummer && it.aktiv }
        if (eksisterendePersonIdent == null) {
            aktør.personidenter.filter { it.aktiv }.map {
                it.aktiv = false
                it.gjelderTil = LocalDateTime.now()
            }
            // Må lagre her fordi unik index er en blanding av aktørid og gjelderTil,
            // og hvis man ikke lagerer før man legger til ny, så feiler det pga indexen.
            if (kanLagre) aktørRepository.saveAndFlush(aktør)

            aktør.personidenter.add(Personident(fødselsnummer = fødselsnummer, aktør = aktør))
            if (kanLagre) aktørRepository.saveAndFlush(aktør)
        }
        return aktør
    }

    private fun opprettAktørIdOgPersonident(aktørIdStr: String, fødselsnummer: String, kanLagre: Boolean): Aktør {
        secureLogger.info("Oppretter aktør og personIdent. aktørIdStr=$aktørIdStr fødselsnummer=$fødselsnummer skal lagre=$kanLagre")
        val aktør = Aktør(aktørId = aktørIdStr).also {
            it.personidenter.add(Personident(fødselsnummer = fødselsnummer, aktør = it))
        }
        return if (kanLagre) aktørRepository.saveAndFlush(aktør) else aktør
    }

    fun hentIdenter(personIdent: String, historikk: Boolean): List<PdlIdent> {
        return pdlClient.hentIdenter(personIdent, historikk)
    }

    private fun filtrerAktivtFødselsnummer(pdlIdenter: List<PdlIdent>) =
        pdlIdenter.singleOrNull { it.gruppe == "FOLKEREGISTERIDENT" }?.ident
            ?: throw Error("Finner ikke aktiv ident i Pdl")

    private fun filtrerAktørId(pdlIdenter: List<PdlIdent>): String =
        pdlIdenter.singleOrNull { it.gruppe == "AKTORID" }?.ident
            ?: throw Error("Finner ikke aktørId i Pdl")
}
