package no.nav.familie.ks.sak.kjerne.brev.domene

import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.Begrunnelse

data class BegrunnelseMedDataFraSanity(
    val begrunnelse: Begrunnelse,
    val sanityBegrunnelse: SanityBegrunnelse
)
