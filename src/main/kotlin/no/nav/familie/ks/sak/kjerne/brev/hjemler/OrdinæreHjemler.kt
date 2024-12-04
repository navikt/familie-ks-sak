package no.nav.familie.ks.sak.kjerne.brev.hjemler

import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse

fun utledOrdinæreHjemler(
    sanityBegrunnelser: List<SanityBegrunnelse>,
    opplysningspliktHjemlerSkalMedIBrev: Boolean,
): List<String> {
    val ordinæreHjemler = mutableSetOf<String>()

    ordinæreHjemler.addAll(sanityBegrunnelser.flatMap { it.hjemler }.toMutableSet())

    if (opplysningspliktHjemlerSkalMedIBrev) {
        val hjemlerNårOpplysningspliktIkkeOppfylt = listOf("13", "16")
        ordinæreHjemler.addAll(hjemlerNårOpplysningspliktIkkeOppfylt)
    }

    return ordinæreHjemler
        .map { it.toInt() }
        .sorted()
        .map { it.toString() }
        .distinct()
}
