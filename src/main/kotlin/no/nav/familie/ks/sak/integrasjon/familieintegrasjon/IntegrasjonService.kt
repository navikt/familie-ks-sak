package no.nav.familie.ks.sak.integrasjon.familieintegrasjon

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import org.springframework.stereotype.Service

@Service
class IntegrasjonService(
    private val integrasjonKlient: IntegrasjonKlient,
) {
    fun hentJournalpost(journalpostId: String): Journalpost = integrasjonKlient.hentJournalpost(journalpostId)

    fun hentAInntektUrl(personIdent: PersonIdent) = integrasjonKlient.hentAInntektUrl(personIdent)

    fun sjekkErEgenAnsattBulk(personIdenter: Set<String>) = integrasjonKlient.sjekkErEgenAnsatt(personIdenter)
}
