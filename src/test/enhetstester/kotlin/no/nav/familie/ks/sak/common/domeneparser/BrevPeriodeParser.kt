package no.nav.familie.ks.sak.common.domeneparser

object BrevPeriodeParser {
    enum class DomenebegrepBrevBegrunnelse(override val nøkkel: String) : Domenenøkkel {
        BEGRUNNELSE("Begrunnelse"),
        GJELDER_SØKER("Gjelder søker"),
        BARNAS_FØDSELSDATOER("Barnas fødselsdatoer"),
        ANTALL_BARN("Antall barn"),
        ANTALL_TIMER_BARNEHAGEPLASS("Antall timer barnehageplass"),
        MÅNED_OG_ÅR_BEGRUNNELSEN_GJELDER_FOR("Måned og år begrunnelsen gjelder for"),
        MÅLFORM("Målform"),
        BELØP("Beløp"),
        SØKNADSTIDSPUNKT("Søknadstidspunkt"),
        AVTALETIDSPUNKT_DELT_BOSTED("Avtaletidspunkt delt bosted"),
        TYPE("Type"),
        GJELDER_ANDRE_FORELDER("Gjelder andre forelder"),
    }

    enum class DomenebegrepBrevPeriode(override val nøkkel: String) : Domenenøkkel {
        BARNAS_FØDSELSDAGER("Barnas fødselsdager"),
        ANTALL_BARN("Antall barn med utbetaling"),
        TYPE("Brevperiodetype"),
        BELØP("Beløp"),
        DU_ELLER_INSTITUSJONEN("Du eller institusjonen"),
    }
}
