package no.nav.familie.ks.sak.integrasjon.nbr.domene

enum class OsloBydel(
    val organisasjonsnummer: String,
    val navn: String,
) {
    GAMLE_OSLO("974778742", "Gamle Oslo"),
    GRUNERLOKKA("870534612", "Grünerløkka"),
    SAGENE("974778726", "Sagene"),
    ST_HANSHAUGEN("971179686", "St. Hanshaugen"),
    FROGNER("874778702", "Frogner"),
    ULLERN("971022051", "Ullern"),
    VESTRE_AKER("970145311", "Vestre Aker"),
    NORDRE_AKER("974778882", "Nordre Aker"),
    BJERKE("974778874", "Bjerke"),
    GRORUD("974778866", "Grorud"),
    STOVNER("874778842", "Stovner"),
    ALNA("970534644", "Alna"),
    OSTENSJO("974778807", "Østensjø"),
    NORDSTRAND("970534679", "Nordstrand"),
    SONDRE_NORDSTRAND("972408875", "Søndre Nordstrand"),
    ;

    companion object {
        fun fraOrganisasjonsnummer(organisasjonsnummer: String): OsloBydel? = entries.find { it.organisasjonsnummer == organisasjonsnummer }
    }
}
