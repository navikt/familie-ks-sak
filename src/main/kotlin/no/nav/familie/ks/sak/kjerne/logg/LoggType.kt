package no.nav.familie.ks.sak.kjerne.logg

enum class LoggType(val visningsnavn: String, val tittel: String = visningsnavn) {
    AUTOVEDTAK_TIL_MANUELL_BEHANDLING("Autovedtak til manuell behandling", "Automatisk behandling stoppet"),
    LIVSHENDELSE("Livshendelse"),
    BEHANDLENDE_ENHET_ENDRET("Behandlende enhet endret", "Endret enhet på behandling"),
    BEHANDLING_OPPRETTET("Behandling opprettet"),
    BEHANDLINGSTYPE_ENDRET("Endret behandlingstype", "Endret behandlingstema"),
    BARN_LAGT_TIL("Barn lagt til på behandling"),
    DOKUMENT_MOTTATT("Dokument ble mottatt"),
    SØKNAD_REGISTRERT("Søknaden ble registrert"),
    VILKÅRSVURDERING("Vilkårsvurdering"),
    SEND_TIL_BESLUTTER("Send til beslutter", "Sendt til beslutter"),
    SEND_TIL_SYSTEM("Send til system", "Sendt til system"),
    GODKJENNE_VEDTAK("Godkjenne vedtak", "Vedtak godkjent"),
    MIGRERING_BEKREFTET("Migrering bekreftet", "Migrering bekreftet"),
    DISTRIBUERE_BREV("Distribuere brev", "Brev sendt"),
    BREV_IKKE_DISTRIBUERT("Brev ikke distribuert", "Brevet ble ikke distribuert fordi mottaker har ukjent adresse"),
    BREV_IKKE_DISTRIBUERT_UKJENT_DØDSBO(
        "Brev ikke distribuert. Ukjent dødsbo",
        "Mottaker har ukjent dødsboadresse, og brevet blir ikke sendt før adressen er satt"
    ),
    FERDIGSTILLE_BEHANDLING("Ferdigstille behandling", "Ferdigstilt behandling"),
    HENLEGG_BEHANDLING("Henlegg behandling", "Behandlingen er henlagt"),
    BEHANDLIG_SATT_PÅ_VENT("Behandlingen er satt på vent"),
    BEHANDLIG_GJENOPPTATT("Behandling gjenopptatt"),
    VENTENDE_BEHANDLING_ENDRET("Behandlingen er oppdatert")
}
