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

    override fun hentEnhet(barnehageOrgNummer: String): NbrEnhet? {
        oppslag.add(barnehageOrgNummer)
        return enheter[barnehageOrgNummer]
    }

    fun reset() {
        enheter.clear()
        oppslag.clear()
    }
}
