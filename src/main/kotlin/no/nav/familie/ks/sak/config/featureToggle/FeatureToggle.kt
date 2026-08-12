package no.nav.familie.ks.sak.config.featureToggle

enum class FeatureToggle(
    val navn: String,
) {
    // Operasjonelle
    TEKNISK_VEDLIKEHOLD_HENLEGGELSE("familie-ks-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"),
    TEKNISK_ENDRING("familie-ks-sak.behandling.teknisk-endring"),
    KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER("familie-ks-sak.kan-opprette-og-endre-sammensatte-kontrollsaker"),
    SKAL_HÅNDTERE_FALSK_IDENTITET("familie-ks-sak.skal-handtere-falsk-identitet"),

    // NAV-29382
    HENT_VEDTAKSBREV_FRA_JOARK("familie-ks-sak.hent-vedtaksbrev-fra-joark"),

    // NAV-29936
    SKAL_SLETTE_GAMLE_VEDTAKSBREV_FRA_DB("familie-ks-sak.skal-slette-gamle-vedtaksbrev-fra-db"),

    // NAV-30011
    OPPDRAG_MIGRERING_HENT_SIMULERING_GCP("familie-baks-sak.oppdrag-migrering-hent-simulering-gcp"),
    OPPDRAG_MIGRERING_IVERKSETT_OPPDRAG_GCP("familie-ba-og-ks-sak.oppdrag-migrering-iverksett-oppdrag-gcp"),
}
