package no.nav.familie.ks.sak.sikkerhet

enum class Rolle {
    VEILEDER,
    SAKSBEHANDLER,
    BESLUTTER,
    FORVALTER,
    TEAMFAMILIE_APPLIKASJON,
    KLAGE_APPLIKASJON,
    BISYS_APPLIKASJON,
    EF_SAK_APPLIKASJON,
    ;

    fun authority(): String = "ROLE_$name"

    companion object {
        fun rollerMedInternTilgang() = setOf(VEILEDER.name, SAKSBEHANDLER.name, BESLUTTER.name, FORVALTER.name, TEAMFAMILIE_APPLIKASJON.name).toTypedArray()
    }
}
