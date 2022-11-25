package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

// TODO: Må legge inn faktiske begrunnelser vi skal ha her
enum class EØSBegrunnelse : IBegrunnelse {
    DUMMY {
        override val sanityApiNavn = "dummyApiNavn"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    };

    override fun enumnavnTilString() = this.name
}
