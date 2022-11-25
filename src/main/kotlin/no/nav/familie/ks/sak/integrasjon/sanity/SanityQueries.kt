package no.nav.familie.ks.sak.integrasjon.sanity

const val hentBegrunnelser =
    "*[_type == \"ksBegrunnelse\" && tema != \"EØS_PRIMÆRLAND\" && tema != \"EØS_SEKUNDÆRLAND\" && apiNavn != null && navnISystem != null]"

const val hentEØSBegrunnelser =
    "*[_type == \"ksBegrunnelse\" && tema == \"EØS_PRIMÆRLAND\" && tema == \"EØS_SEKUNDÆRLAND\" && apiNavn != null && navnISystem != null]"
