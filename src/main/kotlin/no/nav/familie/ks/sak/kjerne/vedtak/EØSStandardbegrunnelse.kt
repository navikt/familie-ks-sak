package no.nav.familie.ks.sak.kjerne.vedtak

// TODO: Må legge inn faktiske begrunnelser vi skal ha her
enum class EØSStandardbegrunnelse : IVedtakBegrunnelse {
    DUMMY {
        override val sanityApiNavn = "dummyApiNavn"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
    };

    override val kanDelesOpp = false
    override fun enumnavnTilString() = this.name
}
