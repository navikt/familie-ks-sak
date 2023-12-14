package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

// TODO: Må legge inn faktiske begrunnelser vi skal ha her
enum class EØSBegrunnelse : IBegrunnelse {
    INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBarnetBorINorge"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },
    OPPHØR_EØS_STANDARD {
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
        override val sanityApiNavn = "opphorEosStandard"
    }, ;

    override fun enumnavnTilString() = this.name
}
