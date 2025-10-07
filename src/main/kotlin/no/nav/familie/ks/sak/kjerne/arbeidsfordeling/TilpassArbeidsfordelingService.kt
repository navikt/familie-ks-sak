package no.nav.familie.ks.sak.kjerne.arbeidsfordeling

import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.containsExactly
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.Arbeidsfordelingsenhet
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TilpassArbeidsfordelingService(
    private val integrasjonClient: IntegrasjonClient,
) {
    private val logger = LoggerFactory.getLogger(TilpassArbeidsfordelingService::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun tilpassArbeidsfordelingsenhetTilSaksbehandler(
        arbeidsfordelingsenhet: Arbeidsfordelingsenhet,
        navIdent: NavIdent?,
    ): Arbeidsfordelingsenhet =
        when (arbeidsfordelingsenhet.enhetId) {
            KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer -> håndterMidlertidigEnhet4863(navIdent)
            KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer -> håndterVikafossenEnhet2103(navIdent)
            else -> håndterAndreEnheter(navIdent, arbeidsfordelingsenhet)
        }

    fun bestemTilordnetRessursPåOppgave(
        arbeidsfordelingsenhet: Arbeidsfordelingsenhet,
        navIdent: NavIdent?,
    ): NavIdent? {
        if (navIdent?.erSystemIdent() == true) {
            return null
        }
        return if (harSaksbehandlerTilgangTilEnhet(enhetId = arbeidsfordelingsenhet.enhetId, navIdent = navIdent)) {
            navIdent
        } else {
            null
        }
    }

    private fun harSaksbehandlerTilgangTilEnhet(
        enhetId: String,
        navIdent: NavIdent?,
    ): Boolean =
        navIdent?.let {
            integrasjonClient
                .hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent = navIdent)
                .any { it.enhetsnummer == enhetId }
        } ?: false

    private fun håndterMidlertidigEnhet4863(
        navIdent: NavIdent?,
    ): Arbeidsfordelingsenhet {
        if (navIdent == null) {
            håndterNavIdentErNull(KontantstøtteEnhet.MIDLERTIDIG_ENHET)
        }
        if (navIdent.erSystemIdent()) {
            logger.error("Kan ikke håndtere ${KontantstøtteEnhet.MIDLERTIDIG_ENHET} i automatiske behandlinger.")
            throw Feil("Kan ikke håndtere ${KontantstøtteEnhet.MIDLERTIDIG_ENHET} i automatiske behandlinger.")
        }

        val enheterNavIdentHarTilgangTil = hentEnheterNavIdentHarTilgangTil(navIdent)

        val navIdentHarKunTilgangTilVikafossen = enheterNavIdentHarTilgangTil.inneholderKunVikafossen()
        if (navIdentHarKunTilgangTilVikafossen) {
            // Skal kun være lovt til å sette Vikafossen når det er eneste valgmulighet
            return Arbeidsfordelingsenhet.opprettFra(KontantstøtteEnhet.VIKAFOSSEN)
        }
        val enheterNavIdentHarTilgangTilForutenVikafossen = enheterNavIdentHarTilgangTil.filter { it.enhetsnummer != KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer }
        // Velger bare det første enhetsnummeret i tilfeller hvor man har flere, avklart med fag
        val nyBehandlendeEnhet = enheterNavIdentHarTilgangTilForutenVikafossen.first()
        return Arbeidsfordelingsenhet.opprettFra(nyBehandlendeEnhet)
    }

    private fun håndterVikafossenEnhet2103(
        navIdent: NavIdent?,
    ): Arbeidsfordelingsenhet {
        if (navIdent == null) {
            håndterNavIdentErNull(KontantstøtteEnhet.VIKAFOSSEN)
        }
        return Arbeidsfordelingsenhet.opprettFra(KontantstøtteEnhet.VIKAFOSSEN)
    }

    private fun håndterAndreEnheter(
        navIdent: NavIdent?,
        arbeidsfordelingsenhet: Arbeidsfordelingsenhet,
    ): Arbeidsfordelingsenhet {
        if (navIdent == null || navIdent.erSystemIdent()) {
            // navIdent er null ved automatisk journalføring
            return arbeidsfordelingsenhet
        }
        val enheterNavIdentHarTilgangTil = hentEnheterNavIdentHarTilgangTil(navIdent = navIdent)
        val navIdentHarKunTilgangTilVikafossen = enheterNavIdentHarTilgangTil.inneholderKunVikafossen()
        if (navIdentHarKunTilgangTilVikafossen) {
            // Skal kun være lovt til å sette Vikafossen når det er eneste valgmulighet
            return Arbeidsfordelingsenhet.opprettFra(KontantstøtteEnhet.VIKAFOSSEN)
        }
        val enheterNavIdentHarTilgangTilForutenVikafossen = enheterNavIdentHarTilgangTil.filter { it.enhetsnummer != KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer }
        val harTilgangTilBehandledeEnhet = enheterNavIdentHarTilgangTilForutenVikafossen.any { it.enhetsnummer == arbeidsfordelingsenhet.enhetId }
        if (!harTilgangTilBehandledeEnhet) {
            // Velger bare det første enhetsnummeret i tilfeller hvor man har flere, avklart med fag
            val nyBehandlendeEnhet = enheterNavIdentHarTilgangTilForutenVikafossen.first()
            return Arbeidsfordelingsenhet.opprettFra(nyBehandlendeEnhet)
        }
        return arbeidsfordelingsenhet
    }

    private fun hentEnheterNavIdentHarTilgangTil(navIdent: NavIdent): List<KontantstøtteEnhet> {
        val enheterNavIdentHarTilgangTil = integrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent = navIdent)
        if (enheterNavIdentHarTilgangTil.isEmpty()) {
            logger.warn("Nav-Ident har ikke tilgang til noen enheter. Se SecureLogs for detaljer.")
            secureLogger.warn("Nav-Ident $navIdent har ikke tilgang til noen enheter.")
            throw FunksjonellFeil("Nav-Ident har ikke tilgang til noen enheter.")
        }
        return enheterNavIdentHarTilgangTil
    }

    private fun håndterNavIdentErNull(kontantstøtteEnhet: KontantstøtteEnhet): Nothing {
        logger.error("Kan ikke håndtere $kontantstøtteEnhet om man mangler NAV-ident.")
        throw Feil("Kan ikke håndtere $kontantstøtteEnhet om man mangler NAV-ident.")
    }

    private fun List<KontantstøtteEnhet>.inneholderKunVikafossen(): Boolean = this.map { it.enhetsnummer }.containsExactly(KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer)

    private fun NavIdent.erSystemIdent(): Boolean = this.ident == SYSTEM_FORKORTELSE
}
