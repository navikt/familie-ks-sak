package no.nav.familie.ks.sak.api.ekstern

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.erAlfanummeriskPlussKolon
import no.nav.familie.ks.sak.integrasjon.pdl.secureLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RestJournalFøring {
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    data class NavnOgIdent(
        val navn: String,
        val id: String,
    ) {
        // Bruker init til å validere personidenten
        init {
            if (!id.erAlfanummeriskPlussKolon()) {
                secureLogger.info("Ugyldig ident: $id")
                throw FunksjonellFeil(
                    melding = "Ugyldig ident. Se securelog for mer informasjon.",
                    frontendFeilmelding = "Ugyldig ident. Normalt et fødselsnummer eller organisasjonsnummer",
                )
            }
        }
    }
}
