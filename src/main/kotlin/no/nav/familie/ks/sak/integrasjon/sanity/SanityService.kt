package no.nav.familie.ks.sak.integrasjon.sanity

import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SanityService(
    private val cachedSanityKlient: CachedSanityKlient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var sanityBegrunnelseCache: List<SanityBegrunnelse> = emptyList()

    fun hentSanityBegrunnelser(): List<SanityBegrunnelse> =
        try {
            cachedSanityKlient
                .hentSanityBegrunnelserCached()
                .also { sanityBegrunnelse -> sanityBegrunnelseCache = sanityBegrunnelse }
        } catch (e: Exception) {
            if (sanityBegrunnelseCache.isEmpty()) {
                throw e
            }
            logger.warn("Kunne ikke hente begrunnelser fra Sanity, bruker siste cachet begrunnelser", e)
            sanityBegrunnelseCache
        }
}
