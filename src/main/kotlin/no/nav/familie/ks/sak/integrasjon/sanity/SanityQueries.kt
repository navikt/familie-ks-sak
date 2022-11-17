package no.nav.familie.ks.sak.integrasjon.sanity

// TODO: Tilpass query for KS
const val hentBegrunnelser =
    "*[_type == \"ksBegrunnelse\" && behandlingstema != \"EØS\" && apiNavn != null && navnISystem != null]"

// TODO: Tilpass query for KS
const val hentEØSBegrunnelser =
    "*[_type == \"ksBegrunnelse\" && behandlingstema == \"EØS\" && apiNavn != null && navnISystem != null]"
