package no.nav.familie.ks.sak.config.featureToggle

enum class FeatureToggle(
    val navn: String,
) {
    // Operasjonelle
    TEKNISK_VEDLIKEHOLD_HENLEGGELSE("familie-ks-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"),
    TEKNISK_ENDRING("familie-ks-sak.behandling.teknisk-endring"),
    KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV("familie-ks-sak.behandling.korreksjon-vedtaksbrev"),
    KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER("familie-ks-sak.kan-opprette-og-endre-sammensatte-kontrollsaker"),

    // Ikke operasjonelle
    SKAL_HÅNDTERE_FALSK_IDENTITET("familie-ks-sak.skal-handtere-falsk-identitet"),
    HENT_ARBEIDSFORDELING_MED_BEHANDLINGSTYPE("familie-ks-sak.hent-arbeidsfordeling-med-behandlingstype"),
    BRUK_NY_LOGIKK_FOR_SØKERS_MELDEPLIKT("familie-ks-sak.bruk-ny-meldepliktlogikk"),
}
