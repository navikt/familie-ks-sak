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
    KAN_OPPRETTE_REVURDERING_MED_ÅRSAK_IVERKSETTE_KA_VEDTAK("familie-ks-sak.kan-opprette-revurdering-med-aarsak-iverksette-ka-vedtak"),
    JOURNALFOER_MANUELT_BREV_I_TASK("familie-ks-sak.journalfoer-manuelt-brev-i-task"),
    PORTEFØLJEJUSTERING("familie-ks-sak.portefoljejustering"),
}
