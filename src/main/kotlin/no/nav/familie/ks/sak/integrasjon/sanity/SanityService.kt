package no.nav.familie.ks.sak.integrasjon.sanity

import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient.Companion.RETRY_BACKOFF_5000MS
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service

@Service
class SanityService(
    private val cachedSanityKlient: CachedSanityKlient,
) {
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    fun hentSanityBegrunnelser(): List<SanityBegrunnelse> = cachedSanityKlient.hentSanityBegrunnelserCached()
}
