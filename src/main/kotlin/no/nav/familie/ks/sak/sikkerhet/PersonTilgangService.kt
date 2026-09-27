package no.nav.familie.ks.sak.sikkerhet

import no.nav.familie.ks.sak.api.dto.PersonInfoDto
import no.nav.familie.ks.sak.integrasjon.pdl.PdlKlient
import no.nav.familie.ks.sak.integrasjon.pdl.tilAdressebeskyttelse
import no.nav.familie.ks.sak.integrasjon.tilgangsmaskin.TilgangsmaskinTilgangskontrollKlient
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Service

@Service
class PersonTilgangService(
    private val tilgangsmaskinTilgangskontrollKlient: TilgangsmaskinTilgangskontrollKlient,
    private val pdlKlient: PdlKlient,
) {
    fun sjekkTilgangTilPerson(personIdent: String): PersonTilgang = sjekkTilgangTilPersoner(setOf(personIdent)).getValue(personIdent)

    fun sjekkTilgangTilPersoner(personIdenter: Set<String>): Map<String, PersonTilgang> =
        tilgangsmaskinTilgangskontrollKlient
            .sjekkTilgangTilPersoner(personIdenter)
            .associateBy { it.personIdent }

    fun hentMaskertPersonInfoVedManglendeTilgang(aktør: Aktør): PersonInfoDto? {
        val harTilgang = sjekkTilgangTilPerson(aktør.aktivFødselsnummer()).harTilgang
        return if (!harTilgang) {
            val adressebeskyttelse = pdlKlient.hentAdressebeskyttelse(aktør).tilAdressebeskyttelse()
            PersonInfoDto(
                personIdent = aktør.aktivFødselsnummer(),
                adressebeskyttelseGradering = adressebeskyttelse,
                harTilgang = false,
            )
        } else {
            null
        }
    }
}
