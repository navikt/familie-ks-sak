package no.nav.familie.ks.sak.kjerne.brev.hjemler

import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse

const val HJEMMEL_60_EØS_FORORDNINGEN_987 = "60"

fun utledEØSForordningen987Hjemler(
    sanityBegrunnelser: List<SanityBegrunnelse>,
    refusjonEøsHjemmelSkalMedIBrev: Boolean,
): List<String> =
    sanityBegrunnelser
        .flatMap { it.hjemlerEØSForordningen987 }
        .plus(
            if (refusjonEøsHjemmelSkalMedIBrev) {
                listOf(HJEMMEL_60_EØS_FORORDNINGEN_987)
            } else {
                emptyList()
            },
        ).distinct()
