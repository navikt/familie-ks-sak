package no.nav.familie.ks.sak.kjerne.brev.hjemler

import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse

fun utledSeprasjonsavtaleStorbritanniaHjemler(sanityBegrunnelser: List<SanityBegrunnelse>) =
    sanityBegrunnelser
        .flatMap { it.hjemlerSeperasjonsavtalenStorbritannina }
        .distinct()
