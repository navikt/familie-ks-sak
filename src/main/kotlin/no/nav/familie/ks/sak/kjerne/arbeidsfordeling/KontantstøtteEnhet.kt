package no.nav.familie.ks.sak.kjerne.arbeidsfordeling

enum class KontantstøtteEnhet(
    val enhetsnummer: String,
    val enhetsnavn: String,
) {
    VIKAFOSSEN("2103", "NAV Vikafossen"),
    DRAMMEN("4806", "NAV Familie- og pensjonsytelser Drammen"),
    VADSØ("4820", "NAV Familie- og pensjonsytelser Vadsø"),
    OSLO("4833", "NAV Familie- og pensjonsytelser Oslo 1"),
    STORD("4842", "NAV Familie- og pensjonsytelser Stord"),
    STEINKJER("4817", "NAV Familie- og pensjonsytelser Steinkjer"),
    BERGEN("4812", "NAV Familie- og pensjonsytelser Bergen"),
    MIDLERTIDIG_ENHET("4863", "Midlertidig enhet"),
    ;

    override fun toString(): String = "$enhetsnavn ($enhetsnummer)"

    companion object {
        private val GYLDIGE_BEHANDLENDE_BARNETRYGD_ENHETER =
            listOf(
                VIKAFOSSEN,
                DRAMMEN,
                VADSØ,
                OSLO,
                STORD,
                STEINKJER,
                BERGEN,
            )

        fun erGyldigBehandlendeKontantstøtteEnhet(enhetsnummer: String): Boolean = GYLDIGE_BEHANDLENDE_BARNETRYGD_ENHETER.any { it.enhetsnummer == enhetsnummer }
    }
}
