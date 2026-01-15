package no.nav.familie.ks.sak.kjerne.falskidentitet

import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.integrasjon.pdl.PdlKlient
import no.nav.familie.ks.sak.integrasjon.pdl.domene.FalskIdentitetPersonInfo
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.adresser.Adresser
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FalskIdentitetService(
    val personRepository: PersonRepository,
    val pdlRestKlient: PdlKlient,
    val featureToggleService: FeatureToggleService,
) {
    fun hentFalskIdentitet(aktør: Aktør): FalskIdentitetPersonInfo? {
        if (!featureToggleService.isEnabled(FeatureToggle.SKAL_HÅNDTERE_FALSK_IDENTITET)) {
            secureLogger.info("Feature toggle: 'SKAL_HÅNDTERE_FALSK_IDENTITET' er deaktivert, eller ${SikkerhetContext.hentSaksbehandlerEpost()} er ikke lagt inn i listen over e-poster som har tilgang til å håndtere falske identiteter.")
            return null
        }
        val pdlFalskIdentitet = pdlRestKlient.hentFalskIdentitet(aktør.aktivFødselsnummer())
        if (pdlFalskIdentitet != null && pdlFalskIdentitet.erFalsk) {
            val personer = personRepository.findByAktør(aktør)
            val person = personer.firstOrNull { it.personopplysningGrunnlag.aktiv } ?: personer.firstOrNull()

            // Vurdere å bruke mer informasjon fra PdlFalskIdentitet hvis tilgjengelig
            // Henter navn, fødselsdato, kjønn og adresser fra personopplysningene i stedet for PDL
            return FalskIdentitetPersonInfo(
                navn = person?.navn ?: "Ukjent navn",
                fødselsdato = person?.fødselsdato,
                kjønn = person?.kjønn ?: Kjønn.UKJENT,
                adresser = person?.let { Adresser.opprettFra(it) },
            )
        } else {
            return null
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(FalskIdentitetService::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
