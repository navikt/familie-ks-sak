package no.nav.familie.ks.sak.common.domeneparser

import io.cucumber.datatable.DataTable

interface Domenenøkkel {
    val nøkkel: String
}

enum class Domenebegrep(override val nøkkel: String) : Domenenøkkel {
    ID("Id"),
    FAGSAK_ID("FagsakId"),
    FAGSAK_TYPE("Fagsaktype"),
    BEHANDLING_ID("BehandlingId"),
    FORRIGE_BEHANDLING_ID("ForrigeBehandlingId"),
    FRA_DATO("Fra dato"),
    TIL_DATO("Til dato"),
    ENDRET_MIGRERINGSDATO("Endret migreringsdato"),
    BEHANDLINGSÅRSAK("Behandlingsårsak"),
    BEHANDLINGSRESULTAT("Behandlingsresultat"),
    BEHANDLINGSSTATUS("Behandlingsstatus"),
    SKAL_BEHANLDES_AUTOMATISK("Skal behandles automatisk"),
    SØKNADSTIDSPUNKT("Søknadstidspunkt"),
    BEHANDLINGSKATEGORI("Behandlingskategori"),
}

object DomeneparserUtil {
    fun DataTable.groupByBehandlingId(): Map<Long, List<Map<String, String>>> =
        this.asMaps().groupBy { rad -> parseLong(Domenebegrep.BEHANDLING_ID, rad) }
}
