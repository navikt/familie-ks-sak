package no.nav.familie.ks.sak.integrasjon.oppgave

import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.enhet.Enhet
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.Arbeidsfordelingsenhet
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet.Companion.erGyldigBehandlendeKontantstøtteEnhet
import org.springframework.stereotype.Service

@Service
class TilpassArbeidsfordelingService(
    private val integrasjonClient: IntegrasjonClient,
) {
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
    ): NavIdent? =
        if (harSaksbehandlerTilgangTilEnhet(enhetId = arbeidsfordelingsenhet.enhetId, navIdent = navIdent)
        ) {
            navIdent
        } else {
            null
        }

    private fun harSaksbehandlerTilgangTilEnhet(
        enhetId: String,
        navIdent: NavIdent?,
    ): Boolean =
        navIdent?.let {
            hentGyldigeBehandlendeKontantstøtteEnheter(navIdent = navIdent)
                .any { it.enhetsnummer == enhetId }
        } ?: false

    private fun hentGyldigeBehandlendeKontantstøtteEnheter(navIdent: NavIdent): List<Enhet> =
        integrasjonClient
            .hentEnheterSomNavIdentHarTilgangTil(navIdent = navIdent)
            .filter { erGyldigBehandlendeKontantstøtteEnhet(it.enhetsnummer) }

    private fun håndterMidlertidigEnhet4863(
        navIdent: NavIdent?,
    ): Arbeidsfordelingsenhet {
        if (navIdent == null) {
            throw Feil("Kan ikke håndtere ${KontantstøtteEnhet.MIDLERTIDIG_ENHET} om man mangler NAV-ident")
        }
        val enheterNavIdentHarTilgangTil =
            hentGyldigeBehandlendeKontantstøtteEnheter(navIdent)
                .filter { it.enhetsnummer != KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer }
        if (enheterNavIdentHarTilgangTil.isEmpty()) {
            throw Feil("Fant ingen passende enhetsnummer for nav-ident $navIdent")
        }
        // Velger bare det første enhetsnummeret i tilfeller hvor man har flere, avklart med fag
        val nyBehandlendeEnhet = enheterNavIdentHarTilgangTil.first()
        return Arbeidsfordelingsenhet(
            nyBehandlendeEnhet.enhetsnummer,
            nyBehandlendeEnhet.enhetsnavn,
        )
    }

    private fun håndterVikafossenEnhet2103(
        navIdent: NavIdent?,
    ): Arbeidsfordelingsenhet {
        if (navIdent == null) {
            throw Feil("Kan ikke håndtere ${KontantstøtteEnhet.VIKAFOSSEN} om man mangler NAV-ident")
        }
        return Arbeidsfordelingsenhet(
            KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer,
            KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn,
        )
    }

    private fun håndterAndreEnheter(
        navIdent: NavIdent?,
        arbeidsfordelingsenhet: Arbeidsfordelingsenhet,
    ): Arbeidsfordelingsenhet {
        if (navIdent == null) {
            // navIdent er null ved automatisk journalføring
            return Arbeidsfordelingsenhet(
                arbeidsfordelingsenhet.enhetId,
                arbeidsfordelingsenhet.enhetNavn,
            )
        }
        val enheterNavIdentHarTilgangTil =
            hentGyldigeBehandlendeKontantstøtteEnheter(navIdent)
                .filter { it.enhetsnummer != KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer }
        if (enheterNavIdentHarTilgangTil.isEmpty()) {
            throw Feil("Fant ingen passende enhetsnummer for NAV-ident $navIdent")
        }
        val harTilgangTilBehandledeEnhet =
            enheterNavIdentHarTilgangTil.any {
                it.enhetsnummer == arbeidsfordelingsenhet.enhetId
            }
        if (!harTilgangTilBehandledeEnhet) {
            // Velger bare det første enhetsnummeret i tilfeller hvor man har flere, avklart med fag
            val nyBehandlendeEnhet = enheterNavIdentHarTilgangTil.first()
            return Arbeidsfordelingsenhet(
                nyBehandlendeEnhet.enhetsnummer,
                nyBehandlendeEnhet.enhetsnavn,
            )
        }
        return Arbeidsfordelingsenhet(
            arbeidsfordelingsenhet.enhetId,
            arbeidsfordelingsenhet.enhetNavn,
        )
    }
}
