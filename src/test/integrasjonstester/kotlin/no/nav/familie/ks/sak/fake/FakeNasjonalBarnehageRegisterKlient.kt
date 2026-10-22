package no.nav.familie.ks.sak.fake

import io.mockk.mockk
import no.nav.familie.ks.sak.integrasjon.nbr.NasjonalBarnehageRegisterKlient
import no.nav.familie.ks.sak.integrasjon.nbr.domene.NbrEnhet

class FakeNasjonalBarnehageRegisterKlient : NasjonalBarnehageRegisterKlient("http://nbr", mockk(relaxed = true)) {
    private val enheter = mutableMapOf<String, NbrEnhet>()
    val oppslag = mutableListOf<String>()

    fun leggTilEnhet(enhet: NbrEnhet) {
        enheter[enhet.organisasjonsnummer] = enhet
    }

    override fun hentEnhet(barnehageOrganisasjonsnummer: String): NbrEnhet? {
        oppslag.add(barnehageOrganisasjonsnummer)
        return enheter[barnehageOrganisasjonsnummer]
    }

    fun reset() {
        enheter.clear()
        oppslag.clear()
    }
}
