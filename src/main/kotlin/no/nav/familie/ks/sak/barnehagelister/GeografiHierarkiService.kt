package no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import org.springframework.stereotype.Service

@Service
class GeografiHierarkiService(
    private val integrasjonKlient: IntegrasjonKlient,
) {
    fun hentBydelEllerKommuneKodeTilNavnFraFylkeNr(fylkeNr: String): Map<String, String> =
        integrasjonKlient
            .hentFylkerOgKommuner()
            .fylker
            .first { it.kode == fylkeNr }
            .kommuner
            .flatMap { kommune ->
                buildList {
                    add(kommune.kode to kommune.navn)
                    kommune.bydeler.forEach { add(it.kode to it.navn) }
                }
            }.toMap()
}
