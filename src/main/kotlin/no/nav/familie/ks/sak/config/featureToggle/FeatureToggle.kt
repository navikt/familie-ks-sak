package no.nav.familie.ks.sak.config.featureToggle

enum class FeatureToggle(
    val navn: String,
) {
    // Operasjonelle
    TEKNISK_VEDLIKEHOLD_HENLEGGELSE("familie-ks-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"),
    TEKNISK_ENDRING("familie-ks-sak.behandling.teknisk-endring"),
    KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV("familie-ks-sak.behandling.korreksjon-vedtaksbrev"),
    KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER("familie-ks-sak.kan-opprette-og-endre-sammensatte-kontrollsaker"),
    LAGRE_BARNEHAGEBARN_I_KS("familie-ks-sak.lagre-barnehagebarn-i-ks"),

    // Ikke operasjonelle
    KAN_OPPRETTE_REVURDERING_MED_ÅRSAK_IVERKSETTE_KA_VEDTAK("familie-ks-sak.kan-opprette-revurdering-med-aarsak-iverksette-ka-vedtak"),
    KAN_BEHANDLE_KLAGE("familie-ks-sak.klage"),

    // NAV-24034
    BRUK_NY_SAKSBEHANDLER_NAVN_FORMAT_I_SIGNATUR("familie-ks-sak.bruk-ny-saksbehandler-navn-format-i-signatur"),
}
