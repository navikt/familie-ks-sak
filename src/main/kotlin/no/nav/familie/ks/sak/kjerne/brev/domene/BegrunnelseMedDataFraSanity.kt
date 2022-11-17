package no.nav.familie.ks.sak.kjerne.brev.domene

import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.Standardbegrunnelse

data class BegrunnelseMedDataFraSanity(
    val standardbegrunnelse: Standardbegrunnelse,
    val sanityBegrunnelse: SanityBegrunnelse
)
