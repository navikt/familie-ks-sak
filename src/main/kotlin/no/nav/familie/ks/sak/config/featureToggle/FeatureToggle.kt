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
    IKKE_LAG_OPPHOERSPERIODE_AV_AVSLAAT_BEHANDLINGSRESULTAT("familie-ba-sak.ikke-lag-opphoersperiode-ved-avslaatt-behandlingsresultat"),
    BRUK_NY_LOGIKK_FOR_AA_FINNE_ENHET_FOR_OPPRETTING_AV_KLAGEBEHANDLING("familie-ks-sak.bruk-ny-logikk-for-aa-finne-enhet-for-oppretting-av-klagebehandling"),
}
