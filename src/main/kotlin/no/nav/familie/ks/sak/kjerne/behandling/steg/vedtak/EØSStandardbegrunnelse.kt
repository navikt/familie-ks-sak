package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

// TODO: Må legge inn faktiske begrunnelser vi skal ha her
enum class EØSStandardbegrunnelse : IVedtakBegrunnelse {
    DUMMY {
        override val sanityApiNavn = "dummyApiNavn"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_OPPHØR
    };

    override fun enumnavnTilString() = this.name
}
