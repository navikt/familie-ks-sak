package no.nav.familie.ks.sak.fake

import no.nav.familie.ks.sak.config.PdlConfig
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.pdl.PdlClient
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlIdent
import org.springframework.web.client.RestOperations
import java.lang.Integer.min
import java.net.URI

class FakePdlClient(
    restOperations: RestOperations,
) : PdlClient(PdlConfig(URI("dummy_uri")), restOperations) {
    private val identMap = mutableMapOf<String, List<PdlIdent>>()

    override fun hentIdenter(
        personIdent: String,
        historikk: Boolean,
    ): List<PdlIdent> {
        // If we have stored identities for this person, return them
        identMap[personIdent]?.let { return it }

        return when {
            historikk ->
                listOf(
                    PdlIdent(personIdent, historisk = false, gruppe = "FOLKEREGISTERIDENT"),
                    PdlIdent(randomFnr(), historisk = true, gruppe = "FOLKEREGISTERIDENT"),
                )

            else ->
                listOf(
                    PdlIdent(
                        ident = personIdent.substring(0, min(11, personIdent.length)),
                        historisk = false,
                        gruppe = "FOLKEREGISTERIDENT",
                    ),
                    PdlIdent(
                        ident = personIdent.substring(0, min(11, personIdent.length)) + "00",
                        historisk = false,
                        gruppe = "AKTORID",
                    ),
                )
        }
    }

    fun leggTilIdent(
        personIdent: String,
        identInformasjon: List<PdlIdent>,
    ) {
        identMap[personIdent] = identInformasjon
    }

    fun reset() {
        identMap.clear()
    }
}
