package no.nav.familie.ks.sak.integrasjon.sanity

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.ks.sak.integrasjon.kallEksternTjeneste
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelserResponsDto
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityEØSBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityEØSBegrunnelserResponsDto
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.net.URLEncoder

@Service
class SanityKlient(
    @Value("\${SANITY_BASE_URL}") private val sanityBaseUrl: String,
    restOperations: RestOperations
) :
    AbstractRestClient(restOperations, "sanity") {
    fun hentBegrunnelser(datasett: String = "ba-brev"): List<SanityBegrunnelse> {
        val uri = lagHentUri(datasett, hentBegrunnelser)

        // TODO: Fjern try/catch og default dummy-respons ved feil når vi får satt opp sanity dokument og struktur på respons.
        return try {
            val restSanityBegrunnelser =
                kallEksternTjeneste<SanityBegrunnelserResponsDto>(
                    tjeneste = "Sanity",
                    uri = uri,
                    formål = "Henter begrunnelser fra sanity"
                ) {
                    getForEntity(uri)
                }

            restSanityBegrunnelser.result.map { it.tilSanityBegrunnelse() }
        } catch (e: Exception) {
            listOf(SanityBegrunnelse("dummyApiNavn", "dummyNavnISystem", Vilkår.values().toList(), hjemler = emptyList()))
        }
    }

    fun hentEØSBegrunnelser(datasett: String = "ba-brev"): List<SanityEØSBegrunnelse> {
        val uri = lagHentUri(datasett, hentEØSBegrunnelser)

        // TODO: Fjern try/catch og default dummy-respons ved feil når vi får satt opp sanity dokument og struktur på respons.
        return try {
            val restSanityEØSBegrunnelser = kallEksternTjeneste<SanityEØSBegrunnelserResponsDto>(
                tjeneste = "Sanity",
                uri = uri,
                formål = "Henter EØS-begrunnelser fra sanity"
            ) {
                getForEntity(uri)
            }

            restSanityEØSBegrunnelser.result.map { it.tilSanityEØSBegrunnelse() }
        } catch (e: Exception) {
            listOf(SanityEØSBegrunnelse("dummyApiNavn", "dummyNavnISystem"))
        }
    }

    private fun lagHentUri(datasett: String, query: String): URI {
        val hentQuery = URLEncoder.encode(query, "utf-8")
        return URI.create("$sanityBaseUrl/$datasett?query=$hentQuery")
    }
}
