package no.nav.familie.ks.sak.kjerne.behandling.domene

enum class BehandlingSteg {
    REGISTRERE_PERSONGRUNNLAG,
    REGISTRERE_SØKNAD,
    VILKÅRSVURDERING,
    BEHANDLINGSRESULTAT,
    VURDER_TILBAKEKREVING,
    VEDTAK,
    BESLUTTE_VEDTAK,
    IVERKSETT_MOT_OPPDRAG,
    BEHANDLING_AVSLUTTET
}

enum class BehandlingStegStatus(val beskrivelse: String) {
    VENTER("Steget er satt på vent, f.eks. venter på brukertilbakemelding"),
    KLAR("Klar til saksbehandling"),
    UTFØRT("Steget er ferdig utført"),
    AUTOUTFØRT("Steget utføres automatisk av systemet"),
    TILBAKEFØRT("Steget er avbrutt og tilbakeført til et tidligere steg"),
    AVBRUTT("Steget er avbrutt");
}
