package no.nav.familie.ks.sak.integrasjon.mottak

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.util.UriUtil
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.ks.sak.integrasjon.kallEksternTjeneste
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class MottakClient(
    @Value("\${FAMILIE_BAKS_MOTTAK_URL}") val mottakBaseUrl: URI,
    @Qualifier("azure") val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "baks-mottak") {
    fun hentStrengesteAdressebeskyttelsegraderingIDigitalSøknad(journalpostId: String): ADRESSEBESKYTTELSEGRADERING? {
        val uri = UriUtil.uri(mottakBaseUrl, "soknad/adressebeskyttelse/${Tema.KON.name}/$journalpostId")
        return kallEksternTjeneste<ADRESSEBESKYTTELSEGRADERING?>(
            tjeneste = "baks-mottak",
            uri = uri,
            formål = "Hente strengeste adressebeskyttelsegradering i digital søknad",
        ) {
            getForEntity(uri)
        }
    }
}
