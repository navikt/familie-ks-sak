package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import no.nav.familie.ks.sak.common.util.konverterEnumsTilString
import no.nav.familie.ks.sak.common.util.konverterStringTilEnums
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
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
    INNVILGET_MÅNEDEN_ETTER_ETT_ÅR {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetMaanedenEtterEttAar"
    },
    INNVILGET_MÅNEDEN_ETTER_SLUTTET_I_BARNEHAGE {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetMaanedenEtterSluttetIBarnehage"
    },
    INNVILGET_BOR_FAST_HOS_SØKER {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBorFastHosSoker"
    },
    INNVILGET_BOSATT_I_NORGE {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBosattINorge"
    },
    INNVILGET_FORELDRENE_BOR_SAMMEN_ENDRET_MOTTAKER {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetForeldreneBorSammenEndretMottaker"
    },
    INNVILGET_DELT_BOSTED_MÅNED_ETTER_SØKNAD {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetDeltBostedMaanedEtterSoknad"
    },
    INNVILGET_DELT_BOSTED_MÅNED_ETTER_ENIGHET {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetDeltBostedMaanedEtterEnighet"
    },
    INNVILGET_MEDLEMSKAP_I_FOLKETRYGDEN {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetMedlemskapIFolketrygden"
    },
    INNVILGET_MEDLEMSKAP_FOLKETRYGDEN_OG_EØS_LAND {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetMedlemskapFolketrygdenOgEOSLand"
    },
    INNVILGET_FRA_MÅNEDEN_ETTER_FORELDREPENGER {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFraMaanedenEtterForeldrepenger"
    },
    INNVILGET_OPPHOLD_I_EØS_LAND {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetOppholdIEosLand"
    },
    INNVILGET_TREDJELANDSBORGER_LOVLIG_OPPHOLD_SAMTIDIG_BOSATT_I_NORGE {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTredjelandsborgerLovligOppholdSamtidigBosattINorge"
    },
    INNVILGET_TREDJELANDSBORGER_LOVLIG_OPPHOLD_FØR_BOSATT_I_NORGE {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTredjelandsborgerLovligOppholdForBosattINorge"
    },
    INNVILGET_TREDJELANDSBORGER_BOSATT_FØR_LOVLIG_OPPHOLD_I_NORGE {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTredjelandsborgerBosattForLovligOppholdINorge"
    },
    INNVILGET_VURDERING_BOR_FAST_HOS_SØKER {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVurderingBorFastHosSoker"
    },
    INNVILGET_RETTSAVGJØRELSE_BOR_FAST_HOS_SØKER {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetRettsavgjorelseBorFastHosSoker"
    },
    INNVILGET_SATSENDRING {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSatsendring"
    },
    INNVILGET_ERKLÆRING_OM_MOTREGNING {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetErklaringOmMotregning"
    },
    AVSLAG_UREGISTRERT_BARN {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagUregistrertBarn"
    },
    AVSLAG_BOSATT_I_RIKET {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagBosattIRiket"
    },
    ETTER_ENDRET_UTBETALING_ETTERBETALING {
        override val begrunnelseType = BegrunnelseType.ETTER_ENDRET_UTBETALING
        override val sanityApiNavn = "etterEndretUtbetalingEtterbetalingTreMaanedTilbakeITid"
    };

    override fun enumnavnTilString() = this.name
}

fun Begrunnelse.støtterFritekst(sanityBegrunnelser: List<SanityBegrunnelse>) =
    sanityBegrunnelser.first { it.apiNavn == this.sanityApiNavn }.støtterFritekst

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
