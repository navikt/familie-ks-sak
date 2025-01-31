package no.nav.familie.ks.sak.config.featureToggle

class FeatureToggleConfig {
    companion object {
        // Operasjonelle
        const val TEKNISK_VEDLIKEHOLD_HENLEGGELSE = "familie-ks-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"
        const val TEKNISK_ENDRING = "familie-ks-sak.behandling.teknisk-endring"
        const val KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV = "familie-ks-sak.behandling.korreksjon-vedtaksbrev"
        const val KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER = "familie-ks-sak.kan-opprette-og-endre-sammensatte-kontrollsaker"
        const val LAGRE_BARNEHAGEBARN_I_KS = "familie-ks-sak.lagre-barnehagebarn-i-ks"

        // Ikke operasjonelle
        const val OPPRETT_SAK_PÅ_RIKTIG_ENHET_OG_SAKSBEHANDLER = "familie-ba-ks-sak.opprett-sak-paa-riktig-enhet-og-saksbehandler"
        const val KAN_OPPRETTE_REVURDERING_MED_ÅRSAK_IVERKSETTE_KA_VEDTAK = "familie-ks-sak.kan-opprette-revurdering-med-aarsak-iverksette-ka-vedtak"
        const val STØTTER_LOVENDRING_2025 = "familie-ks-sak.stotter-lovendring-2025"

        const val BRUK_OMSKRIVING_AV_HJEMLER_I_BREV = "familie-ks-sak.bruk_omskriving_av_hjemler_i_brev"
        const val ALLEREDE_UTBETALT_SOM_ENDRINGSÅRSAK = "familie-ks-sak.allerede-utbetalt"
    }
}
