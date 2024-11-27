package no.nav.familie.ks.sak.kjerne.brev.hjemler

import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse

fun utledEØSForordningen883Hjemler(
    sanityBegrunnelser: List<SanityBegrunnelse>,
) = sanityBegrunnelser.flatMap { it.hjemlerEØSForordningen883 }.distinct()
