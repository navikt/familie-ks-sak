package no.nav.familie.ks.sak.integrasjon.ecb

class ECBServiceException(
    override val message: String,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)
