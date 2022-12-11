package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import no.nav.familie.ks.sak.common.util.konverterEnumsTilString
import no.nav.familie.ks.sak.common.util.konverterStringTilEnums
import javax.persistence.AttributeConverter
import javax.persistence.Converter

interface IBegrunnelse {

    val sanityApiNavn: String
    val begrunnelseType: BegrunnelseType

    fun enumnavnTilString(): String
}

enum class Begrunnelse : IBegrunnelse {
    INNVILGET_IKKE_BARNEHAGE {
        override val sanityApiNavn = "innvilgetIkkeBarnehage"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },
    INNVILGET_IKKE_BARNEHAGE_ADOPSJON {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetIkkeBarnehageAdopsjon"
    },
    INNVILGET_DELTID_BARNEHAGE {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetDeltidBarnehage"
    },
    INNVILGET_DELTID_BARNEHAGE_ADOPSJON {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetDeltidBarnehageAdopsjon"
    },
    INNVILGET_BARN_UNDER_2_ÅR {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBarnUnder2År"
    },
    INNVILGET_BARN_SLUTTET_I_BARNEHAGE {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBarnSluttetIBarnehage"
    },
    INNVILGET_SØKER_OG_ELLER_BARN_BOSATT_I_RIKET {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSøkerOgEllerBarnBosattIRiket"
    },
    INNVILGET_SØKER_OG_ELLER_BARN_HAR_OPPHOLDSTILLATELSE {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSøkerOgEllerBarnHarOppholdstillatelse"
    },
    INNVILGET_SØKER_OG_ELLER_BARN_BOSATT_I_RIKET_OG_HAR_OPPHOLDSTILLATELSE {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSøkerOgEllerBarnBosattIRiketOgHarOppholdstillatelse"
    },
    AVSLAG_UREGISTRERT_BARN {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagUregistrertBarn"
    },
    AVSLAG_BOSATT_I_RIKET {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagBosattIRiket"
    };

    override fun enumnavnTilString() = this.name
}

@Converter
class StandardbegrunnelseListConverter :
    AttributeConverter<List<Begrunnelse>, String> {

    override fun convertToDatabaseColumn(begrunnelser: List<Begrunnelse>) =
        konverterEnumsTilString(begrunnelser)

    override fun convertToEntityAttribute(string: String?): List<Begrunnelse> =
        konverterStringTilEnums(string)
}

val endretUtbetalingsperiodeBegrunnelser: List<Begrunnelse> = listOf(

    // TODO: Legg til begrunnelser
)
