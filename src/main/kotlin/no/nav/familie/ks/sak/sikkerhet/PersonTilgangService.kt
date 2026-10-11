package no.nav.familie.ks.sak.sikkerhet

import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.api.dto.PersonInfoDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.integrasjon.pdl.PdlKlient
import no.nav.familie.ks.sak.integrasjon.pdl.tilAdressebeskyttelse
import no.nav.familie.ks.sak.integrasjon.tilgangsmaskin.TilgangsmaskinTilgangskontrollKlient
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.tilgangsmaskin.Avvisningskode
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class PersonTilgangService(
    private val tilgangsmaskinTilgangskontrollKlient: TilgangsmaskinTilgangskontrollKlient,
    private val integrasjonKlient: IntegrasjonKlient,
    private val featureToggleService: FeatureToggleService,
    private val pdlKlient: PdlKlient,
) {
    fun sjekkTilgangTilPerson(personIdent: String): PersonTilgang = sjekkTilgangTilPersoner(setOf(personIdent)).getValue(personIdent)

    fun skalBrukeTilgangsmaskinen(): Boolean = featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN)

    fun sjekkTilgangTilPersoner(personIdenter: Set<String>): Map<String, PersonTilgang> = sjekkTilgangTilPersoner(personIdenter, skalBrukeTilgangsmaskinen())

    fun sjekkTilgangTilPersoner(
        personIdenter: Set<String>,
        skalBrukeTilgangsmaskinen: Boolean,
    ): Map<String, PersonTilgang> {
        val tilganger =
            if (skalBrukeTilgangsmaskinen) {
                tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(personIdenter)
            } else {
                sjekkTilgangTilPersonerIFamilieIntegrasjoner(personIdenter)
            }
        return tilganger.associateBy { it.personIdent }
    }

    private fun sjekkTilgangTilPersonerIFamilieIntegrasjoner(personIdenter: Set<String>): List<PersonTilgang> {
        val tilgangPerIdent =
            integrasjonKlient
                .sjekkTilgangTilPersoner(personIdenter.toList())
                .associateBy { it.personIdent }
        val identerUtenSvar = personIdenter.filterNot { it in tilgangPerIdent }
        if (identerUtenSvar.isNotEmpty()) {
            throw Feil(
                message = "Fikk ikke svar fra familie-integrasjoner for ${identerUtenSvar.size} av ${personIdenter.size} identer.",
                frontendFeilmelding = "Klarte ikke å sjekke tilgang til personene. Prøv igjen senere.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            )
        }
        return personIdenter.map { tilgangPerIdent.getValue(it).tilPersonTilgang() }
    }

    // familie-integrasjoner oppgir ikke hvilken regel som avviste tilgangen, så avvisningskoden blir UKJENT.
    private fun Tilgang.tilPersonTilgang(): PersonTilgang =
        if (harTilgang) {
            PersonTilgang.medTilgang(personIdent)
        } else {
            PersonTilgang.avvist(
                personIdent = personIdent,
                avvisningskode = Avvisningskode.UKJENT,
                begrunnelse = begrunnelse ?: "familie-integrasjoner ga ikke tilgang til personen",
            )
        }

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
