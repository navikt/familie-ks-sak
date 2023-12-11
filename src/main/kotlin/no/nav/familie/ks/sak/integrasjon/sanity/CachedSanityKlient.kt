package no.nav.familie.ks.sak.integrasjon.sanity

import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class CachedSanityKlient(
    private val sanityKlient: SanityKlient,
    @Value("\${SANITY_DATASET}") private val sanityDatasett: String,
) {
    @Cacheable("sanityBegrunnelser", cacheManager = "shortCache")
    fun hentSanityBegrunnelserCached(): List<SanityBegrunnelse> = sanityKlient.hentBegrunnelser(datasett = sanityDatasett)
}
