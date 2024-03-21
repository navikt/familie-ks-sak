package no.nav.familie.ks.sak.config

class FeatureToggleConfig {
    companion object {
        // Operasjonelle
        const val TEKNISK_VEDLIKEHOLD_HENLEGGELSE = "familie-ks-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"
        const val TEKNISK_ENDRING = "familie-ks-sak.behandling.teknisk-endring"
        const val KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV = "familie-ks-sak.behandling.korreksjon-vedtaksbrev"
        const val SKAL_BRUKE_NY_BEHANDLINGSRESULTAT_LOGIKK = "familie-ks-sak.bruk-ny-behandlingsresultat"
    }
}
