package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

enum class EØSBegrunnelse : IBegrunnelse {
    INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBarnetBorINorge"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_STANDARD {
        override val sanityApiNavn = "innvilgetPrimarlandStandard"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_ALENEANSVAR {
        override val sanityApiNavn = "innvilgetPrimarlandAleneansvar"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBeggeForeldreBosattINorge"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_JOBBER_I_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBeggeForeldreJobberINorge"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "innvilgetPrimarlandUKStandard"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_UK_ALENEANSVAR {
        override val sanityApiNavn = "innvilgetPrimarlandUKAleneansvar"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "innvilgetPrimarlandUKOgUtlandStandard"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_BARNET_FLYTTET_TIL_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBarnetFlyttetTilNorge"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_SÆRKULLSBARN_ANDRE_BARN {
        override val sanityApiNavn = "innvilgetPrimarlandSarkullsbarnAndreBarn"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_SÆRKULLSBARN_ANDRE_BARN_OVERTATT_ANSVAR {
        override val sanityApiNavn = "innvilgetPrimarlandSarkullsbarnAndreBarnOvertattAnsvar"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_TO_ARBEIDSLAND_ANNET_LAND_UTBETALER {
        override val sanityApiNavn = "innvilgetPrimarlandToArbeidslandAnnetLandUtbetaler"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_UK_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetPrimarlandUKToArbeidslandNorgeUtbetaler"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_UK_TO_ARBEIDSLAND_ANNET_LAND_UTBETALER {
        override val sanityApiNavn = "innvilgetPrimarlandUKToArbeidslandAnnetLandUtbetaler"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    OPPHØR_EØS_STANDARD {
        override val sanityApiNavn = "opphorEosStandard"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_EØS_SØKER_BER_OM_OPPHØR {
        override val sanityApiNavn = "opphorEosSokerBerOmOpphor"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_BARN_BOR_IKKE_I_EØS_LAND {
        override val sanityApiNavn = "opphorBarnBorIkkeIEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_IKKE_STATSBORGER_I_EØS_LAND {
        override val sanityApiNavn = "opphorIkkeStatsborgerIEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SEPARASJONSAVTALEN_GJELDER_IKKE {
        override val sanityApiNavn = "opphorSeparasjonsavtalenGjelderIkke"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SENTRUM_FOR_LIVSINTERESSE {
        override val sanityApiNavn = "opphorSentrumForLivsinteresse"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_IKKE_ANSVAR_FOR_BARN {
        override val sanityApiNavn = "opphorIkkeAnsvarForBarn"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_IKKE_OPPHOLDSRETT_SOM_FAMILIEMEDLEM {
        override val sanityApiNavn = "opphorIkkeOppholdsrettSomFamiliemedlem"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SØKER_OG_BARN_BOR_IKKE_I_EØS_LAND {
        override val sanityApiNavn = "opphorSokerOgBarnBorIkkeIEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SØKER_BOR_IKKE_I_EØS_LAND {
        override val sanityApiNavn = "opphorSokerBorIkkeIEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_ARBEIDER_MER_ENN_25_PROSENT_I_ANNET_EØS_LAND {
        override val sanityApiNavn = "opphorArbeiderMerEnn25ProsentIAnnetEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EØS_LAND {
        override val sanityApiNavn = "opphorUtsendtArbeidstakerFraAnnetEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_ETT_BARN_DOD_EØS {
        override val sanityApiNavn = "opphorEttBarnDodEos"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_FLERE_BARN_ER_DODE_EØS {
        override val sanityApiNavn = "opphorFlereBarnErDodeEos"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SELVSTENDIG_RETT_OPPHØR {
        override val sanityApiNavn = "opphorSelvstendigRettOpphor"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SELVSTENDIG_RETT_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EØS_LAND {
        override val sanityApiNavn = "opphorSelvstendigRettUtsendtArbeidstakerFraAnnetEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SELVSTENDIG_RETT_OPPHØR_FRA_START {
        override val sanityApiNavn = "opphorSelvstendigRettOpphorFraStart"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SELVSTENDIG_RETT_VAR_IKKE_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EØS_LAND {
        override val sanityApiNavn = "opphorSelvstendigRettVarIkkeUtsendtArbeidstakerFraAnnetEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_BARN_BODDE_IKKE_I_ET_EØS_LAND {
        override val sanityApiNavn = "opphorBarnBoddeIkkeIEtEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SEPARASJONSAVTALEN_GJALDT_IKKE {
        override val sanityApiNavn = "opphorSeparasjonsavtalenGjaldtIkke"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_NORGE_VAR_IKKE_SENTRUM_FOR_LIVSINTERESSE {
        override val sanityApiNavn = "opphorNorgeVarIkkeSentrumForLivsinteresse"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_HADDE_IKKE_ANSVAR_FOR_BARN {
        override val sanityApiNavn = "opphorHaddeIkkeAnsvarForBarn"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_HADDE_IKKE_OPPHOLDSRETT_SOM_FAMILIEMEDLEM {
        override val sanityApiNavn = "opphorHaddeIkkeOppholdsrettSomFamiliemedlem"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SØKER_OG_BARN_BODDE_IKKE_I_EØS_LAND {
        override val sanityApiNavn = "opphorSokerOgBarnBoddeIkkeIEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SØKER_BODDE_IKKE_I_ET_EØS_LAND {
        override val sanityApiNavn = "opphorSokerBoddeIkkeIEtEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    REDUKSJON_BARN_DØD_EØS {
        override val sanityApiNavn = "reduksjonBarnDodEos"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },

    REDUKSJON_SØKER_BER_OM_OPPHØR_EØS {
        override val sanityApiNavn = "reduksjonSokerBerOmOpphorEos"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },

    REDUKSJON_BARN_BOR_IKKE_I_EØS_LAND {
        override val sanityApiNavn = "reduksjonBarnBorIkkeIEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },

    REDUKSJON_IKKE_ANSVAR_FOR_BARN {
        override val sanityApiNavn = "reduksjonIkkeAnsvarForBarn"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },

    REDUKSJON_TILLEGGSTEKST_VALUTAJUSTERING {
        override val sanityApiNavn = "reduksjonTilleggstekstValutajustering"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },

    REDUKSJON_UK_MIDLERTIDIG_DIFFERANSEUTBETALING {
        override val sanityApiNavn = "reduksjonUkMidlertidigDifferanseutbetaling"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },

    REDUKSJON_DELT_BOSTED_BEGGE_FORELDRE_IKKE_OMFATTET_NORSK_LOVVALG {
        override val sanityApiNavn = "reduksjonDeltBostedBeggeForeldreIkkeOmfattetNorskLovvalg"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },

    REDUKSJON_SELVSTENDIG_RETT_BARN_FLYTTET_FRA_SØKER {
        override val sanityApiNavn = "reduksjonSelvstendigRettBarnFlyttetFraSoker"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },
    FORTSATT_INNVILGET_EØS_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetEosStandard"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },
    ;

    override fun enumnavnTilString() = this.name
}
