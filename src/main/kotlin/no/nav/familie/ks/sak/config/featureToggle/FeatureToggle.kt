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
    STØTTER_LOVENDRING_2025("familie-ks-sak.stotter-lovendring-2025"),
    STØTTER_ADOPSJON("familie-ks-sak.stotter-adopsjon"),
    SKAL_GENERERE_ANDELER_FOR_PRAKSISENDRING_2024("familie-ks-sak.skal-generere-andel-for-praksisendring-2024"),

    BRUK_OMSKRIVING_AV_HJEMLER_I_BREV("familie-ks-sak.bruk_omskriving_av_hjemler_i_brev"),
    ALLEREDE_UTBETALT_SOM_ENDRINGSÅRSAK("familie-ks-sak.allerede-utbetalt"),
}
