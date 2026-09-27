package no.nav.familie.ks.sak.fake

import io.mockk.mockk
import no.nav.familie.ks.sak.integrasjon.tilgangsmaskin.TilgangsmaskinTilgangskontrollKlient
import no.nav.familie.ks.sak.sikkerhet.PersonTilgang
import no.nav.familie.tilgangsmaskin.Avvisningskode
import no.nav.familie.tilgangsmaskin.Regeltype
import no.nav.familie.tilgangsmaskin.TilgangsmaskinKlient
import no.nav.familie.tilgangsmaskin.TilgangsmaskinResultat
import java.net.URI

/**
 * Fake som bare erstatter kallet mot Tilgangsmaskinen. Resten av [TilgangsmaskinTilgangskontrollKlient] kjøres som i
 * produksjon, slik at systemkontekst, validering og mapping av svar også gjelder i testene.
 */
class FakeTilgangsmaskinTilgangskontrollKlient(
    private val tilgangsmaskin: StyrbarTilgangsmaskinKlient = StyrbarTilgangsmaskinKlient(),
) : TilgangsmaskinTilgangskontrollKlient(URI("http://tilgangsmaskin-url"), tilgangsmaskin) {
    /**
     * Legger til tilgang for testIdenter og setter defaulten for godkjenning til false
     *
     * VIKTIG at man resetter godkjennDefault tilbake til true i etterkant, hvis ikke feiler påfølgende tester som trenger at den er satt til true
     */
    fun leggTilTilganger(
        personTilganger: List<PersonTilgang>,
        godkjennDefault: Boolean = false,
    ) {
        tilgangsmaskin.tilganger.putAll(personTilganger.associateBy { it.personIdent })
        tilgangsmaskin.godkjennByDefault = godkjennDefault
    }

    fun reset() {
        tilgangsmaskin.tilganger.clear()
        tilgangsmaskin.godkjennByDefault = true
    }

    class StyrbarTilgangsmaskinKlient : TilgangsmaskinKlient(URI("http://tilgangsmaskin-url"), mockk(relaxed = true)) {
        val tilganger = mutableMapOf<String, PersonTilgang>()
        var godkjennByDefault = true

        override fun sjekkTilgangTilPersoner(
            personIdenter: Set<String>,
            regeltype: Regeltype,
        ): List<TilgangsmaskinResultat> =
            personIdenter.map { personIdent ->
                tilganger[personIdent]?.tilTilgangsmaskinResultat() ?: standardsvar(personIdent)
            }

        private fun standardsvar(personIdent: String) =
            if (godkjennByDefault) {
                TilgangsmaskinResultat(personIdent = personIdent, harTilgang = true, httpStatus = 204)
            } else {
                TilgangsmaskinResultat(personIdent = personIdent, harTilgang = false, httpStatus = 403, avvisningskode = Avvisningskode.UKJENT)
            }

        private fun PersonTilgang.tilTilgangsmaskinResultat() =
            TilgangsmaskinResultat(
                personIdent = personIdent,
                harTilgang = harTilgang,
                httpStatus = if (harTilgang) 204 else 403,
                avvisningskode = avvisning?.avvisningskode,
                begrunnelse = avvisning?.begrunnelse,
            )
    }
}
