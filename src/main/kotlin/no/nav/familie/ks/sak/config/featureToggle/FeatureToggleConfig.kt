package no.nav.familie.ks.sak.config.featureToggle

class FeatureToggleConfig {
    companion object {
        // Operasjonelle
        const val TEKNISK_VEDLIKEHOLD_HENLEGGELSE = "familie-ks-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"
        const val TEKNISK_ENDRING = "familie-ks-sak.behandling.teknisk-endring"
        const val KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV = "familie-ks-sak.behandling.korreksjon-vedtaksbrev"

        // Release
        const val LOV_ENDRING_7_MND_NYE_BEHANDLINGER = "familie-ks-sak.lov-endring-7-mnd-nye-behandlinger"
    }
}
