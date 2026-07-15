package no.nav.familie.ks.sak.config.featureToggle

enum class FeatureToggle(
    val navn: String,
) {
    // Operasjonelle
    TEKNISK_VEDLIKEHOLD_HENLEGGELSE("familie-ks-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"),
    TEKNISK_ENDRING("familie-ks-sak.behandling.teknisk-endring"),
    KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER("familie-ks-sak.kan-opprette-og-endre-sammensatte-kontrollsaker"),

    // Ikke operasjonelle
    SKAL_HÅNDTERE_FALSK_IDENTITET("familie-ks-sak.skal-handtere-falsk-identitet"),
    HENT_ARBEIDSFORDELING_MED_BEHANDLINGSTYPE("familie-ks-sak.hent-arbeidsfordeling-med-behandlingstype"),

    // NAV-29382
    HENT_VEDTAKSBREV_FRA_JOARK("familie-ks-sak.hent-vedtaksbrev-fra-joark"),

    // NAV-29936
    SKAL_SLETTE_GAMLE_VEDTAKSBREV_FRA_DB("familie-ks-sak.skal-slette-gamle-vedtaksbrev-fra-db"),
}
