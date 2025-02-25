package no.nav.familie.ks.sak.common.domeneparser

import io.cucumber.datatable.DataTable

interface Domenenøkkel {
    val nøkkel: String
}

enum class Domenebegrep(
    override val nøkkel: String,
) : Domenenøkkel {
    ID("Id"),
    FAGSAK_ID("FagsakId"),
    BEHANDLING_ID("BehandlingId"),
    FORRIGE_BEHANDLING_ID("ForrigeBehandlingId"),
    FRA_DATO("Fra dato"),
    TIL_DATO("Til dato"),
    BEHANDLINGSÅRSAK("Behandlingsårsak"),
    BEHANDLINGSRESULTAT("Behandlingsresultat"),
    BEHANDLINGSSTATUS("Behandlingsstatus"),
    SØKNADSTIDSPUNKT("Søknadstidspunkt"),
    BEHANDLINGSKATEGORI("Behandlingskategori"),
}

enum class DomenebegrepAndelTilkjentYtelse(
    override val nøkkel: String,
) : Domenenøkkel {
    ER_AUTOMATISK_VURDERT("Er automatisk vurdert"),
}

object DomeneparserUtil {
    fun DataTable.groupByBehandlingId(): Map<Long, List<Map<String, String>>> = this.asMaps().groupBy { rad -> parseLong(Domenebegrep.BEHANDLING_ID, rad) }
}
