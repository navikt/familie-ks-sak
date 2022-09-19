package no.nav.familie.ks.sak.integrasjon.familieintegrasjon

import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.api.dto.PersonInfoDto
import no.nav.familie.ks.sak.integrasjon.pdl.PdlClient
import no.nav.familie.ks.sak.integrasjon.pdl.tilAdressebeskyttelse
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Service

@Service
class IntegrasjonService(private val integrasjonClient: IntegrasjonClient, private val pdlClient: PdlClient) {

    fun sjekkTilgangTilPersoner(personIdenter: List<String>): Tilgang = integrasjonClient.sjekkTilgangTilPersoner(personIdenter)

    fun hentMaskertPersonInfoVedManglendeTilgang(aktør: Aktør): PersonInfoDto? {
        val harTilgang = sjekkTilgangTilPersoner(listOf(aktør.aktivFødselsnummer())).harTilgang
        return if (!harTilgang) {
            val adressebeskyttelse = pdlClient.hentAdressebeskyttelse(aktør).tilAdressebeskyttelse()
            PersonInfoDto(
                personIdent = aktør.aktivFødselsnummer(),
                adressebeskyttelseGradering = adressebeskyttelse,
                harTilgang = false
            )
        } else {
            null
        }
    }
}
