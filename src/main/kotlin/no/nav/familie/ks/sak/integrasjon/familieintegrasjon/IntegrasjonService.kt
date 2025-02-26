package no.nav.familie.ks.sak.integrasjon.familieintegrasjon

import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.api.dto.PersonInfoDto
import no.nav.familie.ks.sak.integrasjon.pdl.PdlClient
import no.nav.familie.ks.sak.integrasjon.pdl.tilAdressebeskyttelse
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Service

@Service
class IntegrasjonService(
    private val integrasjonClient: IntegrasjonClient,
    private val pdlClient: PdlClient,
) {
    fun sjekkTilgangTilPerson(personIdent: String): Tilgang = sjekkTilgangTilPersoner(listOf(personIdent)).values.single()

    fun sjekkTilgangTilPersoner(personIdenter: List<String>): Map<String, Tilgang> = integrasjonClient.sjekkTilgangTilPersoner(personIdenter).associateBy { it.personIdent }

    fun hentMaskertPersonInfoVedManglendeTilgang(aktør: Aktør): PersonInfoDto? {
        val harTilgang = sjekkTilgangTilPerson(aktør.aktivFødselsnummer()).harTilgang
        return if (!harTilgang) {
            val adressebeskyttelse = pdlClient.hentAdressebeskyttelse(aktør).tilAdressebeskyttelse()
            PersonInfoDto(
                personIdent = aktør.aktivFødselsnummer(),
                adressebeskyttelseGradering = adressebeskyttelse,
                harTilgang = false,
            )
        } else {
            null
        }
    }

    fun hentJournalpost(journalpostId: String): Journalpost = integrasjonClient.hentJournalpost(journalpostId)
}
