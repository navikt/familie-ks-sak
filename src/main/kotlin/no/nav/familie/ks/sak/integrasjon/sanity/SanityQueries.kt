package no.nav.familie.ks.sak.integrasjon.sanity

const val HENT_BEGRUNNELSER =
    "*[_type == \"ksBegrunnelse\" && tema != \"EØS_PRIMÆRLAND\" && tema != \"EØS_SEKUNDÆRLAND\" && apiNavn != null && navnISystem != null]"

const val HENT_EØS_BEGRUNNELSER =
    "*[_type == \"ksBegrunnelse\" && tema == \"EØS_PRIMÆRLAND\" && tema == \"EØS_SEKUNDÆRLAND\" && apiNavn != null && navnISystem != null]"
