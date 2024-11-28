package no.nav.familie.ks.sak.kjerne.brev.hjemler

const val FORVALTINIGSLOVEN_PARAGRAF_35 = "35"

fun utledForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev: Boolean): List<String> =
    if (vedtakKorrigertHjemmelSkalMedIBrev) {
        listOf(FORVALTINIGSLOVEN_PARAGRAF_35)
    } else {
        emptyList()
    }
