package no.nav.familie.ks.sak.config.featureToggle

class FeatureToggleConfig {
    companion object {
        // Operasjonelle
        const val TEKNISK_VEDLIKEHOLD_HENLEGGELSE = "familie-ks-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"
        const val TEKNISK_ENDRING = "familie-ks-sak.behandling.teknisk-endring"
        const val KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV = "familie-ks-sak.behandling.korreksjon-vedtaksbrev"
        const val KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER = "familie-ks-sak.kan-opprette-og-endre-sammensatte-kontrollsaker"
        const val KAN_KJORE_LOVENDRING_FLERE_GANGER = "familie-ks-sak.kan-kjore-lovendring-flere-ganger"
        const val OVERGANGSORDNING = "familie-ks-sak.overgangsordning"

        // Ikke operasjonelle
        const val OPPRETT_SAK_PÅ_RIKTIG_ENHET_OG_SAKSBEHANDLER = "familie-ba-ks-sak.opprett-sak-paa-riktig-enhet-og-saksbehandler"

        const val BRUK_NY_LØYPE_FOR_GENERERING_AV_ANDELER = "familie-ks-sak.bruk-ny-loype-for-generering-av-andeler"
    }
}
