package no.nav.familie.ks.sak.integrasjon.oppgave

import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet.Companion.erGyldigBehandlendeKontantstøtteEnhet
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import org.springframework.stereotype.Service

@Service
class OppgaveArbeidsfordelingService(
    private val integrasjonClient: IntegrasjonClient,
) {
    fun finnArbeidsfordelingForOppgave(
        arbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling,
        navIdent: NavIdent?,
    ): OppgaveArbeidsfordeling =
        when (arbeidsfordelingPåBehandling.behandlendeEnhetId) {
            KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer -> håndterMidlertidigEnhet4863(navIdent)
            KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer -> håndterVikafossenEnhet2103(navIdent)
            else -> håndterAndreEnheter(navIdent, arbeidsfordelingPåBehandling)
        }

    private fun håndterMidlertidigEnhet4863(
        navIdent: NavIdent?,
    ): OppgaveArbeidsfordeling {
        if (navIdent == null) {
            throw Feil("Kan ikke sette ${KontantstøtteEnhet.MIDLERTIDIG_ENHET} om man mangler NAV-ident")
        }
        val enheterNavIdentHarTilgangTil =
            integrasjonClient
                .hentEnheterSomNavIdentHarTilgangTil(navIdent)
                .filter { erGyldigBehandlendeKontantstøtteEnhet(it.enhetsnummer) }
                .filter { it.enhetsnummer != KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer }
        if (enheterNavIdentHarTilgangTil.isEmpty()) {
            throw Feil("Fant ingen passende enhetsnummer for nav-ident $navIdent")
        }
        // Velger bare det første enhetsnummeret i tilfeller hvor man har flere, avklart med fag
        val nyBehandlendeEnhet = enheterNavIdentHarTilgangTil.first()
        return OppgaveArbeidsfordeling(
            navIdent,
            nyBehandlendeEnhet.enhetsnummer,
            nyBehandlendeEnhet.enhetsnavn,
        )
    }

    private fun håndterVikafossenEnhet2103(
        navIdent: NavIdent?,
    ): OppgaveArbeidsfordeling {
        if (navIdent == null) {
            throw Feil("Kan ikke sette ${KontantstøtteEnhet.VIKAFOSSEN} om man mangler NAV-ident")
        }
        val harTilgangTilVikafossenEnhet2103 =
            integrasjonClient
                .hentEnheterSomNavIdentHarTilgangTil(navIdent)
                .filter { erGyldigBehandlendeKontantstøtteEnhet(it.enhetsnummer) }
                .any { it.enhetsnummer == KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer }
        if (!harTilgangTilVikafossenEnhet2103) {
            return OppgaveArbeidsfordeling(
                null,
                KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer,
                KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn,
            )
        }
        return OppgaveArbeidsfordeling(
            navIdent,
            KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer,
            KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn,
        )
    }

    private fun håndterAndreEnheter(
        navIdent: NavIdent?,
        arbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling,
    ): OppgaveArbeidsfordeling {
        if (navIdent == null) {
            // navIdent er null ved automatisk journalføring
            return OppgaveArbeidsfordeling(
                null,
                arbeidsfordelingPåBehandling.behandlendeEnhetId,
                arbeidsfordelingPåBehandling.behandlendeEnhetNavn,
            )
        }
        val enheterNavIdentHarTilgangTil =
            integrasjonClient
                .hentEnheterSomNavIdentHarTilgangTil(navIdent)
                .filter { erGyldigBehandlendeKontantstøtteEnhet(it.enhetsnummer) }
                .filter { it.enhetsnummer != KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer }
        if (enheterNavIdentHarTilgangTil.isEmpty()) {
            throw Feil("Fant ingen passende enhetsnummer for NAV-ident $navIdent")
        }
        val harTilgangTilBehandledeEnhet =
            enheterNavIdentHarTilgangTil.any {
                it.enhetsnummer == arbeidsfordelingPåBehandling.behandlendeEnhetId
            }
        if (!harTilgangTilBehandledeEnhet) {
            // Velger bare det første enhetsnummeret i tilfeller hvor man har flere, avklart med fag
            val nyBehandlendeEnhet = enheterNavIdentHarTilgangTil.first()
            return OppgaveArbeidsfordeling(
                navIdent,
                nyBehandlendeEnhet.enhetsnummer,
                nyBehandlendeEnhet.enhetsnavn,
            )
        }
        return OppgaveArbeidsfordeling(
            navIdent,
            arbeidsfordelingPåBehandling.behandlendeEnhetId,
            arbeidsfordelingPåBehandling.behandlendeEnhetNavn,
        )
    }
}

data class OppgaveArbeidsfordeling(
    val navIdent: NavIdent?,
    val enhetsnummer: String,
    val enhetsnavn: String,
) {
    init {
        require(enhetsnummer.length == 4) { "Enhetsnummer må være 4 siffer" }
    }
}